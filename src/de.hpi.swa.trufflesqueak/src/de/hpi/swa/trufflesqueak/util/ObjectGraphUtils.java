/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.logging.Level;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.EphemeronObject;
import de.hpi.swa.trufflesqueak.model.NilObject;

public final class ObjectGraphUtils {
    private static final int ADDITIONAL_SPACE = 10_000;

    private static int lastSeenObjects = 500_000;

    private final SqueakImageContext image;
    private final boolean trackOperations;
    private final ArrayDeque<AbstractSqueakObjectWithClassAndHash> worklist = new ArrayDeque<>();

    public ObjectGraphUtils(final SqueakImageContext image) {
        this.image = image;
        this.trackOperations = image.options.printResourceSummary() || LogUtils.OBJECT_GRAPH.isLoggable(Level.FINE);
    }

    public static int getLastSeenObjects() {
        return lastSeenObjects;
    }

    @TruffleBoundary
    public AbstractCollection<AbstractSqueakObjectWithClassAndHash> allInstances() {
        final long startTime = System.nanoTime();
        final ArrayDeque<AbstractSqueakObjectWithClassAndHash> seen = new ArrayDeque<>(lastSeenObjects + ADDITIONAL_SPACE);
        final ObjectTracer pending = new ObjectTracer();
        final boolean currentMarkingFlag = pending.currentMarkingFlag;
        AbstractSqueakObjectWithClassAndHash currentObject;
        while ((currentObject = getNextFromWorklist()) != null) {
            if (currentObject.tryToMark(currentMarkingFlag)) {
                seen.add(currentObject);
                pending.tracePointers(currentObject);
            }
        }
        lastSeenObjects = seen.size();
        if (trackOperations) {
            ObjectGraphOperations.ALL_INSTANCES.addNanos(System.nanoTime() - startTime);
        }
        return seen;
    }

    @TruffleBoundary
    public Object[] allInstancesOf(final ClassObject targetClass) {
        final long startTime = System.nanoTime();
        final ArrayDeque<AbstractSqueakObjectWithClassAndHash> result = new ArrayDeque<>();
        final ObjectTracer pending = new ObjectTracer();
        final boolean currentMarkingFlag = pending.currentMarkingFlag;
        AbstractSqueakObjectWithClassAndHash currentObject;
        while ((currentObject = getNextFromWorklist()) != null) {
            if (currentObject.tryToMark(currentMarkingFlag)) {
                if (targetClass == currentObject.getSqueakClass()) {
                    result.add(currentObject);
                }
                pending.tracePointers(currentObject);
            }
        }
        ObjectGraphOperations.ALL_INSTANCES_OF.addNanos(System.nanoTime() - startTime);
        return result.toArray();
    }

    @TruffleBoundary
    public AbstractSqueakObject someInstanceOf(final ClassObject targetClass) {
        final long startTime = System.nanoTime();
        final ArrayDeque<AbstractSqueakObjectWithClassAndHash> marked = new ArrayDeque<>(lastSeenObjects / 2);
        final ObjectTracer pending = new ObjectTracer();
        final boolean currentMarkingFlag = pending.currentMarkingFlag;
        AbstractSqueakObject result = NilObject.SINGLETON;
        AbstractSqueakObjectWithClassAndHash currentObject;
        while ((currentObject = getNextFromWorklist()) != null) {
            if (currentObject.tryToMark(currentMarkingFlag)) {
                marked.add(currentObject);
                if (targetClass == currentObject.getSqueakClass()) {
                    clearWorklist();
                    // Unmark marked objects
                    for (final AbstractSqueakObjectWithClassAndHash object : marked) {
                        object.unmark(currentMarkingFlag);
                    }
                    // Restore marking flag
                    image.toggleCurrentMarkingFlag();
                    result = currentObject;
                    break;
                } else {
                    pending.tracePointers(currentObject);
                }
            }
        }
        if (trackOperations) {
            ObjectGraphOperations.SOME_INSTANCE_OF.addNanos(System.nanoTime() - startTime);
        }
        return result;
    }

    @TruffleBoundary
    public AbstractSqueakObject nextObject(final AbstractSqueakObjectWithClassAndHash targetObject) {
        final long startTime = System.nanoTime();
        final ArrayDeque<AbstractSqueakObjectWithClassAndHash> marked = new ArrayDeque<>(lastSeenObjects / 2);
        final ObjectTracer pending = new ObjectTracer();
        final boolean currentMarkingFlag = pending.currentMarkingFlag;
        AbstractSqueakObjectWithClassAndHash currentObject = getNextFromWorklist();
        AbstractSqueakObject result = currentObject; // first object
        boolean foundObject = false;
        while (currentObject != null) {
            if (foundObject) {
                clearWorklist();
                // Unmark marked objects
                for (final AbstractSqueakObjectWithClassAndHash object : marked) {
                    object.unmark(currentMarkingFlag);
                }
                // Restore marking flag
                image.toggleCurrentMarkingFlag();
                result = currentObject;
                break;
            }
            if (currentObject.tryToMark(currentMarkingFlag)) {
                marked.add(currentObject);
                if (currentObject == targetObject) {
                    foundObject = true;
                }
                pending.tracePointers(currentObject);
            }
            currentObject = getNextFromWorklist();
        }
        if (trackOperations) {
            ObjectGraphOperations.NEXT_OBJECT.addNanos(System.nanoTime() - startTime);
        }
        return result;
    }

