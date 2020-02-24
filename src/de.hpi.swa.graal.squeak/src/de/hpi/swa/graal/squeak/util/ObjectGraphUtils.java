/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.util;

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
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectPointersBecomeOneWayNode;

public final class ObjectGraphUtils {
    private static final int ADDITIONAL_SPACE = 10_000;

    private static int lastSeenObjects = 500_000;

    private ObjectGraphUtils() {
    }

    public static int getLastSeenObjects() {
        return lastSeenObjects;
    }

    @TruffleBoundary
    public static AbstractCollection<AbstractSqueakObjectWithHash> allInstances(final SqueakImageContext image) {
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
    public static void pointersBecomeOneWay(final SqueakImageContext image, final SqueakObjectPointersBecomeOneWayNode pointersBecomeNode, final Object[] fromPointers,
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
    public static Object[] allInstancesOf(final SqueakImageContext image, final ClassObject classObj) {
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
    public static AbstractSqueakObject someInstanceOf(final SqueakImageContext image, final ClassObject classObj) {
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
            object.tracePointers(this);
        }
    }
}
