/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.util.AbstractCollection;
import java.util.ArrayDeque;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NilObject;

public final class ObjectGraphUtils {
    private static final int ADDITIONAL_SPACE = 10_000;

    private static int lastSeenObjects = 500_000;

    private ObjectGraphUtils() {
    }

    public static int getLastSeenObjects() {
        return lastSeenObjects;
    }

    @TruffleBoundary
    public static AbstractCollection<AbstractSqueakObjectWithClassAndHash> allInstances(final SqueakImageContext image) {
        final ArrayDeque<AbstractSqueakObjectWithClassAndHash> seen = new ArrayDeque<>(lastSeenObjects + ADDITIONAL_SPACE);
        final ObjectTracer pending = new ObjectTracer(image);
        AbstractSqueakObjectWithClassAndHash currentObject;
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
    public static void pointersBecomeOneWay(final SqueakImageContext image, final Object[] fromPointers, final Object[] toPointers) {
        final ObjectTracer pending = new ObjectTracer(image);
        AbstractSqueakObjectWithClassAndHash currentObject;
        while ((currentObject = pending.getNextPending()) != null) {
            if (currentObject.tryToMark(pending.getCurrentMarkingFlag())) {
                currentObject.pointersBecomeOneWay(fromPointers, toPointers);
                pending.tracePointers(currentObject);
            }
        }
    }

    @TruffleBoundary
    public static Object[] allInstancesOf(final SqueakImageContext image, final ClassObject targetClass) {
        final ArrayDeque<AbstractSqueakObjectWithClassAndHash> result = new ArrayDeque<>();
        final ObjectTracer pending = new ObjectTracer(image);
        AbstractSqueakObjectWithClassAndHash currentObject;
        while ((currentObject = pending.getNextPending()) != null) {
            if (currentObject.tryToMark(pending.getCurrentMarkingFlag())) {
                if (targetClass == currentObject.getSqueakClass()) {
                    result.add(currentObject);
                }
                pending.tracePointers(currentObject);
            }
        }
        return result.toArray();
    }

    @TruffleBoundary
    public static AbstractSqueakObject someInstanceOf(final SqueakImageContext image, final ClassObject targetClass) {
        final ArrayDeque<AbstractSqueakObjectWithClassAndHash> marked = new ArrayDeque<>(lastSeenObjects / 2);
        final ObjectTracer pending = new ObjectTracer(image);
        final boolean currentMarkingFlag = pending.getCurrentMarkingFlag();
        AbstractSqueakObjectWithClassAndHash currentObject;
        while ((currentObject = pending.getNextPending()) != null) {
            if (currentObject.tryToMark(currentMarkingFlag)) {
                marked.add(currentObject);
                if (targetClass == currentObject.getSqueakClass()) {
                    // Unmark marked objects
                    for (final AbstractSqueakObjectWithClassAndHash object : marked) {
                        object.unmark(currentMarkingFlag);
                    }
                    // Restore marking flag
                    image.toggleCurrentMarkingFlag();
                    return currentObject;
                } else {
                    pending.tracePointers(currentObject);
                }
            }
        }
        return NilObject.SINGLETON;
    }

    public static final class ObjectTracer {
        /* Power of two, large enough to avoid resizing. */
        private static final int PENDING_INITIAL_SIZE = 1 << 17;

        private final boolean currentMarkingFlag;
        private final ArrayDeque<AbstractSqueakObjectWithClassAndHash> deque = new ArrayDeque<>(PENDING_INITIAL_SIZE);

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
                if (!FrameAccess.isTruffleSqueakFrame(current)) {
                    return null;
                }
                for (final Object argument : current.getArguments()) {
                    addIfUnmarked(argument);
                }
                addIfUnmarked(FrameAccess.getContext(current));
                FrameAccess.iterateStackSlots(current, slotIndex -> {
                    if (current.isObject(slotIndex)) {
                        addIfUnmarked(current.getObject(slotIndex));
                    }
                });
                return null;
            });
        }

        public void addIfUnmarked(final AbstractSqueakObjectWithClassAndHash object) {
            if (object != null && !object.isMarked(currentMarkingFlag)) {
                deque.add(object);
            }
        }

        public void addIfUnmarked(final Object object) {
            if (object instanceof final AbstractSqueakObjectWithClassAndHash o) {
                addIfUnmarked(o);
            }
        }

        private boolean getCurrentMarkingFlag() {
            return currentMarkingFlag;
        }

        private AbstractSqueakObjectWithClassAndHash getNextPending() {
            return deque.pollFirst();
        }

        private void tracePointers(final AbstractSqueakObjectWithClassAndHash object) {
            addIfUnmarked(object.getSqueakClass());
            object.tracePointers(this);
        }
    }
}
