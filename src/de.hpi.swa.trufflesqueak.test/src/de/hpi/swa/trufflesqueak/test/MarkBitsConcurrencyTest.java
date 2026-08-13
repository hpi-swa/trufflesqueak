/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithHash;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;

@SuppressWarnings("static-method")
public final class MarkBitsConcurrencyTest extends AbstractSqueakTestCaseWithDummyImage {
    private static final long FLAG_ITERATIONS = 10_000_000;
    private static final int MARK_OBJECT_COUNT = 20_000;
    private static final int MARK_THREAD_COUNT = 4;

    /**
     * Verifies that concurrent bit-field updates (boolean flags vs. GC mark bits) are thread-safe.
     * One thread toggles boolean flags while another marks/unmarks the object; neither may lose
     * updates or mutate the identity hash.
     */
    @Test
    public void testFlagAndMarkingBitsDoNotLoseUpdates() throws InterruptedException {
        final PointersObject object = instantiate(createFreshTestClass());
        final int hash = 0x2A2A2A;
        object.setSqueakHash(hash);
        object.setBooleanDBit(); /* Set once, must never be observed unset again. */

        final Queue<String> failures = new ConcurrentLinkedQueue<>();
        final CountDownLatch start = new CountDownLatch(1);

        final Thread booleanBitsThread = new Thread(() -> {
            try {
                start.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for (long i = 0; i < FLAG_ITERATIONS; i++) {
                object.setBooleanABit();
                if (!object.isBooleanASet()) {
                    failures.add("Lost setBooleanABit in iteration " + i);
                    return;
                }
                object.clearBooleanABit();
                if (object.isBooleanASet()) {
                    failures.add("Lost clearBooleanABit in iteration " + i);
                    return;
                }
                if (!object.isBooleanDSet()) {
                    failures.add("Lost the untouched boolean D bit in iteration " + i);
                    return;
                }
                if (object.getSqueakHashInt() != hash) {
                    failures.add("Hash changed to " + object.getSqueakHashInt() + " in iteration " + i);
                    return;
                }
            }
        }, "booleanBitsThread");

        final Thread markingBitsThread = new Thread(() -> {
            try {
                start.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for (long i = 0; i < FLAG_ITERATIONS; i++) {
                if (!object.tryToMarkWith(true)) {
                    failures.add("Could not mark in iteration " + i);
                    return;
                }
                if (!object.isMarkedWith(true)) {
                    failures.add("Lost the mark in iteration " + i);
                    return;
                }
                object.unmarkWith(true);
                if (object.isMarkedWith(true)) {
                    failures.add("Lost the unmark in iteration " + i);
                    return;
                }
            }
        }, "markingBitsThread");

        booleanBitsThread.start();
        markingBitsThread.start();
        start.countDown();
        booleanBitsThread.join();
        markingBitsThread.join();

        assertEquals("Lost updates: " + failures, 0, failures.size());
        assertEquals("Hash survived", hash, object.getSqueakHashInt());
        assertTrue("Boolean D bit survived", object.isBooleanDSet());
    }

    /**
     * Verifies that marking objects is atomic across concurrent threads.
     * When multiple threads race to mark the same object, {@code tryToMarkWith(true)} must return
     * {@code true} for exactly 1 thread and {@code false} for all others. Across N objects,
     * the total number of successful marks across all threads must equal N.
     */
    @Test
    public void testConcurrentMarkingMarksEachObjectExactlyOnce() throws InterruptedException {
        final ClassObject testClass = createFreshTestClass();
        final List<AbstractSqueakObjectWithHash> objects = new ArrayList<>(MARK_OBJECT_COUNT);
        for (int i = 0; i < MARK_OBJECT_COUNT; i++) {
            objects.add(instantiate(testClass));
        }

        final AtomicLong marked = new AtomicLong();
        final CountDownLatch start = new CountDownLatch(1);
        final Thread[] threads = new Thread[MARK_THREAD_COUNT];
        for (int t = 0; t < threads.length; t++) {
            /* Walk in opposite directions to widen the window in which two threads meet. */
            final boolean forwards = t % 2 == 0;
            threads[t] = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                long count = 0;
                for (int i = 0; i < MARK_OBJECT_COUNT; i++) {
                    final AbstractSqueakObjectWithHash object = objects.get(forwards ? i : MARK_OBJECT_COUNT - 1 - i);
                    if (object.tryToMarkWith(true)) {
                        count++;
                    }
                }
                marked.addAndGet(count);
            }, "graphWalkerThread" + t);
        }
        for (final Thread thread : threads) {
            thread.start();
        }
        start.countDown();
        for (final Thread thread : threads) {
            thread.join();
        }

        assertEquals("Every object marked exactly once", MARK_OBJECT_COUNT, marked.get());
        for (final AbstractSqueakObjectWithHash object : objects) {
            assertTrue(object.isMarkedWith(true));
            object.unmarkWith(true);
            assertFalse(object.isMarkedWith(true));
        }
    }
}
