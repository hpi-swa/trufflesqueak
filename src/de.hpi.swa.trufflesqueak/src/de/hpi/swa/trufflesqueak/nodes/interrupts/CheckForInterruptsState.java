/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interrupts;

import java.util.ArrayDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

public final class CheckForInterruptsState {
    private static final String CHECK_FOR_INTERRUPTS_THREAD_NAME = "TruffleSqueakCheckForInterrupts";

    private static final int DEFAULT_INTERRUPT_CHECK_MILLISECONDS = 2;

    private final SqueakImageContext image;
    private ScheduledExecutorService executor;
    private final ArrayDeque<Integer> semaphoresToSignal = new ArrayDeque<>();

    /**
     * `interruptCheckMilliseconds` is the interval between updates to 'shouldTrigger'. This
     * controls the timing accuracy of Smalltalk Delays.
     */
    private int interruptCheckMilliseconds;

    private volatile boolean isActive = true;
    protected long nextWakeupTick;
    protected boolean interruptPending;
    private boolean hasPendingFinalizations;

    /**
     * `shouldTrigger` is set to `true` by a dedicated thread. To guarantee atomicity, it would be
     * necessary to mark this field as `volatile` or use an `AtomicBoolean`. However, such a field
     * cannot be moved by the Graal compiler during compilation. Since atomicity is not needed for
     * the interrupt handler mechanism, we can use a standard boolean here for better compilation.
     */
    private boolean shouldTrigger;

    private ScheduledFuture<?> interruptChecks;

    public CheckForInterruptsState(final SqueakImageContext image) {
        this.image = image;
        this.interruptCheckMilliseconds = DEFAULT_INTERRUPT_CHECK_MILLISECONDS;
        if (image.options.disableInterruptHandler()) {
            image.printToStdOut("Interrupt handler disabled...");
        }
    }

    @TruffleBoundary
    public void start() {
        if (image.options.disableInterruptHandler()) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, CHECK_FOR_INTERRUPTS_THREAD_NAME);
            t.setDaemon(true);
            t.setPriority(Thread.MAX_PRIORITY);
            return t;
        });
        createOrUpdateInterruptChecks();
    }

    @TruffleBoundary
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @TruffleBoundary
    private void createOrUpdateInterruptChecks() {
        if (executor != null) {
            if (interruptChecks != null && !interruptChecks.isCancelled()) {
                interruptChecks.cancel(false);
            }
            interruptChecks = executor.scheduleWithFixedDelay(
                            this::setShouldTriggerIfNeeded,
                            interruptCheckMilliseconds,
                            interruptCheckMilliseconds,
                            TimeUnit.MILLISECONDS);
        }
    }

    /* Interrupt check interval */

    public int getInterruptCheckMilliseconds() {
        return interruptCheckMilliseconds;
    }

    public void setInterruptCheckMilliseconds(final int milliseconds) {
        interruptCheckMilliseconds = Math.max(1, milliseconds);
        createOrUpdateInterruptChecks();
    }

    /* Interrupt trigger state */

    public boolean shouldTrigger() {
        return shouldTrigger;
    }

    public void resetTrigger() {
        /**
         * CheckForInterrupts***Node signals semaphores as part of interrupt handling. If the
         * semaphore signalled wakes a process with higher priority than the current process, a
         * ProcessSwitch exception will be raised, and the remaining interrupts will be left
         * unhandled. Rather than simply resetting shouldTrigger, we must recompute it.
         */
        shouldTrigger = false;
        setShouldTriggerIfNeeded();
    }

    private void setShouldTriggerIfNeeded() {
        if (!shouldTrigger) {
            shouldTrigger = isActive && (interruptPending() || nextWakeUpTickTrigger() || hasPendingFinalizations() || hasSemaphoresToSignal());
        }
    }

    /* Enable / disable interrupts */

    public boolean isActive() {
        return isActive;
    }

    public void activate() {
        isActive = true;
        setShouldTriggerIfNeeded();
    }

    public void deactivate() {
        /* avoid race condition caused by interference from interruptChecks thread */
        shouldTrigger = true;
        isActive = false;
        shouldTrigger = false;
    }

    /* User interrupt */

    protected boolean interruptPending() {
        return interruptPending;
    }

    public void setInterruptPending() {
        interruptPending = true;
        shouldTrigger = isActive;
    }

    /* Timer interrupt */

    protected boolean nextWakeUpTickTrigger() {
        if (nextWakeupTick != 0) {
            final long time = MiscUtils.currentTimeMillis();
            if (time >= nextWakeupTick) {
                LogUtils.INTERRUPTS.finer(() -> "Reached nextWakeupTick: " + nextWakeupTick);
                return true;
            }
        }
        return false;
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

    protected boolean hasPendingFinalizations() {
        return hasPendingFinalizations;
    }

    public void clearPendingFinalizations() {
        hasPendingFinalizations = false;
    }

    public void setPendingFinalizations() {
        hasPendingFinalizations = true;
        shouldTrigger = isActive;
    }

    /* Semaphore interrupts */

    protected boolean hasSemaphoresToSignal() {
        return !semaphoresToSignal.isEmpty();
    }

    protected Integer nextSemaphoreToSignal() {
        return semaphoresToSignal.pollFirst();
    }

    @TruffleBoundary
    public void signalSemaphoreWithIndex(final int index) {
        semaphoresToSignal.addLast(index);
        shouldTrigger = isActive;
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
        if (interruptChecks != null) {
            interruptChecks.cancel(true);
        }
        shutdown();
        clear();
    }

    private void clearWeakPointersQueue() {
        while (image.weakPointersQueue.poll() != null) {
            // Poll until empty.
        }
    }
}
