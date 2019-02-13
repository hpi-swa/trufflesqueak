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
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ObjectGraphNode extends AbstractNodeWithImage {
    private static final int SEEN_INITIAL_CAPACITY = 500000;
    private static final int PENDING_INITIAL_SIZE = 256;

    protected ObjectGraphNode(final SqueakImageContext image) {
        super(image);
    }

    public static ObjectGraphNode create(final SqueakImageContext image) {
        return new ObjectGraphNode(image);
    }

    @TruffleBoundary
    public AbstractCollection<AbstractSqueakObject> executeAllInstances() {
        final HashSet<AbstractSqueakObject> seen = new HashSet<>(SEEN_INITIAL_CAPACITY);
        final ArrayDeque<AbstractSqueakObject> pending = new ArrayDeque<>(PENDING_INITIAL_SIZE);
        pending.add(image.specialObjectsArray);
        addObjectsFromTruffleFrames(pending);
        while (!pending.isEmpty()) {
            final AbstractSqueakObject currentObject = pending.pop();
            if (seen.add(currentObject)) {
                tracePointers(pending, currentObject);
            }
        }
        return seen;
    }

    @TruffleBoundary
    public AbstractCollection<AbstractSqueakObject> executeAllInstancesOf(final ClassObject classObj) {
        final ArrayDeque<AbstractSqueakObject> result = new ArrayDeque<>();
        final HashSet<AbstractSqueakObject> seen = new HashSet<>(SEEN_INITIAL_CAPACITY);
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
        return result;
    }

    @TruffleBoundary
    public AbstractSqueakObject executeSomeInstanceOf(final ClassObject classObj) {
        final HashSet<AbstractSqueakObject> seen = new HashSet<>(SEEN_INITIAL_CAPACITY);
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
                addAbstractSqueakObjectToPendingDeque(pending, argument);
            }
            final CompiledCodeObject blockOrMethod = FrameAccess.getBlockOrMethod(current);
            final FrameDescriptor frameDescriptor = blockOrMethod.getFrameDescriptor();
            final FrameSlot[] stackSlots = blockOrMethod.getStackSlots();
            for (final FrameSlot slot : stackSlots) {
                if (frameDescriptor.getFrameSlotKind(slot) == FrameSlotKind.Illegal) {
                    return null; // Stop here, because this slot and all following are not used.
                }
                addAbstractSqueakObjectToPendingDeque(pending, current.getValue(slot));
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

    private static void addAbstractSqueakObjectToPendingDeque(final ArrayDeque<AbstractSqueakObject> pending, final Object argument) {
        if (SqueakGuards.isAbstractSqueakObject(argument)) {
            final AbstractSqueakObject abstractSqueakObject = (AbstractSqueakObject) argument;
            assert !abstractSqueakObject.getSqueakClass().isImmediateClassType();
            pending.add(abstractSqueakObject);
        }
    }

    private static void addTraceablePointers(final ArrayDeque<AbstractSqueakObject> pending, final AbstractSqueakObject object) {
        if (object instanceof ClassObject) {
            final ClassObject classObject = ((ClassObject) object);
            pending.add(classObject.getSuperclass());
            pending.add(classObject.getMethodDict());
            pending.add(classObject.getInstanceVariables());
            pending.add(classObject.getOrganization());
            for (Object value : classObject.getOtherPointers()) {
                addAbstractSqueakObjectToPendingDeque(pending, value);
            }
        } else if (object instanceof BlockClosureObject) {
            final BlockClosureObject closure = (BlockClosureObject) object;
            addAbstractSqueakObjectToPendingDeque(pending, closure.getReceiver());
            pending.add(closure.getOuterContext());
            for (Object value : closure.getCopied()) {
                addAbstractSqueakObjectToPendingDeque(pending, value);
            }
        } else if (object instanceof ContextObject) {
            final ContextObject context = (ContextObject) object;
            if (context.hasTruffleFrame()) {
                pending.add(context.getSender());
                pending.add(context.getMethod());
                if (context.hasClosure()) {
                    pending.add(context.getClosure());
                }
                addAbstractSqueakObjectToPendingDeque(pending, context.getReceiver());
                for (int i = 0; i < context.getBlockOrMethod().getNumStackSlots(); i++) {
                    addAbstractSqueakObjectToPendingDeque(pending, context.atTemp(i));
                }
            }
        } else if (object instanceof CompiledMethodObject) {
            for (Object literal : ((CompiledMethodObject) object).getLiterals()) {
                addAbstractSqueakObjectToPendingDeque(pending, literal);
            }
        } else if (object instanceof ArrayObject) {
            final ArrayObject array = (ArrayObject) object;
            if (array.isObjectType()) {
                for (Object value : array.getObjectStorage()) {
                    addAbstractSqueakObjectToPendingDeque(pending, value);
                }
            } else if (array.isAbstractSqueakObjectType()) {
                for (Object value : array.getAbstractSqueakObjectStorage()) {
                    addAbstractSqueakObjectToPendingDeque(pending, value);
                }
            }
        } else if (object instanceof PointersObject) {
            for (Object pointer : ((PointersObject) object).getPointers()) {
                addAbstractSqueakObjectToPendingDeque(pending, pointer);
            }
        } else if (object instanceof WeakPointersObject) {
            final WeakPointersObject weakPointersObject = ((WeakPointersObject) object);
            for (int i = 0; i < weakPointersObject.size(); i++) {
                addAbstractSqueakObjectToPendingDeque(pending, weakPointersObject.at0(i));
            }
        }
    }
}
