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
    public AbstractCollection<AbstractSqueakObject> executeAllInstancesOf(final ClassObject classObj) {
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
    public AbstractSqueakObject executeSomeInstanceOf(final ClassObject classObj) {
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
                addIfHasNotImmediateClassType(pending, argument);
            }
            final CompiledCodeObject blockOrMethod = FrameAccess.getBlockOrMethod(current);
            final FrameDescriptor frameDescriptor = blockOrMethod.getFrameDescriptor();
            final ContextObject context = FrameAccess.getContext(current, blockOrMethod);
            if (context != null) {
                pending.add(context);
            }
            final FrameSlot[] stackSlots = blockOrMethod.getStackSlots();
            for (final FrameSlot slot : stackSlots) {
                final FrameSlotKind currentSlotKind = frameDescriptor.getFrameSlotKind(slot);
                if (currentSlotKind == FrameSlotKind.Object) {
                    addIfHasNotImmediateClassType(pending, FrameUtil.getObjectSafe(current, slot));
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

    private static void addIfHasNotImmediateClassType(final ArrayDeque<AbstractSqueakObject> pending, final Object argument) {
        if (SqueakGuards.isAbstractSqueakObject(argument)) {
            final AbstractSqueakObject abstractSqueakObject = (AbstractSqueakObject) argument;
            if (!abstractSqueakObject.getSqueakClass().isImmediateClassType()) {
                pending.add(abstractSqueakObject);
            }
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
                addIfHasNotImmediateClassType(pending, value);
            }
        } else if (object instanceof BlockClosureObject) {
            final BlockClosureObject closure = (BlockClosureObject) object;
            addIfHasNotImmediateClassType(pending, closure.getReceiver());
            pending.add(closure.getOuterContext());
            for (Object value : closure.getCopied()) {
                addIfHasNotImmediateClassType(pending, value);
            }
        } else if (object instanceof ContextObject) {
            final ContextObject context = (ContextObject) object;
            if (context.hasTruffleFrame()) {
                pending.add(context.getSender());
                pending.add(context.getMethod());
                if (context.hasClosure()) {
                    pending.add(context.getClosure());
                }
                addIfHasNotImmediateClassType(pending, context.getReceiver());
                for (int i = 0; i < context.getBlockOrMethod().getNumStackSlots(); i++) {
                    addIfHasNotImmediateClassType(pending, context.atTemp(i));
                }
            }
        } else if (object instanceof CompiledMethodObject) {
            for (Object literal : ((CompiledMethodObject) object).getLiterals()) {
                addIfHasNotImmediateClassType(pending, literal);
            }
        } else if (object instanceof ArrayObject) {
            final ArrayObject array = (ArrayObject) object;
            if (array.isObjectType()) {
                for (Object value : array.getObjectStorage()) {
                    addIfHasNotImmediateClassType(pending, value);
                }
            } else if (array.isAbstractSqueakObjectType()) {
                for (Object value : array.getAbstractSqueakObjectStorage()) {
                    addIfHasNotImmediateClassType(pending, value);
                }
            }
        } else if (object instanceof PointersObject) {
            for (Object pointer : ((PointersObject) object).getPointers()) {
                addIfHasNotImmediateClassType(pending, pointer);
            }
        } else if (object instanceof WeakPointersObject) {
            final WeakPointersObject weakPointersObject = ((WeakPointersObject) object);
            for (int i = 0; i < weakPointersObject.size(); i++) {
                addIfHasNotImmediateClassType(pending, weakPointersObject.at0(i));
            }
        }
    }
}
