/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import java.util.AbstractCollection;
import java.util.ArrayDeque;

import com.oracle.truffle.api.CompilerAsserts;
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
import de.hpi.swa.graal.squeak.model.VariablePointersObject;
import de.hpi.swa.graal.squeak.model.WeakVariablePointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectPointersBecomeOneWayNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ObjectGraphNode extends AbstractNodeWithImage {
    private static final int ADDITIONAL_SPACE = 10_000;

    private static int lastSeenObjects = 500_000;

    protected ObjectGraphNode(final SqueakImageContext image) {
        super(image);
    }

    public static ObjectGraphNode create(final SqueakImageContext image) {
        return new ObjectGraphNode(image);
    }

    @TruffleBoundary
    public AbstractCollection<AbstractSqueakObjectWithHash> executeAllInstances() {
        final ArrayDeque<AbstractSqueakObjectWithHash> seen = new ArrayDeque<>(lastSeenObjects + ADDITIONAL_SPACE);
        final ObjectTracer pending = new ObjectTracer(image);
        AbstractSqueakObjectWithHash currentObject;
        while ((currentObject = pending.getNextPending()) != null) {
            if (currentObject.tryToMark(pending.getCurrentMarkingFlag())) {
                seen.add(currentObject);
                pending.tracePointers(currentObject);
            }
        }
        lastSeenObjects = seen.size();
        return seen;
    }

    @TruffleBoundary
    public void executePointersBecomeOneWay(final SqueakObjectPointersBecomeOneWayNode pointersBecomeNode, final Object[] fromPointers,
                    final Object[] toPointers, final boolean copyHash) {
        final ObjectTracer pending = new ObjectTracer(image);
        AbstractSqueakObjectWithHash currentObject;
        while ((currentObject = pending.getNextPending()) != null) {
            if (currentObject.tryToMark(pending.getCurrentMarkingFlag())) {
                pointersBecomeNode.execute(currentObject, fromPointers, toPointers, copyHash);
                pending.tracePointers(currentObject);
            }
        }
    }

    @TruffleBoundary
    public Object[] executeAllInstancesOf(final ClassObject classObj) {
        final ArrayDeque<AbstractSqueakObjectWithHash> result = new ArrayDeque<>();
        final ObjectTracer pending = new ObjectTracer(image);
        AbstractSqueakObjectWithHash currentObject;
        while ((currentObject = pending.getNextPending()) != null) {
            if (currentObject.tryToMark(pending.getCurrentMarkingFlag())) {
                if (classObj == currentObject.getSqueakClass()) {
                    result.add(currentObject);
                }
                pending.tracePointers(currentObject);
            }
        }
        return result.toArray();
    }

    @TruffleBoundary
    public AbstractSqueakObject executeSomeInstanceOf(final ClassObject classObj) {
        final ObjectTracer pending = new ObjectTracer(image);
        AbstractSqueakObjectWithHash currentObject;
        while ((currentObject = pending.getNextPending()) != null) {
            if (currentObject.tryToMark(pending.getCurrentMarkingFlag())) {
                if (classObj == currentObject.getSqueakClass()) {
                    return currentObject;
                }
                pending.tracePointers(currentObject);
            }
        }
        return NilObject.SINGLETON;
    }

    public static final class ObjectTracer {
        /* Power of two, large enough to avoid resizing. */
        private static final int PENDING_INITIAL_SIZE = 1 << 17;

        private final boolean currentMarkingFlag;
        private final ArrayDeque<AbstractSqueakObjectWithHash> deque = new ArrayDeque<>(PENDING_INITIAL_SIZE);

        private ObjectTracer(final SqueakImageContext image) {
            // Flip the marking flag
            currentMarkingFlag = image.toggleCurrentMarkingFlag();
            // Add roots
            addIfUnmarked(image.specialObjectsArray);
            addObjectsFromTruffleFrames();
        }

        private void addObjectsFromTruffleFrames() {
            CompilerAsserts.neverPartOfCompilation();
            Truffle.getRuntime().iterateFrames(frameInstance -> {
                final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                if (!FrameAccess.isGraalSqueakFrame(current)) {
                    return null;
                }
                for (final Object argument : current.getArguments()) {
                    addIfUnmarked(argument);
                }
                final CompiledCodeObject blockOrMethod = FrameAccess.getBlockOrMethod(current);
                final FrameDescriptor frameDescriptor = blockOrMethod.getFrameDescriptor();
                addIfUnmarked(FrameAccess.getContext(current, blockOrMethod));
                final FrameSlot[] stackSlots = blockOrMethod.getStackSlotsUnsafe();
                for (final FrameSlot slot : stackSlots) {
                    if (slot == null) {
                        return null; // Stop here, slot has not (yet) been created.
                    }
                    final FrameSlotKind currentSlotKind = frameDescriptor.getFrameSlotKind(slot);
                    if (currentSlotKind == FrameSlotKind.Object) {
                        addIfUnmarked(FrameUtil.getObjectSafe(current, slot));
                    } else if (currentSlotKind == FrameSlotKind.Illegal) {
                        return null; // Stop here, because this slot and all following are not used.
                    }
                }
                return null;
            });
        }

        public void addIfUnmarked(final AbstractSqueakObjectWithHash object) {
            if (object != null && !object.isMarked(currentMarkingFlag)) {
                deque.add(object);
            }
        }

        public void addIfUnmarked(final Object object) {
            if (object instanceof AbstractSqueakObjectWithHash) {
                addIfUnmarked((AbstractSqueakObjectWithHash) object);
            }
        }

        private boolean getCurrentMarkingFlag() {
            return currentMarkingFlag;
        }

        private AbstractSqueakObjectWithHash getNextPending() {
            return deque.pollFirst();
        }

        private void tracePointers(final AbstractSqueakObjectWithHash object) {
            addIfUnmarked(object.getSqueakClass());
            if (object instanceof ClassObject) {
                ((ClassObject) object).traceObjects(this);
            } else if (object instanceof BlockClosureObject) {
                ((BlockClosureObject) object).traceObjects(this);
            } else if (object instanceof ContextObject) {
                ((ContextObject) object).traceObjects(this);
            } else if (object instanceof CompiledMethodObject) {
                ((CompiledMethodObject) object).traceObjects(this);
            } else if (object instanceof ArrayObject) {
                ((ArrayObject) object).traceObjects(this);
            } else if (object instanceof PointersObject) {
                ((PointersObject) object).traceObjects(this);
            } else if (object instanceof VariablePointersObject) {
                ((VariablePointersObject) object).traceObjects(this);
            } else if (object instanceof WeakVariablePointersObject) {
                ((WeakVariablePointersObject) object).traceObjects(this);
            }
        }
    }
}
