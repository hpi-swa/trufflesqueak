package de.hpi.swa.graal.squeak.nodes.context;

import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.HashSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectPointersBecomeOneWayNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ObjectGraphNode extends AbstractNodeWithImage {
    private static final float SEEN_LOAD_FACTOR = 0.9F;
    private static final int PENDING_INITIAL_SIZE = 256;

    private static int lastSeenObjects = 500000;

    protected ObjectGraphNode(final SqueakImageContext image) {
        super(image);
    }

    public static ObjectGraphNode create(final SqueakImageContext image) {
        return new ObjectGraphNode(image);
    }

    @TruffleBoundary
    public AbstractCollection<AbstractSqueakObject> executeAllInstances() {
        final HashSet<AbstractSqueakObject> seen = new HashSet<>((int) (lastSeenObjects / SEEN_LOAD_FACTOR), SEEN_LOAD_FACTOR);
        final ArrayDeque<AbstractSqueakObject> pending = new ArrayDeque<>(PENDING_INITIAL_SIZE);
        pending.add(image.specialObjectsArray);
        addObjectsFromTruffleFrames(pending);
        while (!pending.isEmpty()) {
            final AbstractSqueakObject currentObject = pending.pop();
            if (seen.add(currentObject)) {
                tracePointers(pending, currentObject);
            }
        }
        lastSeenObjects = seen.size();
        return seen;
    }

    @TruffleBoundary
    public void executePointersBecomeOneWay(final SqueakObjectPointersBecomeOneWayNode pointersBecomeNode, final Object[] fromPointers,
                    final Object[] toPointers, final boolean copyHash) {
        final HashSet<AbstractSqueakObject> seen = new HashSet<>((int) (lastSeenObjects / SEEN_LOAD_FACTOR), SEEN_LOAD_FACTOR);
        final ArrayDeque<AbstractSqueakObject> pending = new ArrayDeque<>(PENDING_INITIAL_SIZE);
        pending.add(image.specialObjectsArray);
        while (!pending.isEmpty()) {
            final AbstractSqueakObject currentObject = pending.pop();
            if (seen.add(currentObject)) {
                pointersBecomeNode.execute(currentObject, fromPointers, toPointers, copyHash);
                tracePointers(pending, currentObject);
            }
        }
        lastSeenObjects = seen.size();
    }

    @TruffleBoundary
    public AbstractCollection<AbstractSqueakObject> executeAllInstancesOf(final ClassObject classObj) {
        assert classObj != image.nilClass;
        final ArrayDeque<AbstractSqueakObject> result = new ArrayDeque<>();
        final HashSet<AbstractSqueakObject> seen = new HashSet<>((int) (lastSeenObjects / SEEN_LOAD_FACTOR), SEEN_LOAD_FACTOR);
        final ArrayDeque<AbstractSqueakObject> pending = new ArrayDeque<>(PENDING_INITIAL_SIZE);
        pending.add(image.specialObjectsArray);
        addObjectsFromTruffleFrames(pending);
        while (!pending.isEmpty()) {
            final AbstractSqueakObject currentObject = pending.pop();
            if (seen.add(currentObject)) {
                if (classObj == currentObject.getSqueakClass()) {
                    result.add(currentObject);
                }
                tracePointers(pending, currentObject);
            }
        }
        lastSeenObjects = seen.size();
        return result;
    }

    @TruffleBoundary
    public TruffleObject executeSomeInstanceOf(final ClassObject classObj) {
        assert classObj != image.nilClass;
        final HashSet<AbstractSqueakObject> seen = new HashSet<>((int) (lastSeenObjects / SEEN_LOAD_FACTOR), SEEN_LOAD_FACTOR);
        final ArrayDeque<AbstractSqueakObject> pending = new ArrayDeque<>(PENDING_INITIAL_SIZE);
        pending.add(image.specialObjectsArray);
        addObjectsFromTruffleFrames(pending);
        while (!pending.isEmpty()) {
            final AbstractSqueakObject currentObject = pending.pop();
            if (seen.add(currentObject)) {
                if (classObj == currentObject.getSqueakClass()) {
                    return currentObject;
                }
                tracePointers(pending, currentObject);
            }
        }
        lastSeenObjects = seen.size();
        return image.nil;
    }

    @TruffleBoundary
    private static void addObjectsFromTruffleFrames(final ArrayDeque<AbstractSqueakObject> pending) {
        Truffle.getRuntime().iterateFrames(frameInstance -> {
            final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
            if (!FrameAccess.isGraalSqueakFrame(current)) {
                return null;
            }
            for (final Object argument : current.getArguments()) {
                addIfAbstractSqueakObject(pending, argument);
            }
            final CompiledCodeObject blockOrMethod = FrameAccess.getBlockOrMethod(current);

            final FrameDescriptor frameDescriptor = blockOrMethod.getFrameDescriptor();
            final ContextObject context = FrameAccess.getContext(current, blockOrMethod);
            if (context != null) {
                pending.add(context);
            }
            final FrameSlot[] stackSlots = blockOrMethod.getStackSlotsUnsafe();
            for (final FrameSlot slot : stackSlots) {
                if (slot == null) {
                    return null; // Stop here, slot has not (yet) been created.
                }
                final FrameSlotKind currentSlotKind = frameDescriptor.getFrameSlotKind(slot);
                if (currentSlotKind == FrameSlotKind.Object) {
                    addIfAbstractSqueakObject(pending, FrameUtil.getObjectSafe(current, slot));
                } else if (currentSlotKind == FrameSlotKind.Illegal) {
                    return null; // Stop here, because this slot and all following are not used.
                }
            }
            return null;
        });
    }

    private static void tracePointers(final ArrayDeque<AbstractSqueakObject> pending, final AbstractSqueakObject currentObject) {
        final ClassObject sqClass = currentObject.getSqueakClass();
        if (sqClass != null) {
            pending.add(sqClass);
        }
        addTraceablePointers(pending, currentObject);
    }

    private static void addIfAbstractSqueakObject(final ArrayDeque<AbstractSqueakObject> pending, final Object argument) {
        if (argument instanceof AbstractSqueakObject) {
            pending.add((AbstractSqueakObject) argument);
        }
    }

    private static void addTraceablePointers(final ArrayDeque<AbstractSqueakObject> pending, final AbstractSqueakObject object) {
        if (object instanceof ClassObject) {
            final ClassObject classObject = (ClassObject) object;
            addIfAbstractSqueakObject(pending, classObject.getSuperclass());
            addIfAbstractSqueakObject(pending, classObject.getMethodDict());
            addIfAbstractSqueakObject(pending, classObject.getInstanceVariables());
            addIfAbstractSqueakObject(pending, classObject.getOrganization());
            for (final Object value : classObject.getOtherPointers()) {
                addIfAbstractSqueakObject(pending, value);
            }
        } else if (object instanceof BlockClosureObject) {
            final BlockClosureObject closure = (BlockClosureObject) object;
            addIfAbstractSqueakObject(pending, closure.getReceiver());
            addIfAbstractSqueakObject(pending, closure.getOuterContext());
            for (final Object value : closure.getCopied()) {
                addIfAbstractSqueakObject(pending, value);
            }
        } else if (object instanceof ContextObject) {
            final ContextObject context = (ContextObject) object;
            if (context.hasTruffleFrame()) {
                addIfAbstractSqueakObject(pending, context.getSender());
                addIfAbstractSqueakObject(pending, context.getMethod());
                if (context.hasClosure()) {
                    pending.add(context.getClosure());
                }
                addIfAbstractSqueakObject(pending, context.getReceiver());
                for (int i = 0; i < context.getBlockOrMethod().getNumStackSlots(); i++) {
                    addIfAbstractSqueakObject(pending, context.atTemp(i));
                }
            }
        } else if (object instanceof CompiledMethodObject) {
            for (final Object literal : ((CompiledMethodObject) object).getLiterals()) {
                addIfAbstractSqueakObject(pending, literal);
            }
        } else if (object instanceof ArrayObject) {
            final ArrayObject array = (ArrayObject) object;
            if (array.isObjectType()) {
                for (final Object value : array.getObjectStorage()) {
                    addIfAbstractSqueakObject(pending, value);
                }
            }
        } else if (object instanceof PointersObject) {
            for (final Object pointer : ((PointersObject) object).getPointers()) {
                addIfAbstractSqueakObject(pending, pointer);
            }
        }
        /**
         * No need to trace weak pointers objects. Their pointers are reachable from other objects
         * and therefore are traced anyway.
         */
    }
}
