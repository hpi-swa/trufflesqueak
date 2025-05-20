/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives;
import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.UnmodifiableEconomicMap;

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

    private final int usableThreadCount = Math.min(Runtime.getRuntime().availableProcessors(), 4);

    private final SqueakImageContext image;
    private final boolean trackOperations;

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
        final ObjectTracer pending = new ObjectTracer(true);
        AbstractSqueakObjectWithClassAndHash currentObject;
        while ((currentObject = pending.getNext()) != null) {
            seen.add(currentObject);
            pending.tracePointers(currentObject);
        }
        lastSeenObjects = seen.size();
        if (trackOperations) {
            ObjectGraphOperations.ALL_INSTANCES.addNanos(System.nanoTime() - startTime);
        }
        return seen;
    }

    @TruffleBoundary
    public Object[] allInstancesOf(final ClassObject targetClass) {

        class AllInstancesOfTask implements Runnable {
            private final ObjectTracer roots;
            private final ArrayDeque<AbstractSqueakObjectWithClassAndHash> objects;

            public AllInstancesOfTask(final ObjectTracer theRoots, final ArrayDeque<AbstractSqueakObjectWithClassAndHash> theObjects) {
                roots = theRoots;
                objects = theObjects;
            }

            public void run() {
                final ObjectTracer pending = new ObjectTracer(roots);
                AbstractSqueakObjectWithClassAndHash root;
                while ((root = roots.getNextWithLock()) != null) {
                    AbstractSqueakObjectWithClassAndHash currentObject = root;
                    do {
                        if (targetClass == currentObject.getSqueakClass()) {
                            objects.add(currentObject);
                        }
                        pending.tracePointers(currentObject);
                    } while ((currentObject = pending.getNext()) != null);
                }
            }
        }


        final long startTime = System.nanoTime();

        final ObjectTracer roots = new ObjectTracer(true);

        Runnable[] tasks = new Runnable[usableThreadCount];
        List<ArrayDeque<AbstractSqueakObjectWithClassAndHash>> objectsList = new ArrayList<>(usableThreadCount);
        for (int i = 0; i < usableThreadCount; i++) {
            final ArrayDeque<AbstractSqueakObjectWithClassAndHash> objects = new ArrayDeque<>();
            objectsList.add(objects);
            tasks[i] = new AllInstancesOfTask(roots,objects);
        }
        roots.runTasks(tasks);

        int totalSize = 0;
        for (ArrayDeque<AbstractSqueakObjectWithClassAndHash> deque : objectsList) {
            totalSize += deque.size();
        }

        final ArrayDeque<AbstractSqueakObjectWithClassAndHash> result = new ArrayDeque<>(totalSize);
        for (ArrayDeque<AbstractSqueakObjectWithClassAndHash> deque : objectsList) {
            result.addAll(deque);
        }

        if (trackOperations) {
            ObjectGraphOperations.ALL_INSTANCES_OF.addNanos(System.nanoTime() - startTime);
        }
        return result.toArray();
    }

    @TruffleBoundary
    public AbstractSqueakObject someInstanceOf(final ClassObject targetClass) {
        final long startTime = System.nanoTime();
        final ArrayDeque<AbstractSqueakObjectWithClassAndHash> marked = new ArrayDeque<>(lastSeenObjects / 2);
        final ObjectTracer pending = new ObjectTracer(true);
        final boolean currentMarkingFlag = pending.currentMarkingFlag;
        AbstractSqueakObject result = NilObject.SINGLETON;
        AbstractSqueakObjectWithClassAndHash currentObject;
        while ((currentObject = pending.getNext()) != null) {
            marked.add(currentObject);
            if (targetClass == currentObject.getSqueakClass()) {
                result = currentObject;
                // Unmark marked objects
                while ((currentObject = pending.getNext()) != null) {
                    currentObject.unmark(currentMarkingFlag);
                }
                for (final AbstractSqueakObjectWithClassAndHash object : marked) {
                    object.unmark(currentMarkingFlag);
                }
                // Restore marking flag
                image.toggleCurrentMarkingFlag();
                break;
            } else {
                pending.tracePointers(currentObject);
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
        final ObjectTracer pending = new ObjectTracer(true);
        final boolean currentMarkingFlag = pending.currentMarkingFlag;
        AbstractSqueakObjectWithClassAndHash currentObject = pending.getNext();
        AbstractSqueakObject result = currentObject; // first object
        boolean foundObject = false;
        while (currentObject != null) {
            if (foundObject) {
                result = currentObject;
                // Unmark marked objects
                while ((currentObject = pending.getNext()) != null) {
                    currentObject.unmark(currentMarkingFlag);
                }
                for (final AbstractSqueakObjectWithClassAndHash object : marked) {
                    object.unmark(currentMarkingFlag);
                }
                // Restore marking flag
                image.toggleCurrentMarkingFlag();
                break;
            }
            marked.add(currentObject);
            if (currentObject == targetObject) {
                foundObject = true;
            }
            pending.tracePointers(currentObject);
            currentObject = pending.getNext();
        }
        if (trackOperations) {
            ObjectGraphOperations.NEXT_OBJECT.addNanos(System.nanoTime() - startTime);
        }
        return result;
    }

    @TruffleBoundary
    public void pointersBecomeOneWay(final Object[] fromPointers, final Object[] toPointers) {
        final long startTime = System.nanoTime();
        if (fromPointers.length == 1) {
            pointersBecomeOneWaySinglePair(fromPointers[0], toPointers[0]);
        } else {
            pointersBecomeOneWayManyPairs(fromPointers, toPointers);
        }
        if (trackOperations) {
            ObjectGraphOperations.POINTERS_BECOME_ONE_WAY.addNanos(System.nanoTime() - startTime);
        }
    }

    private void pointersBecomeOneWaySinglePair(final Object fromPointer, final Object toPointer) {

        class BecomeOneWaySinglePairTask implements Runnable {
            private final ObjectTracer roots;
            private final Object from;
            private final Object to;

            public BecomeOneWaySinglePairTask(final ObjectTracer theRoots) {
                roots = theRoots;
                from = fromPointer;
                to = toPointer;
            }

            public void run() {
                final ObjectTracer pending = new ObjectTracer(roots);
                AbstractSqueakObjectWithClassAndHash root;
                while ((root = roots.getNextWithLock()) != null) {
                    AbstractSqueakObjectWithClassAndHash currentObject = root;
                    do {
                        currentObject.pointersBecomeOneWay(from, to);
                        pending.tracePointers(currentObject);
                    } while ((currentObject = pending.getNext()) != null);
                }
            }
        }

        final ObjectTracer roots = new ObjectTracer(false);
        pointersBecomeOneWayFrames(roots, fromPointer, toPointer);

        Runnable[] tasks = new Runnable[usableThreadCount];
        for (int i = 0; i < usableThreadCount; i++) {
            tasks[i] = new BecomeOneWaySinglePairTask(roots);
        }
        roots.runTasks(tasks);
    }

    private void pointersBecomeOneWayManyPairs(final Object[] fromPointers, final Object[] toPointers) {

        class BecomeOneWayManyPairsTask implements Runnable {
            private final ObjectTracer roots;
            private final EconomicMap<Object, Object> fromToMap;

            public BecomeOneWayManyPairsTask(final ObjectTracer theRoots, final EconomicMap<Object, Object> theFromToMap) {
                roots = theRoots;
                fromToMap = theFromToMap;
            }

            public void run() {
                final ObjectTracer pending = new ObjectTracer(roots);
                AbstractSqueakObjectWithClassAndHash root;
                while ((root = roots.getNextWithLock()) != null) {
                    AbstractSqueakObjectWithClassAndHash currentObject = root;
                    do {
                        currentObject.pointersBecomeOneWay(fromToMap);
                        pending.tracePointers(currentObject);
                    } while ((currentObject = pending.getNext()) != null);
                }
            }
        }

        final EconomicMap<Object, Object> fromToMap = EconomicMap.create(Equivalence.IDENTITY, fromPointers.length);
        for (int i = 0; i < fromPointers.length; i++) {
            fromToMap.put(fromPointers[i], toPointers[i]);
        }

        final ObjectTracer roots = new ObjectTracer(false);
        pointersBecomeOneWayFrames(roots, fromToMap);

        Runnable[] tasks = new Runnable[usableThreadCount];
        for (int i = 0; i < usableThreadCount; i++) {
            tasks[i] = new BecomeOneWayManyPairsTask(roots, fromToMap);
        }
        roots.runTasks(tasks);
//
//
//        try (final ExecutorService executor = Executors.newFixedThreadPool(usableThreadCount)) {
//            final ObjectTracer roots = new ObjectTracer(false);
//            pointersBecomeOneWayFrames(roots, fromToMap);
//            for (int i = 0; i < usableThreadCount; i++) {
//                executor.submit(new BecomeOneWayManyPairsTask(roots, fromToMap));
//            }
//            executor.shutdown();
//            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
//        } catch (final InterruptedException e) {
//            throw CompilerDirectives.shouldNotReachHere("pointersBecomeOneWayManyPairs was interrupted");
//        }
    }

    @TruffleBoundary
    private static void pointersBecomeOneWayFrames(final ObjectTracer tracer, final Object fromPointer, final Object toPointer) {
        Truffle.getRuntime().iterateFrames((frameInstance) -> {
            final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE);
            if (!FrameAccess.isTruffleSqueakFrame(current)) {
                return null;
            }
            final Object[] arguments = current.getArguments();
            for (int i = 0; i < arguments.length; i++) {
                if (arguments[i] == fromPointer) {
                    arguments[i] = toPointer;
                }
                tracer.addIfUnmarked(arguments[i]);
            }

            final ContextObject context = FrameAccess.getContext(current);
            if (context != null) {
                if (context == fromPointer && toPointer instanceof final ContextObject o) {
                    FrameAccess.setContext(current, o);
                }
                tracer.addIfUnmarked(FrameAccess.getContext(current));
            }

            /*
             * Iterate over all stack slots here instead of stackPointer because in rare cases, the
             * stack is accessed behind the stackPointer.
             */
            FrameAccess.iterateStackSlots(current, slotIndex -> {
                if (current.isObject(slotIndex)) {
                    final Object stackObject = current.getObject(slotIndex);
                    if (stackObject == null) {
                        return;
                    }
                    if (stackObject == fromPointer) {
                        current.setObject(slotIndex, toPointer);
                        tracer.addIfUnmarked(toPointer);
                    } else {
                        tracer.addIfUnmarked(stackObject);
                    }
                }
            });
            return null;
        });
    }

    @TruffleBoundary
    private static void pointersBecomeOneWayFrames(final ObjectTracer tracer, final UnmodifiableEconomicMap<Object, Object> fromToMap) {
        Truffle.getRuntime().iterateFrames((frameInstance) -> {
            final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE);
            if (!FrameAccess.isTruffleSqueakFrame(current)) {
                return null;
            }
            final Object[] arguments = current.getArguments();
            for (int i = 0; i < arguments.length; i++) {
                final Object argument = arguments[i];
                if (argument != null) {
                    final Object migratedValue = fromToMap.get(argument);
                    if (migratedValue != null) {
                        arguments[i] = migratedValue;
                    }
                }
                tracer.addIfUnmarked(arguments[i]);
            }

            final ContextObject context = FrameAccess.getContext(current);
            if (context != null) {
                final Object toContext = fromToMap.get(context);
                if (toContext instanceof final ContextObject o) {
                    FrameAccess.setContext(current, o);
                }
                tracer.addIfUnmarked(FrameAccess.getContext(current));
            }

            /*
             * Iterate over all stack slots here instead of stackPointer because in rare cases, the
             * stack is accessed behind the stackPointer.
             */
            FrameAccess.iterateStackSlots(current, slotIndex -> {
                if (current.isObject(slotIndex)) {
                    final Object stackObject = current.getObject(slotIndex);
                    if (stackObject != null) {
                        final Object migratedObject = fromToMap.get(stackObject);
                        if (migratedObject != null) {
                            current.setObject(slotIndex, migratedObject);
                            tracer.addIfUnmarked(migratedObject);
                        } else {
                            tracer.addIfUnmarked(stackObject);
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
        final ObjectTracer pending = new ObjectTracer(true);
        final boolean currentMarkingFlag = pending.currentMarkingFlag;
        final ArrayDeque<EphemeronObject> ephemeronsToBeMarked = new ArrayDeque<>();
        AbstractSqueakObjectWithClassAndHash currentObject;

        while ((currentObject = pending.getNext()) != null) {
            // Ephemerons are traced in a special way.
            if (currentObject instanceof final EphemeronObject ephemeronObject) {
                // An Ephemeron is traced normally if it has been signaled or its key has been
                // marked already. Otherwise, they are traced after all other objects.
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
            finishPendingMarking(pending);
        }

        if (ephemeronsToBeMarked.isEmpty()) {
            return;
        }

        // Now, we have ephemerons whose keys are reachable only through ephemerons.
        // Mark them to keep consistent marking flags.
        for (EphemeronObject ephemeronObject : ephemeronsToBeMarked) {
            pending.tracePointers(ephemeronObject);
        }
        finishPendingMarking(pending);
    }

    private static void finishPendingMarking(final ObjectTracer pending) {
        AbstractSqueakObjectWithClassAndHash currentObject;
        while ((currentObject = pending.getNext()) != null) {
            pending.tracePointers(currentObject);
        }
    }

    public final class ObjectTracer {
        /* Using a stack for DFS traversal when walking Smalltalk objects. */
        private final ArrayDeque<AbstractSqueakObjectWithClassAndHash> workStack = new ArrayDeque<>();
        private final boolean currentMarkingFlag;
        private final Lock lock = new ReentrantLock();

        private ObjectTracer(final boolean addObjectsFromFrames) {
            // Flip the marking flag
            currentMarkingFlag = image.toggleCurrentMarkingFlag();
            // Add roots, in reversed order because workStack is LIFO
            if (addObjectsFromFrames) {
                addObjectsFromFrames();
            }
            // Unreachable ephemerons in the queue must be kept visible to the rest of the image.
            // These are technically "dead" and do not need to be saved when the image is stored on
            // disk, but by tracing them we avoid an expensive reachability test in the
            // fetch-next-mourner primitive.
            workStack.addAll(image.ephemeronsQueue);

            // Trace the special objects to give separate roots to the threads.
            addIfUnmarked(image.specialObjectsArray);
            tracePointers(image.specialObjectsArray);
        }

        private void addObjectsFromFrames() {
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

        public ObjectTracer(final ObjectTracer roots) {
            // Create an empty traversal based on this root traversal.
            this.currentMarkingFlag = roots.currentMarkingFlag;
        }

        private AbstractSqueakObjectWithClassAndHash getNext() {
            return workStack.pollFirst();
        }

        public void addIfUnmarked(final Object object) {
            if (object instanceof final AbstractSqueakObjectWithClassAndHash o && o.tryToMark(currentMarkingFlag)) {
                workStack.addFirst(o);
            }
        }

        public void addAllIfUnmarked(final Object[] objects) {
            for (final Object object : objects) {
                addIfUnmarked(object);
            }
        }

        private void tracePointers(final AbstractSqueakObjectWithClassAndHash object) {
            object.tracePointers(this);
            addIfUnmarked(object.getSqueakClass());
        }

        private AbstractSqueakObjectWithClassAndHash getNextWithLock() {
            lock.lock();
            try {
                return getNext();
            } finally {
                lock.unlock();
            }
        }

        private void runTasks(final Runnable[] tasks) {
            try (final ExecutorService executor = Executors.newFixedThreadPool(usableThreadCount)) {
                for (Runnable task : tasks) {
                    executor.submit(task);
                }
                executor.shutdown();
                boolean success = executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                if (!success) {
                    throw CompilerDirectives.shouldNotReachHere("runTasks timed out");
                }
            } catch (final InterruptedException e) {
                throw CompilerDirectives.shouldNotReachHere("runTasks was interrupted");
            }

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
