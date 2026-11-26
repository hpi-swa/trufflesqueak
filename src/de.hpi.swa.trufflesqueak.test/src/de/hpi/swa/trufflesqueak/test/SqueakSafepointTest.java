/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.AfterClass;
import org.junit.Test;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject.CompiledCodeHeaderUtils;
import de.hpi.swa.trufflesqueak.model.NilObject;

/**
 * Interpreted loops without sends must poll Truffle safepoints, so that thread-local actions such
 * as {@link org.graalvm.polyglot.Context#interrupt(Duration)} reach them. Unit tests run on the
 * fallback runtime, so only the interpreter can poll here.
 */
public final class SqueakSafepointTest extends AbstractSqueakTestCaseWithDummyImage {
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static boolean loopStillRunning;

    /** Shadows the superclass teardown, which would wait forever for a loop that never polls. */
    @AfterClass
    public static void tearDown() {
        if (!loopStillRunning) {
            destroyImageContext();
        }
    }

    @Test
    public void testInterruptLoopSistaV1() throws Exception {
        // 0 <E1 FF> extB: -1, 2 <ED FC> jump: -4 (back to pc 0)
        assertInterruptible(makeMethod(CompiledCodeHeaderUtils.makeHeaderWord(false, 0, 0, 2, false, false), new Object[0], 0xE1, 0xFF, 0xED, 0xFC));
    }

    private static void assertInterruptible(final CompiledCodeObject method) throws Exception {
        final AtomicReference<Throwable> result = new AtomicReference<>();
        final CountDownLatch entered = new CountDownLatch(1);
        final Thread worker = new Thread(() -> {
            context.enter();
            entered.countDown();
            try {
                runMethod(method, NilObject.SINGLETON);
            } catch (final Throwable t) {
                result.set(t);
            } finally {
                context.leave();
            }
        });
        worker.setDaemon(true);
        context.leave(); // The superclass setup entered the context on this thread.
        try {
            worker.start();
            loopStillRunning = true;
            entered.await(); // Interrupt only reaches threads that have entered the context.
            context.interrupt(TIMEOUT); // Throws a TimeoutException if the loop never polls.
            worker.join(TIMEOUT.toMillis());
            loopStillRunning = worker.isAlive();
            assertFalse("loop was not interrupted", loopStillRunning);
            assertNotNull("loop must end with the interrupt", result.get());
        } finally {
            context.enter();
        }
    }
}
