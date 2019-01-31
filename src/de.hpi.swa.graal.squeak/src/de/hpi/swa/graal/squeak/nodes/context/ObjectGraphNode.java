package de.hpi.swa.graal.squeak.nodes.context;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayNode;
import de.hpi.swa.graal.squeak.nodes.context.ObjectGraphNodeGen.GetTraceablePointersNodeGen;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class ObjectGraphNode extends AbstractNodeWithImage {
    private static final int SEEN_INITIAL_CAPACITY = 500000;
    private static final int PENDING_INITIAL_SIZE = 256;

    @CompilationFinal private static HashSet<AbstractSqueakObject> classesWithNoInstances;

    @Child private GetTraceablePointersNode getPointersNode = GetTraceablePointersNode.create();

    protected ObjectGraphNode(final SqueakImageContext image) {
        super(image);
    }

    public static ObjectGraphNode create(final SqueakImageContext image) {
        return ObjectGraphNodeGen.create(image);
    }

    public HashSet<AbstractSqueakObject> getClassesWithNoInstances() {
        if (classesWithNoInstances == null) {
            // TODO: BlockContext missing.
            final AbstractSqueakObject[] classes = new AbstractSqueakObject[]{image.smallIntegerClass, image.characterClass, image.floatClass};
            classesWithNoInstances = new HashSet<>(Arrays.asList(classes));
        }
        return classesWithNoInstances;
    }

    public List<AbstractSqueakObject> allInstances() {
        return executeTrace(null, false);
    }

    public List<AbstractSqueakObject> allInstancesOf(final ClassObject classObj) {
        return executeTrace(classObj, false);
    }

    @TruffleBoundary
    public AbstractSqueakObject someInstanceOf(final ClassObject classObj) {
        return executeTrace(classObj, true).get(0);
    }

    public abstract List<AbstractSqueakObject> executeTrace(ClassObject classObj, boolean isSomeInstance);

    @SuppressWarnings("unused")
    @Specialization(guards = {"classObj == null", "!isSomeInstance"})
    @TruffleBoundary
    protected final List<AbstractSqueakObject> doAllInstances(final ClassObject classObj, final boolean isSomeInstance) {
        final List<AbstractSqueakObject> result = new ArrayList<>();
        final Set<AbstractSqueakObject> seen = new HashSet<>(SEEN_INITIAL_CAPACITY);
        final Deque<AbstractSqueakObject> pending = new ArrayDeque<>(PENDING_INITIAL_SIZE);
        pending.add(image.specialObjectsArray);
        addObjectsFromTruffleFrames(pending);
        while (!pending.isEmpty()) {
            final AbstractSqueakObject currentObject = pending.pop();
            if (seen.add(currentObject)) {
                result.add(currentObject);
                pending.addAll(tracePointers(currentObject));
            }
        }
        return result;
    }

    @Specialization(guards = {"classObj != null"})
    @TruffleBoundary
    protected final List<AbstractSqueakObject> doAllInstancesOf(final ClassObject classObj, final boolean isSomeInstance) {
        final List<AbstractSqueakObject> result = new ArrayList<>();
        final Set<AbstractSqueakObject> seen = new HashSet<>(SEEN_INITIAL_CAPACITY);
        final Deque<AbstractSqueakObject> pending = new ArrayDeque<>(PENDING_INITIAL_SIZE);
        pending.add(image.specialObjectsArray);
        addObjectsFromTruffleFrames(pending);
        while (!pending.isEmpty()) {
            final AbstractSqueakObject currentObject = pending.pop();
            if (seen.add(currentObject)) {
                if (classObj == currentObject.getSqueakClass()) {
                    result.add(currentObject);
                    if (isSomeInstance) {
                        break;
                    }
                }
                pending.addAll(tracePointers(currentObject));
            }
        }
        return result;
    }

    @Fallback
    protected static final List<AbstractSqueakObject> doFail(final ClassObject classObj, final boolean isSomeInstance) {
        throw new SqueakException("Unexpected arguments:", classObj, isSomeInstance);
    }

    @TruffleBoundary
    private static void addObjectsFromTruffleFrames(final Deque<AbstractSqueakObject> pending) {
        Truffle.getRuntime().iterateFrames(frameInstance -> {
            final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
            if (!FrameAccess.isGraalSqueakFrame(current)) {
                return null;
            }
            final Object[] arguments = current.getArguments();
            for (int i = FrameAccess.RECEIVER; i < arguments.length; i++) {
                final Object argument = arguments[i];
                if (SqueakGuards.isAbstractSqueakObject(argument)) {
                    pending.add((AbstractSqueakObject) argument);
                }
            }
            final CompiledCodeObject blockOrMethod = FrameAccess.getBlockOrMethod(current);
            final FrameDescriptor frameDescriptor = blockOrMethod.getFrameDescriptor();
            final FrameSlot[] stackSlots = blockOrMethod.getStackSlots();
            for (final FrameSlot slot : stackSlots) {
                if (frameDescriptor.getFrameSlotKind(slot) == FrameSlotKind.Illegal) {
                    return null; // Stop here, because this slot and all following are not used.
                }
                final Object stackObject = current.getValue(slot);
                if (SqueakGuards.isAbstractSqueakObject(stackObject)) {
                    pending.add((AbstractSqueakObject) stackObject);
                }
            }
            return null;
        });
    }

    private List<AbstractSqueakObject> tracePointers(final AbstractSqueakObject currentObject) {
        final List<AbstractSqueakObject> result = new ArrayList<>(32);
        final ClassObject sqClass = currentObject.getSqueakClass();
        if (sqClass != null) {
            result.add(sqClass);
        }
        for (Object object : getPointersNode.executeGet(currentObject)) {
            if (SqueakGuards.isAbstractSqueakObject(object)) {
                result.add((AbstractSqueakObject) object);
            }
        }
        return result;
    }

    protected abstract static class GetTraceablePointersNode extends Node {

        private static GetTraceablePointersNode create() {
            return GetTraceablePointersNodeGen.create();
        }

        protected abstract Object[] executeGet(AbstractSqueakObject object);

        @Specialization
        protected static final Object[] doClass(final ClassObject object) {
            return object.getTraceableObjects();
        }

        @Specialization
        protected static final Object[] doClosure(final BlockClosureObject object) {
            return object.getTraceableObjects();
        }

        @Specialization(guards = "object.hasTruffleFrame()")
        protected static final Object[] doContext(final ContextObject object) {
            return object.asPointers();
        }

        @Specialization(guards = "!object.hasTruffleFrame()")
        protected static final Object[] doContextNoFrame(@SuppressWarnings("unused") final ContextObject object) {
            return ArrayUtils.EMPTY_ARRAY;
        }

        @Specialization
        protected static final Object[] doMethod(final CompiledMethodObject object) {
            return object.getLiterals();
        }

        @Specialization
        protected static final Object[] doArray(final ArrayObject object,
                        @Cached("create()") final ArrayObjectToObjectArrayNode getObjectArrayNode) {
            return getObjectArrayNode.execute(object);
        }

        @Specialization
        protected static final Object[] doPointers(final PointersObject object) {
            return object.getPointers();
        }

        @Specialization
        protected static final Object[] doWeakPointers(final WeakPointersObject object) {
            return object.getPointers();
        }

        @Fallback
        protected static final Object[] doFallback(@SuppressWarnings("unused") final AbstractSqueakObject object) {
            return ArrayUtils.EMPTY_ARRAY;
        }
    }
}
