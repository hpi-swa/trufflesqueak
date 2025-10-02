/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.MapCursor;
import org.graalvm.collections.UnmodifiableEconomicMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
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
    private static final int USABLE_THREAD_COUNT = Math.min(Runtime.getRuntime().availableProcessors(), 4);
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(USABLE_THREAD_COUNT, r -> new Thread(r, "TruffleSqueakObjectGraphUtils"));

    private final SqueakImageContext image;
    private final boolean trackOperations;

    private final EconomicMap<Object, Object> becomeMap = EconomicMap.create(Equivalence.IDENTITY);

    public ObjectGraphUtils(final SqueakImageContext image) {
        this.image = image;
        this.trackOperations = image.options.printResourceSummary() || LogUtils.OBJECT_GRAPH.isLoggable(Level.FINE);
    }

    public static int getLastSeenObjects() {
        return lastSeenObjects;
    }

    public SqueakImageContext getImage() {
        return image;
    }

    static final class AllInstancesTask implements Runnable {
        private final ObjectTracer roots;
        private final ArrayDeque<AbstractSqueakObjectWithClassAndHash> objects;
        private final UnmodifiableEconomicMap<Object, Object> fromToMap;

        AllInstancesTask(final ObjectTracer theRoots, final ArrayDeque<AbstractSqueakObjectWithClassAndHash> theObjects, final UnmodifiableEconomicMap<Object, Object> theFromToMap) {
            roots = theRoots;
            objects = theObjects;
            fromToMap = theFromToMap;
        }

        public void run() {
            final ObjectTracer tracer = roots.copyEmpty();
            AbstractSqueakObjectWithClassAndHash root;
            while ((root = roots.getNextWithLock()) != null) {
                AbstractSqueakObjectWithClassAndHash currentObject = root;
                do {
                    currentObject.pointersBecomeOneWay(fromToMap);
                    objects.add(currentObject);
                    tracer.tracePointers(currentObject);
                } while ((currentObject = tracer.getNext()) != null);
            }
        }
    }

    @TruffleBoundary
    public Object[] allInstances() {
        final long startTime = System.nanoTime();

        final ObjectTracer roots = ObjectTracer.fromRoots(image, true);

        final Runnable[] tasks = new Runnable[USABLE_THREAD_COUNT];
        final List<ArrayDeque<AbstractSqueakObjectWithClassAndHash>> objectsList = new ArrayList<>(USABLE_THREAD_COUNT);
        final int initialSize = (lastSeenObjects + ADDITIONAL_SPACE) / USABLE_THREAD_COUNT;
        for (int i = 0; i < USABLE_THREAD_COUNT; i++) {
            final ArrayDeque<AbstractSqueakObjectWithClassAndHash> objects = new ArrayDeque<>(initialSize);
            objectsList.add(objects);
            tasks[i] = new AllInstancesTask(roots, objects, becomeMap);
        }
        runTasks(tasks);
        postProcessBecomeMap(null);

        int totalSize = 0;
        for (ArrayDeque<AbstractSqueakObjectWithClassAndHash> deque : objectsList) {
            totalSize += deque.size();
        }
        lastSeenObjects = totalSize;

        final AbstractSqueakObjectWithClassAndHash[] result = new AbstractSqueakObjectWithClassAndHash[totalSize];
        int i = 0;
        for (final ArrayDeque<AbstractSqueakObjectWithClassAndHash> deque : objectsList) {
            for (final AbstractSqueakObjectWithClassAndHash value : deque) {
                result[i++] = value;
            }
        }

        if (trackOperations) {
            ObjectGraphOperations.ALL_INSTANCES.addNanos(System.nanoTime() - startTime);
        }
        return result;
    }

    static final class AllInstancesOfTask implements Runnable {
        private final ClassObject targetClass;
        private final ObjectTracer roots;
        private final ArrayDeque<AbstractSqueakObjectWithClassAndHash> objects;
        private final UnmodifiableEconomicMap<Object, Object> fromToMap;

        AllInstancesOfTask(final ClassObject theTargetClass, final ObjectTracer theRoots, final ArrayDeque<AbstractSqueakObjectWithClassAndHash> theObjects,
                        final UnmodifiableEconomicMap<Object, Object> theFromToMap) {
            targetClass = theTargetClass;
            roots = theRoots;
            objects = theObjects;
            fromToMap = theFromToMap;
        }

        public void run() {
            final ObjectTracer tracer = roots.copyEmpty();
            AbstractSqueakObjectWithClassAndHash root;
            while ((root = roots.getNextWithLock()) != null) {
                AbstractSqueakObjectWithClassAndHash currentObject = root;
                do {
                    currentObject.pointersBecomeOneWay(fromToMap);
                    if (targetClass == currentObject.getSqueakClass()) {
                        objects.add(currentObject);
                    }
                    tracer.tracePointers(currentObject);
                } while ((currentObject = tracer.getNext()) != null);
            }
        }
    }

    @TruffleBoundary
    public Object[] allInstancesOf(final ClassObject targetClass) {
        final long startTime = System.nanoTime();

        final ObjectTracer roots = ObjectTracer.fromRoots(image, true);

        final Runnable[] tasks = new Runnable[USABLE_THREAD_COUNT];
        final List<ArrayDeque<AbstractSqueakObjectWithClassAndHash>> objectsList = new ArrayList<>(USABLE_THREAD_COUNT);
        for (int i = 0; i < USABLE_THREAD_COUNT; i++) {
            final ArrayDeque<AbstractSqueakObjectWithClassAndHash> objects = new ArrayDeque<>();
            objectsList.add(objects);
            tasks[i] = new AllInstancesOfTask(targetClass, roots, objects, becomeMap);
        }
        runTasks(tasks);
        postProcessBecomeMap(targetClass);

        int totalSize = 0;
        for (ArrayDeque<AbstractSqueakObjectWithClassAndHash> deque : objectsList) {
            totalSize += deque.size();
        }

        final AbstractSqueakObjectWithClassAndHash[] result = new AbstractSqueakObjectWithClassAndHash[totalSize];
        int i = 0;
        for (final ArrayDeque<AbstractSqueakObjectWithClassAndHash> deque : objectsList) {
            for (final AbstractSqueakObjectWithClassAndHash value : deque) {
                result[i++] = value;
            }
        }

        if (trackOperations) {
            ObjectGraphOperations.ALL_INSTANCES_OF.addNanos(System.nanoTime() - startTime);
        }
        return result;
    }

    @TruffleBoundary
    public AbstractSqueakObject someInstanceOf(final ClassObject targetClass) {
        final long startTime = System.nanoTime();
        final ArrayDeque<AbstractSqueakObjectWithClassAndHash> marked = new ArrayDeque<>(lastSeenObjects / 2);
        final ObjectTracer tracer = ObjectTracer.fromRoots(image, true);
        AbstractSqueakObject result = NilObject.SINGLETON;
        AbstractSqueakObjectWithClassAndHash currentObject;
        while ((currentObject = tracer.getNext()) != null) {
            marked.add(currentObject);
            if (targetClass == currentObject.getSqueakClass()) {
                result = currentObject;
                // Unmark marked objects
                tracer.unmarkAll(marked);
                // Restore marking flag
                image.toggleCurrentMarkingFlag();
                break;
            } else {
                tracer.tracePointers(currentObject);
            }
        }
        if (trackOperations) {
            ObjectGraphOperations.SOME_INSTANCE_OF.addNanos(System.nanoTime() - startTime);
        }
        return result;
    }

    @TruffleBoundary
    public AbstractSqueakObject nextObject(final AbstractSqueakObjectWithClassAndHash targetObject) {
        // Should not be parallelized since Smalltalk is assuming a fixed order enumeration
        // of all live objects and a parallel search is likely to produce a different result
        // each time it is called.
        final long startTime = System.nanoTime();
        final ArrayDeque<AbstractSqueakObjectWithClassAndHash> marked = new ArrayDeque<>(lastSeenObjects / 2);
        final ObjectTracer tracer = ObjectTracer.fromRoots(image, true);
        AbstractSqueakObjectWithClassAndHash currentObject = tracer.getNext();
        AbstractSqueakObject result = currentObject; // first object
        boolean foundObject = false;
        while (currentObject != null) {
            if (foundObject) {
                result = currentObject;
                // Unmark marked objects
                tracer.unmarkAll(marked);
                // Restore marking flag
                image.toggleCurrentMarkingFlag();
                break;
            }
            marked.add(currentObject);
            if (currentObject == targetObject) {
                foundObject = true;
            }
            tracer.tracePointers(currentObject);
            currentObject = tracer.getNext();
        }
        if (trackOperations) {
            ObjectGraphOperations.NEXT_OBJECT.addNanos(System.nanoTime() - startTime);
        }
        return result;
    }

    @TruffleBoundary
    public void pointersBecomeOneWay(final Object[] fromPointers, final Object[] toPointers, final boolean copyHash) {
        final long startTime = System.nanoTime();
        boolean allAbstractSqueakObjectWithClassAndHash = true;
        for (int i = 0; i < fromPointers.length; i++) {
            final Object from = fromPointers[i];
            final Object to = toPointers[i];
            if (from != to) {
                becomeMap.put(from, to);
                if (from instanceof final AbstractSqueakObjectWithClassAndHash f && to instanceof final AbstractSqueakObjectWithClassAndHash t) {
                    if (copyHash) {
                        t.setSqueakHash(MiscUtils.toIntExact(f.getOrCreateSqueakHash()));
                    }
                    f.forwardTo(t);
                } else {
                    allAbstractSqueakObjectWithClassAndHash = false;
                }
            }
        }
        if (!becomeMap.isEmpty()) {
            flattenForwardPointers();
            if (allAbstractSqueakObjectWithClassAndHash) {
                pointersBecomeOneWayFrames(o -> {
                }, becomeMap);
            } else {
                final ObjectTracer roots = ObjectTracer.fromRoots(image, false);
                pointersBecomeOneWayFrames(roots::addIfUnmarked, becomeMap);
                becomeOneWayManyPairs(roots);
            }
        }
        if (trackOperations) {
            ObjectGraphOperations.POINTERS_BECOME_ONE_WAY.addNanos(System.nanoTime() - startTime);
        }
    }

    /* Ensure forward pointers are never recursive. */
    private void flattenForwardPointers() {
        final MapCursor<Object, Object> cursor = becomeMap.getEntries();
        while (cursor.advance()) {
            final Object toPointer = becomeMap.get(cursor.getValue());
            if (toPointer instanceof final AbstractSqueakObjectWithClassAndHash to) {
                /* Current value is also key, so update forward object of value's key. */
                ((AbstractSqueakObjectWithClassAndHash) cursor.getKey()).forwardTo(to);
                cursor.setValue(to);
            }
        }
    }

    public boolean isUnfollowNeeded() {
        return !becomeMap.isEmpty();
    }

    @TruffleBoundary
    public void unfollow() {
        if (becomeMap.isEmpty()) {
            return; // nothing to do
        }
        final long startTime = System.nanoTime();
        becomeOneWayManyPairs(ObjectTracer.fromRoots(image, true));
        if (trackOperations) {
            ObjectGraphOperations.UNFOLLOW.addNanos(System.nanoTime() - startTime);
        }
    }

    private void becomeOneWayManyPairs(final ObjectTracer roots) {
        final Runnable[] tasks = new Runnable[USABLE_THREAD_COUNT];
        for (int i = 0; i < USABLE_THREAD_COUNT; i++) {
            tasks[i] = new BecomeOneWayManyPairsTask(roots, becomeMap);
        }
        runTasks(tasks); // TODO: run these tasks async?
        postProcessBecomeMap(null);
    }

    static final class BecomeOneWayManyPairsTask implements Runnable {
        private final ObjectTracer roots;
        private final UnmodifiableEconomicMap<Object, Object> fromToMap;

        BecomeOneWayManyPairsTask(final ObjectTracer theRoots, final UnmodifiableEconomicMap<Object, Object> theFromToMap) {
            roots = theRoots;
            fromToMap = theFromToMap;
        }

        public void run() {
            final ObjectTracer tracer = roots.copyEmpty();
            AbstractSqueakObjectWithClassAndHash root;
            while ((root = roots.getNextWithLock()) != null) {
                AbstractSqueakObjectWithClassAndHash currentObject = root;
                do {
                    currentObject.pointersBecomeOneWay(fromToMap);
                    tracer.tracePointers(currentObject);
                } while ((currentObject = tracer.getNext()) != null);
            }
        }
    }

    @TruffleBoundary
    private static void pointersBecomeOneWayFrames(final Consumer<Object> tracer, final UnmodifiableEconomicMap<Object, Object> fromToMap) {
        final ContextObject resumeContextObject = Truffle.getRuntime().iterateFrames((frameInstance) -> {
            final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE);
            if (!FrameAccess.isTruffleSqueakFrame(current)) {
                return FrameAccess.getResumingContextObjectOrSkip(frameInstance);
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
                tracer.accept(arguments[i]);
            }

            final ContextObject context = FrameAccess.getContext(current);
            if (context != null) {
                final Object toContext = fromToMap.get(context);
                if (toContext instanceof final ContextObject o) {
                    FrameAccess.setContext(current, o);
                }
                tracer.accept(FrameAccess.getContext(current));
            }

            /*
             * Iterate over all stack slots here instead of stackPointer because in rare cases, the
             * stack is accessed behind the stackPointer.
             */
            FrameAccess.iterateStackObjectsWithReplacement(current, false, stackObject -> {
                if (stackObject != null) {
                    final Object migratedObject = fromToMap.get(stackObject);
                    tracer.accept(Objects.requireNonNullElse(migratedObject, stackObject));
                    return migratedObject;
                } else {
                    return null;
                }
            });
            return null;
        });
        if (resumeContextObject != null) {
            tracer.accept(resumeContextObject);
        } else {
            // TODO: find out when and why this happens
            LogUtils.OBJECT_GRAPH.warning("Failed to find ResumeContextRootNode");
        }
    }

    static final class EphemeronsTask implements Runnable {
        private final ObjectTracer roots;
        private final ArrayDeque<EphemeronObject> ephemeronsToBeTraced;
        private final UnmodifiableEconomicMap<Object, Object> fromToMap;

        EphemeronsTask(final ObjectTracer theRoots, final ArrayDeque<EphemeronObject> ephemerons, final UnmodifiableEconomicMap<Object, Object> theFromToMap) {
            roots = theRoots;
            ephemeronsToBeTraced = ephemerons;
            fromToMap = theFromToMap;
        }

        public void run() {
            final ObjectTracer tracer = roots.copyEmpty();
            AbstractSqueakObjectWithClassAndHash root;
            while ((root = roots.getNextWithLock()) != null) {
                AbstractSqueakObjectWithClassAndHash currentObject = root;
                do {
                    currentObject.pointersBecomeOneWay(fromToMap);
                    // Ephemerons are traced in a special way.
                    if (currentObject instanceof final EphemeronObject ephemeronObject) {
                        // An Ephemeron is traced normally if it has been signaled or its key has
                        // been marked already. Otherwise, they are traced after all other objects.
                        if (ephemeronObject.hasBeenSignaled() || ephemeronObject.keyHasBeenMarked(tracer)) {
                            tracer.tracePointers(currentObject);
                        } else {
                            synchronized (ephemeronsToBeTraced) {
                                ephemeronsToBeTraced.add(ephemeronObject);
                            }
                        }
                    } else {
                        // Normal object
                        tracer.tracePointers(currentObject);
                    }
                } while ((currentObject = tracer.getNext()) != null);
            }
        }
    }

    @TruffleBoundary
    public boolean checkEphemerons() {
        final long startTime = System.nanoTime();

        final ObjectTracer roots = ObjectTracer.fromRoots(image, true);

        // Mark and trace all non-ephemeron objects. Mark and trace ephemerons that have
        // been signaled or whose keys have been marked. Save all other ephemerons for later.
        final Runnable[] tasks = new Runnable[USABLE_THREAD_COUNT];
        final ArrayDeque<EphemeronObject> ephemeronsToBeTraced = new ArrayDeque<>();
        for (int i = 0; i < USABLE_THREAD_COUNT; i++) {
            tasks[i] = new EphemeronsTask(roots, ephemeronsToBeTraced, becomeMap);
        }
        runTasks(tasks);
        postProcessBecomeMap(null);

        // Now, trace the ephemerons until there are only ephemerons whose keys are reachable
        // through ephemerons.
        traceRemainingEphemerons(ephemeronsToBeTraced, roots);

        // Make sure that they do not signal more than once.
        image.ephemeronsQueue.addAll(ephemeronsToBeTraced);
        for (EphemeronObject ephemeronObject : ephemeronsToBeTraced) {
            ephemeronObject.setHasBeenSignaled();
        }
        if (trackOperations) {
            ObjectGraphOperations.CHECK_EPHEMERONS.addNanos(System.nanoTime() - startTime);
        }
        return true;
    }

    private static void traceRemainingEphemerons(final ArrayDeque<EphemeronObject> ephemeronsToBeMarked, final ObjectTracer tracer) {
        // Trace the ephemerons that have marked keys until there are only ephemerons with unmarked
        // keys left.
        while (true) {
            boolean finished = true;
            final Iterator<EphemeronObject> iterator = ephemeronsToBeMarked.iterator();
            while (iterator.hasNext()) {
                final EphemeronObject ephemeronObject = iterator.next();
                if (ephemeronObject.keyHasBeenMarked(tracer)) {
                    tracer.tracePointers(ephemeronObject);
                    iterator.remove();
                    finished = false;
                }
            }
            if (finished) {
                break;
            }
            finishPendingMarking(tracer);
        }

        if (ephemeronsToBeMarked.isEmpty()) {
            return;
        }

        // Now, we have ephemerons whose keys are reachable only through ephemerons.
        // Mark them to keep consistent marking flags.
        for (EphemeronObject ephemeronObject : ephemeronsToBeMarked) {
            tracer.tracePointers(ephemeronObject);
        }
        finishPendingMarking(tracer);
    }

    private static void finishPendingMarking(final ObjectTracer tracer) {
        AbstractSqueakObjectWithClassAndHash currentObject;
        while ((currentObject = tracer.getNext()) != null) {
            tracer.tracePointers(currentObject);
        }
    }

    private static void runTasks(final Runnable[] tasks) {
        try {
            final Future<?>[] futures = new Future<?>[USABLE_THREAD_COUNT];
            for (int i = 0; i < USABLE_THREAD_COUNT; i++) {
                futures[i] = EXECUTOR.submit(tasks[i]);
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (final ExecutionException | InterruptedException e) {
            throw CompilerDirectives.shouldNotReachHere("runTasks was interrupted", e);
        }
    }

    private void postProcessBecomeMap(final ClassObject classObjectOrNull) {
        image.purgeDuplicateAndUnreachableClassTableEntriesFor(classObjectOrNull, becomeMap);
        becomeMap.clear();
    }

    public static final class ObjectTracer {
        /* Using a stack for DFS traversal when walking Smalltalk objects. */
        private final ArrayDeque<AbstractSqueakObjectWithClassAndHash> workStack = new ArrayDeque<>();
        private final boolean currentMarkingFlag;

        private ObjectTracer(final boolean markingFlag) {
            this.currentMarkingFlag = markingFlag;
        }

        /** Return an empty traversal based on this root traversal. */
        public ObjectTracer copyEmpty() {
            return new ObjectTracer(currentMarkingFlag);
        }

        /**
         * Return an ObjectTracer initialized with the roots and optionally including objects in the
         * Truffle frames. Flips the global marking flag.
         */
        private static ObjectTracer fromRoots(final SqueakImageContext image, final boolean addObjectsFromFrames) {
            final ObjectTracer tracer = new ObjectTracer(image.toggleCurrentMarkingFlag());
            tracer.addRoots(image, addObjectsFromFrames);
            return tracer;
        }

        private void addRoots(final SqueakImageContext image, final boolean addObjectsFromFrames) {
            /* Add roots, in reversed order because workStack is LIFO. */
            if (addObjectsFromFrames) {
                addObjectsFromFrames();
            }

            /*
             * Unreachable ephemerons in the queue must be kept visible to the rest of the image.
             * These are technically "dead" and do not need to be saved when the image is stored on
             * disk, but by tracing them we avoid an expensive reachability test in the
             * fetch-next-mourner primitive.
             */
            for (final AbstractSqueakObjectWithClassAndHash object : image.ephemeronsQueue) {
                addIfUnmarked(object);
            }

            /* Trace the special objects to give separate roots to the threads. */
            addIfUnmarked(image.specialObjectsArray);
            tracePointers(image.specialObjectsArray);
        }

        private void addObjectsFromFrames() {
            CompilerAsserts.neverPartOfCompilation();
            final ContextObject resumeContextObject = Truffle.getRuntime().iterateFrames(frameInstance -> {
                final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                if (!FrameAccess.isTruffleSqueakFrame(current)) {
                    return FrameAccess.getResumingContextObjectOrSkip(frameInstance);
                }
                addAllIfUnmarked(current.getArguments());
                addIfUnmarked(FrameAccess.getContext(current));
                FrameAccess.iterateStackObjects(current, false, this::addIfUnmarked);
                return null; // continue
            });
            assert resumeContextObject != null : "Failed to find ResumeContextRootNode";
            addIfUnmarked(resumeContextObject);
        }

        private AbstractSqueakObjectWithClassAndHash getNext() {
            return workStack.pollFirst();
        }

        private synchronized AbstractSqueakObjectWithClassAndHash getNextWithLock() {
            return workStack.pollFirst();
        }

        public boolean isMarked(final AbstractSqueakObjectWithClassAndHash object) {
            return object.isMarkedWith(currentMarkingFlag);
        }

        public void addIfUnmarked(final AbstractSqueakObjectWithClassAndHash object) {
            if (object != null) {
                addNonNullIfUnmarked(object);
            }
        }

        public void addIfUnmarked(final Object object) {
            if (object instanceof final AbstractSqueakObjectWithClassAndHash o) {
                addNonNullIfUnmarked(o);
            }
        }

        private void addNonNullIfUnmarked(final AbstractSqueakObjectWithClassAndHash object) {
            final AbstractSqueakObjectWithClassAndHash o = object.resolveForwardingPointer();
            if (o.tryToMarkWith(currentMarkingFlag)) {
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
        }

        /**
         * Unmark all objects remaining in the object graph traversal AND in the argument.
         */
        private void unmarkAll(final ArrayDeque<AbstractSqueakObjectWithClassAndHash> objects) {
            for (final AbstractSqueakObjectWithClassAndHash object : workStack) {
                object.unmarkWith(currentMarkingFlag);
            }
            for (final AbstractSqueakObjectWithClassAndHash object : objects) {
                object.unmarkWith(currentMarkingFlag);
            }
        }
    }

    public enum ObjectGraphOperations {
        ALL_INSTANCES("allInstances"),
        ALL_INSTANCES_OF("allInstancesOf"),
        SOME_INSTANCE_OF("someInstanceOf"),
        NEXT_OBJECT("nextObject"),
        POINTERS_BECOME_ONE_WAY("pointersBecomeOneWay"),
        UNFOLLOW("unfollow"),
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
