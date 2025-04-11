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

    private static final int INTERRUPT_CHECKS_EVERY_N_MILLISECONDS = 20;

    private final SqueakImageContext image;
    private ScheduledExecutorService executor;
    private final ArrayDeque<Integer> semaphoresToSignal = new ArrayDeque<>();

    private boolean isActive = true;
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
            return t;
        });
        interruptChecks = executor.scheduleWithFixedDelay(() -> {
            if (!shouldTrigger) updateShouldTrigger();
        }, INTERRUPT_CHECKS_EVERY_N_MILLISECONDS, INTERRUPT_CHECKS_EVERY_N_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    @TruffleBoundary
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    private void updateShouldTrigger() {
        shouldTrigger = isActive && (interruptPending() || nextWakeUpTickTrigger() || hasPendingFinalizations() || hasSemaphoresToSignal());
    }

    public boolean shouldTrigger() {
        return shouldTrigger;
    }

    public void resetTrigger() {
        /*
         CheckForInterrupts***Node signals semaphores as part of interrupt handling. If the semaphore signalled wakes a
         process with higher priority than the current process, a ProcessSwitch exception will be raised, and the
         remaining interrupts will be left unhandled. Rather than simply resetting shouldTrigger, we must recompute it.
        */
        updateShouldTrigger();
    }

    public void setInterruptPending() {
        interruptPending = true;
        shouldTrigger = isActive;
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

    public boolean isActive() {
        return isActive;
    }

    public void activate() {
        isActive = true;
    }

    public void deactivate() {
        isActive = false;
        resetTrigger();
    }

    protected boolean interruptPending() {
        return interruptPending;
    }

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

    public void clearPendingFinalizations() { hasPendingFinalizations = false; }

    public void setPendingFinalizations() {
        hasPendingFinalizations = true;
        shouldTrigger = isActive;
    }

    protected boolean hasPendingFinalizations() { return hasPendingFinalizations; }

    protected boolean hasSemaphoresToSignal() {
        return !semaphoresToSignal.isEmpty();
    }

    protected Integer nextSemaphoreToSignal() {
        return semaphoresToSignal.pollFirst();
    }

    public static int getInterruptChecksEveryNms() { return INTERRUPT_CHECKS_EVERY_N_MILLISECONDS; }

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
