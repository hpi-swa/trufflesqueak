package de.hpi.swa.graal.squeak.nodes.context;

import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Set;

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
import de.hpi.swa.graal.squeak.model.AbstractSqueakObjectWithHash;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectPointersBecomeOneWayNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ObjectGraphNode extends AbstractNodeWithImage {
    private static final int PENDING_INITIAL_SIZE = 256;
    private static final Object SEEN_MARKER = new Object();
    private static final int ADDITIONAL_SPACE = 10_000;

    private static int lastSeenObjects = 500_000;

    protected ObjectGraphNode(final SqueakImageContext image) {
        super(image);
    }

    public static ObjectGraphNode create(final SqueakImageContext image) {
        return new ObjectGraphNode(image);
    }

    @TruffleBoundary
    public Set<AbstractSqueakObject> executeAllInstances() {
        final IdentityHashMap<AbstractSqueakObject, Object> seen = new IdentityHashMap<>(lastSeenObjects + ADDITIONAL_SPACE);
        final ArrayDeque<AbstractSqueakObjectWithHash> pending = new ArrayDeque<>(PENDING_INITIAL_SIZE);
        pending.add(image.specialObjectsArray);
        addObjectsFromTruffleFrames(pending);
        AbstractSqueakObjectWithHash currentObject;
        while ((currentObject = pending.pollFirst()) != null) {
            if (seen.put(currentObject, SEEN_MARKER) == null) {
                tracePointers(pending, currentObject);
            }
        }
        lastSeenObjects = seen.size();
        return seen.keySet();
    }

    @TruffleBoundary
    public void executePointersBecomeOneWay(final SqueakObjectPointersBecomeOneWayNode pointersBecomeNode, final Object[] fromPointers,
                    final Object[] toPointers, final boolean copyHash) {
        final IdentityHashMap<AbstractSqueakObjectWithHash, Object> seen = new IdentityHashMap<>(lastSeenObjects + ADDITIONAL_SPACE);
        final ArrayDeque<AbstractSqueakObjectWithHash> pending = new ArrayDeque<>(PENDING_INITIAL_SIZE);
        pending.add(image.specialObjectsArray);
        AbstractSqueakObjectWithHash currentObject;
        while ((currentObject = pending.pollFirst()) != null) {
            if (seen.put(currentObject, SEEN_MARKER) == null) {
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
        final IdentityHashMap<AbstractSqueakObject, Object> seen = new IdentityHashMap<>(lastSeenObjects + ADDITIONAL_SPACE);
        final ArrayDeque<AbstractSqueakObjectWithHash> pending = new ArrayDeque<>(PENDING_INITIAL_SIZE);
        pending.add(image.specialObjectsArray);
        addObjectsFromTruffleFrames(pending);
        AbstractSqueakObjectWithHash currentObject;
        while ((currentObject = pending.pollFirst()) != null) {
            if (seen.put(currentObject, SEEN_MARKER) == null) {
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
        assert classObj != image.nilClass;
        final IdentityHashMap<AbstractSqueakObjectWithHash, Object> seen = new IdentityHashMap<>(lastSeenObjects + ADDITIONAL_SPACE);
        final ArrayDeque<AbstractSqueakObjectWithHash> pending = new ArrayDeque<>(PENDING_INITIAL_SIZE);
        pending.add(image.specialObjectsArray);
        addObjectsFromTruffleFrames(pending);
        AbstractSqueakObjectWithHash currentObject;
        while ((currentObject = pending.pollFirst()) != null) {
            if (seen.put(currentObject, SEEN_MARKER) == null) {
                if (classObj == currentObject.getSqueakClass()) {
                    return currentObject;
                }
                tracePointers(pending, currentObject);
            }
        }
        lastSeenObjects = seen.size();
        return NilObject.SINGLETON;
    }

    @TruffleBoundary
    private static void addObjectsFromTruffleFrames(final ArrayDeque<AbstractSqueakObjectWithHash> pending) {
        Truffle.getRuntime().iterateFrames(frameInstance -> {
            final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
            if (!FrameAccess.isGraalSqueakFrame(current)) {
                return null;
            }
            for (final Object argument : current.getArguments()) {
                addIfAbstractSqueakObjectWithImage(pending, argument);
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
                    addIfAbstractSqueakObjectWithImage(pending, FrameUtil.getObjectSafe(current, slot));
                } else if (currentSlotKind == FrameSlotKind.Illegal) {
                    return null; // Stop here, because this slot and all following are not used.
                }
            }
            return null;
        });
    }

    private static void tracePointers(final ArrayDeque<AbstractSqueakObjectWithHash> pending, final AbstractSqueakObjectWithHash currentObject) {
        final ClassObject sqClass = currentObject.getSqueakClass();
        if (sqClass != null) {
            pending.add(sqClass);
        }
        addTraceablePointers(pending, currentObject);
    }

    private static void addIfAbstractSqueakObjectWithImage(final ArrayDeque<AbstractSqueakObjectWithHash> pending, final Object argument) {
        if (argument instanceof AbstractSqueakObjectWithHash) {
            pending.add((AbstractSqueakObjectWithHash) argument);
        }
    }

    private static void addTraceablePointers(final ArrayDeque<AbstractSqueakObjectWithHash> pending, final AbstractSqueakObjectWithHash object) {
        final Class<? extends AbstractSqueakObjectWithHash> objectClass = object.getClass();
        if (objectClass == ClassObject.class) {
            final ClassObject classObject = (ClassObject) object;
            addIfAbstractSqueakObjectWithImage(pending, classObject.getSuperclass());
            addIfAbstractSqueakObjectWithImage(pending, classObject.getMethodDict());
            addIfAbstractSqueakObjectWithImage(pending, classObject.getInstanceVariables());
            addIfAbstractSqueakObjectWithImage(pending, classObject.getOrganization());
            for (final Object value : classObject.getOtherPointers()) {
                addIfAbstractSqueakObjectWithImage(pending, value);
            }
        } else if (objectClass == BlockClosureObject.class) {
            final BlockClosureObject closure = (BlockClosureObject) object;
            addIfAbstractSqueakObjectWithImage(pending, closure.getReceiver());
            addIfAbstractSqueakObjectWithImage(pending, closure.getOuterContext());
            for (final Object value : closure.getCopied()) {
                addIfAbstractSqueakObjectWithImage(pending, value);
            }
        } else if (objectClass == ContextObject.class) {
            final ContextObject context = (ContextObject) object;
            if (context.hasTruffleFrame()) {
                addIfAbstractSqueakObjectWithImage(pending, context.getFrameSender());
                addIfAbstractSqueakObjectWithImage(pending, context.getMethod());
                if (context.hasClosure()) {
                    pending.add(context.getClosure());
                }
                addIfAbstractSqueakObjectWithImage(pending, context.getReceiver());
                for (int i = 0; i < context.getBlockOrMethod().getNumStackSlots(); i++) {
                    addIfAbstractSqueakObjectWithImage(pending, context.atTemp(i));
                }
            }
        } else if (objectClass == CompiledMethodObject.class) {
            for (final Object literal : ((CompiledMethodObject) object).getLiterals()) {
                addIfAbstractSqueakObjectWithImage(pending, literal);
            }
        } else if (objectClass == ArrayObject.class) {
            final ArrayObject array = (ArrayObject) object;
            if (array.isObjectType()) {
                for (final Object value : array.getObjectStorage()) {
                    addIfAbstractSqueakObjectWithImage(pending, value);
                }
            }
        } else if (objectClass == PointersObject.class) {
            for (final Object pointer : ((PointersObject) object).getPointers()) {
                addIfAbstractSqueakObjectWithImage(pending, pointer);
            }
        }
        /**
         * No need to trace weak pointers objects. Their pointers are reachable from other objects
         * and therefore are traced anyway.
         */
    }
}