    @TruffleBoundary
    public static void pointersBecomeOneWay(final SqueakImageContext image, final Object[] fromPointers, final Object[] toPointers) {
        final long startTime = System.nanoTime();
        final boolean currentMarkingFlag = image.toggleCurrentMarkingFlag();
        image.specialObjectsArray.pointersBecomeOneWay(currentMarkingFlag, fromPointers, toPointers);
        for (final EphemeronObject eo : image.ephemeronsQueue) {
            eo.pointersBecomeOneWay(currentMarkingFlag, fromPointers, toPointers);
        }
        pointersBecomeOneWayFrames(currentMarkingFlag, fromPointers, toPointers);
        ObjectGraphOperations.POINTERS_BECOME_ONE_WAY.addNanos(System.nanoTime() - startTime);
    }

    @TruffleBoundary
    private static void pointersBecomeOneWayFrames(final boolean currentMarkingFlag, final Object[] fromPointers, final Object[] toPointers) {
        final int fromPointersLength = fromPointers.length;
        Truffle.getRuntime().iterateFrames((frameInstance) -> {
            final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE);
            if (!FrameAccess.isTruffleSqueakFrame(current)) {
                return null;
            }
            final Object[] arguments = current.getArguments();
            for (int i = 0; i < arguments.length; i++) {
                final Object argument = arguments[i];
                for (int j = 0; j < fromPointersLength; j++) {
                    if (argument == fromPointers[j]) {
                        arguments[i] = toPointers[j];
                    } else if (argument instanceof final AbstractSqueakObjectWithClassAndHash o) {
                        o.pointersBecomeOneWay(currentMarkingFlag, fromPointers, toPointers);
                    }
                }
            }

            final ContextObject context = FrameAccess.getContext(current);
            if (context != null) {
                for (int j = 0; j < fromPointersLength; j++) {
                    if (context == fromPointers[j]) {
                        FrameAccess.setContext(current, (ContextObject) toPointers[j]);
                    } else {
                        context.pointersBecomeOneWay(currentMarkingFlag, fromPointers, toPointers);
                    }
                }
            }

            /*
             * Iterate over all stack slots here instead of stackPointer because in rare cases, the
             * stack is accessed behind the stackPointer.
             */
            FrameAccess.iterateStackSlots(current, slotIndex -> {
                if (current.isObject(slotIndex)) {
                    final Object stackObject = current.getObject(slotIndex);
                    for (int j = 0; j < fromPointersLength; j++) {
                        if (stackObject == fromPointers[j]) {
                            final Object toPointer = toPointers[j];
                            assert toPointer != null : "Unexpected `null` value";
                            current.setObject(slotIndex, toPointer);
                        } else if (stackObject instanceof final AbstractSqueakObjectWithClassAndHash o) {
                            o.pointersBecomeOneWay(currentMarkingFlag, fromPointers, toPointers);
                        }
                    }
                }
            });
            return null;
        });
    }

    @TruffleBoundary
    public boolean checkEphemerons() {
        final long startTime = System.nanoTime();
        final ObjectTracer pending = new ObjectTracer();
        final boolean currentMarkingFlag = pending.currentMarkingFlag;
        final ArrayDeque<EphemeronObject> ephemeronsToBeMarked = new ArrayDeque<>();
        AbstractSqueakObjectWithClassAndHash currentObject;

        while ((currentObject = getNextFromWorklist()) != null) {
            if (currentObject.tryToMark(currentMarkingFlag)) {
                // Ephemerons are traced in a special way.
                if (currentObject instanceof final EphemeronObject ephemeronObject) {
                    // An Ephemeron is traced normally if it has been signaled or its key has been
                    // marked already.
                    // Otherwise, they are traced after all other objects.
                    if (ephemeronObject.hasBeenSignaled() || ephemeronObject.keyHasBeenMarked(currentMarkingFlag)) {
                        pending.tracePointers(currentObject);
                    } else {
                        ephemeronsToBeMarked.add(ephemeronObject);
                    }
                } else {
                    // Normal object
                    pending.tracePointers(currentObject);
                }
            }
        }

        // Now, trace the ephemerons until there are only ephemerons whose keys are reachable
        // through ephemerons.
        traceRemainingEphemerons(ephemeronsToBeMarked, pending, currentMarkingFlag);

        // Make sure that they do not signal more than once.
        image.ephemeronsQueue.addAll(ephemeronsToBeMarked);
        for (EphemeronObject ephemeronObject : ephemeronsToBeMarked) {
            ephemeronObject.setHasBeenSignaled();
        }
        if (trackOperations) {
            ObjectGraphOperations.CHECK_EPHEMERONS.addNanos(System.nanoTime() - startTime);
        }
        return true;
    }

    private void traceRemainingEphemerons(final ArrayDeque<EphemeronObject> ephemeronsToBeMarked, final ObjectTracer pending, final boolean currentMarkingFlag) {
        // Trace the ephemerons that have marked keys until there are only ephemerons with unmarked
        // keys left.
        while (true) {
            boolean finished = true;
            final Iterator<EphemeronObject> iterator = ephemeronsToBeMarked.iterator();
            while (iterator.hasNext()) {
                final EphemeronObject ephemeronObject = iterator.next();
                if (ephemeronObject.keyHasBeenMarked(currentMarkingFlag)) {
                    pending.tracePointers(ephemeronObject);
                    iterator.remove();
                    finished = false;
                }
            }
            if (finished) {
                break;
            }
            finishPendingMarking(pending, currentMarkingFlag);
        }

        if (ephemeronsToBeMarked.isEmpty()) {
            return;
        }

        // Now, we have ephemerons whose keys are reachable only through ephemerons.
        // Mark them to keep consistent marking flags.
        for (EphemeronObject ephemeronObject : ephemeronsToBeMarked) {
            pending.tracePointers(ephemeronObject);
        }
        finishPendingMarking(pending, currentMarkingFlag);
    }

    private void finishPendingMarking(final ObjectTracer pending, final boolean currentMarkingFlag) {
        AbstractSqueakObjectWithClassAndHash currentObject;
        while ((currentObject = getNextFromWorklist()) != null) {
            if (currentObject.tryToMark(currentMarkingFlag)) {
                pending.tracePointers(currentObject);
            }
        }
    }

    private AbstractSqueakObjectWithClassAndHash getNextFromWorklist() {
        /*
         * For some reason, using pollLast() is faster than pollFirst() and keeps the max size of
         * the deque significantly smaller.
         */
        return worklist.pollLast();
    }

    private void clearWorklist() {
        worklist.clear();
    }

    public final class ObjectTracer {
        private final boolean currentMarkingFlag;

        private ObjectTracer() {
            assert worklist.isEmpty() : "worklist should be empty";
            // Flip the marking flag
            currentMarkingFlag = image.toggleCurrentMarkingFlag();
            // Add roots
            addIfUnmarked(image.specialObjectsArray);
            addObjectsFromTruffleFrames();
            // Unreachable ephemerons in the queue must be kept visible to the rest of the image.
            // These are technically "dead" and do not need to be saved when the image is stored on
            // disk,
            // but by tracing them we avoid an expensive reachability test in the fetch-next-mourner
            // primitive.
            worklist.addAll(image.ephemeronsQueue);
        }

        private void addObjectsFromTruffleFrames() {
            CompilerAsserts.neverPartOfCompilation();
            Truffle.getRuntime().iterateFrames(frameInstance -> {
                final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                if (!FrameAccess.isTruffleSqueakFrame(current)) {
                    return null;
                }
                addAllIfUnmarked(current.getArguments());
                addIfUnmarked(FrameAccess.getContext(current));
                FrameAccess.iterateStackSlots(current, slotIndex -> {
                    if (current.isObject(slotIndex)) {
                        addIfUnmarked(current.getObject(slotIndex));
                    }
                });
                return null;
            });
        }

        public void addIfUnmarked(final Object object) {
            if (object instanceof final AbstractSqueakObjectWithClassAndHash o && !o.isMarked(currentMarkingFlag)) {
                worklist.addLast(o);
            }
        }

        public void addAllIfUnmarked(final Object[] objects) {
            for (final Object object : objects) {
                addIfUnmarked(object);
            }
        }

        private void tracePointers(final AbstractSqueakObjectWithClassAndHash object) {
            addIfUnmarked(object.getSqueakClass());
            object.tracePointers(this);
        }
    }

    public enum ObjectGraphOperations {
        ALL_INSTANCES("allInstances"),
        ALL_INSTANCES_OF("allInstancesOf"),
        SOME_INSTANCE_OF("someInstanceOf"),
        NEXT_OBJECT("nextObject"),
        POINTERS_BECOME_ONE_WAY("pointersBecomeOneWay"),
        CHECK_EPHEMERONS("checkEphemerons");

        private static final int[] COUNTS = new int[ObjectGraphOperations.values().length];
        private static final long[] MILLIS = new long[ObjectGraphOperations.values().length];

        private final String name;

        ObjectGraphOperations(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void addNanos(final long nanos) {
            final long millis = nanos / 1_000_000;
            MILLIS[ordinal()] += millis;
            COUNTS[ordinal()]++;
            LogUtils.OBJECT_GRAPH.log(Level.FINE, () -> getName() + " took " + millis + "ms");
        }

        public int getCount() {
            return COUNTS[ordinal()];
        }

        public long getMillis() {
            return MILLIS[ordinal()];
        }
    }
}
