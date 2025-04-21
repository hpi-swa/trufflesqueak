/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interrupts;

import java.util.ArrayDeque;
import java.util.concurrent.locks.LockSupport;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

public final class CheckForInterruptsState {
    private static final String CHECK_FOR_INTERRUPTS_THREAD_NAME = "TruffleSqueakCheckForInterrupts";

    /*
     * Process switches are relatively expensive. So to give interrupts enough time to run,
     * TruffleSqueak uses an interval of 4ms by default (twice of what OpenSmalltalkVM uses).
     */
    private static final int DEFAULT_INTERRUPT_CHECK_NANOS = 4_000_000;

    private final SqueakImageContext image;
    private final ArrayDeque<Integer> semaphoresToSignal = new ArrayDeque<>();

    /**
     * `interruptCheckNanos` is the interval between updates to 'shouldTrigger'. This controls the
     * timing accuracy of Smalltalk Delays.
     */
    private long interruptCheckNanos = DEFAULT_INTERRUPT_CHECK_NANOS;

    private boolean isActive = true;
    private volatile boolean hasTriggered;
    private long nextWakeupTick;
    private boolean interruptPending;
    private boolean hasPendingFinalizations;

    /**
     * `shouldTrigger` is set to `true` by a dedicated thread. To guarantee atomicity, it would be
     * necessary to mark this field as `volatile` or use an `AtomicBoolean`. However, such a field
     * cannot be moved by the Graal compiler during compilation. Since atomicity is not needed for
     * the interrupt handler mechanism, we can use a standard boolean here for better compilation.
     */
    private boolean shouldTrigger;

    private Thread thread;

    public CheckForInterruptsState(final SqueakImageContext image) {
        this.image = image;
        if (image.options.disableInterruptHandler()) {
            image.printToStdOut("Interrupt handler disabled...");
        }
    }

    @TruffleBoundary
    public void start() {
        if (image.options.disableInterruptHandler()) {
            return;
        }
        thread = new CheckForInterruptsThread();
        thread.start();
    }

    final class CheckForInterruptsThread extends Thread {
        CheckForInterruptsThread() {
            super(CHECK_FOR_INTERRUPTS_THREAD_NAME);
            setDaemon(true);
        }

        @Override
        public void run() {
            while (true) {
                checkForInterrupts();
                LockSupport.parkNanos(interruptCheckNanos);
                if (Thread.interrupted()) {
                    break;
                }
            }
        }

        private void checkForInterrupts() {
            if (hasTriggered) {
                /*
                 * Handler has triggered recently, so skip the next check. This avoids that
                 * interrupts can be triggered more than once in the specified interval.
                 */
                hasTriggered = false;
                shouldTrigger = false;
            } else {
                shouldTrigger |= interruptPending || nextWakeUpTickTrigger() || hasPendingFinalizations || hasSemaphoresToSignal();
            }
        }
    }

    @TruffleBoundary
    public void shutdown() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    /* Interrupt check interval */

    public long getInterruptCheckMilliseconds() {
        return interruptCheckNanos / 1_000_000;
    }

    public void setInterruptCheckMilliseconds(final long milliseconds) {
        interruptCheckNanos = Math.max(DEFAULT_INTERRUPT_CHECK_NANOS, milliseconds * 1_000_000);
    }

    /* Interrupt trigger state */

    public boolean shouldSkip() {
        if (!isActive) {
            return true;
        }
        if (shouldTrigger) {
            shouldTrigger = false; // reset trigger
            return false;
        } else {
            return true;
        }
    }

    /* Enable / disable interrupts */

    public boolean isActive() {
        return isActive;
    }

    public void activate() {
        isActive = true;
    }

    public void deactivate() {
        isActive = false;
    }

    /* User interrupt */

    public boolean tryInterruptPending() {
        if (interruptPending) {
            LogUtils.INTERRUPTS.fine("User interrupt");
            interruptPending = false; // reset
            hasTriggered = true;
            return true;
        } else {
            return false;
        }
    }

    public void setInterruptPending() {
        interruptPending = true;
        shouldTrigger = true;
    }

    /* Timer interrupt */

    private boolean nextWakeUpTickTrigger() {
        if (nextWakeupTick != 0) {
            final long time = MiscUtils.currentTimeMillis();
            if (time >= nextWakeupTick) {
                LogUtils.INTERRUPTS.finer(() -> "Reached nextWakeupTick: " + nextWakeupTick);
                return true;
            }
        }
        return false;
    }

    public boolean tryWakeUpTickTrigger() {
        if (nextWakeUpTickTrigger()) {
            LogUtils.INTERRUPTS.fine("Timer interrupt");
            nextWakeupTick = 0; // reset
            hasTriggered = true;
            return true;
        } else {
            return false;
        }
    }

    public void setNextWakeupTick(final long msTime) {
        LogUtils.INTERRUPTS.finer(() -> {
            if (nextWakeupTick != 0) {
                return (msTime != 0 ? "Changing nextWakeupTick to " + msTime + " from " : "Resetting nextWakeupTick from ") + nextWakeupTick;
            } else {
                return msTime != 0 ? "Setting nextWakeupTick to " + msTime : "Resetting nextWakeupTick when it was already 0";
            }
        });
        nextWakeupTick = msTime;
    }

    /* Finalization interrupt */

    public boolean tryPendingFinalizations() {
        if (hasPendingFinalizations) {
            LogUtils.INTERRUPTS.fine("Finalization interrupt");
            hasPendingFinalizations = false;
            hasTriggered = true;
            return true;
        } else {
            return false;
        }
    }

    public void setPendingFinalizations() {
        hasPendingFinalizations = true;
        shouldTrigger = true;
    }

    /* Semaphore interrupts */

    private boolean hasSemaphoresToSignal() {
        return !semaphoresToSignal.isEmpty();
    }

    public boolean trySemaphoresToSignal() {
        if (hasSemaphoresToSignal()) {
            LogUtils.INTERRUPTS.fine("Semaphore interrupt");
            hasTriggered = true;
            return true;
        } else {
            return false;
        }
    }

    public Integer nextSemaphoreToSignal() {
        return semaphoresToSignal.pollFirst();
    }

    @TruffleBoundary
    public void signalSemaphoreWithIndex(final int index) {
        semaphoresToSignal.addLast(index);
        shouldTrigger = true;
    }

    /*
     * TESTING
     */

    public void clear() {
        nextWakeupTick = 0;
        interruptPending = false;
        hasPendingFinalizations = false;
        clearWeakPointersQueue();
        semaphoresToSignal.clear();
    }

    public void reset() {
        CompilerAsserts.neverPartOfCompilation("Resetting interrupt handler only supported for testing purposes");
        isActive = true;
        shutdown();
        clear();
    }

    private void clearWeakPointersQueue() {
        while (image.weakPointersQueue.poll() != null) {
            // Poll until empty.
        }
    }
}
