/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.util.ArrayDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;

public final class InterruptHandlerState {
    private static final int INTERRUPT_CHECKS_EVERY_N_MILLISECONDS = 3;

    private final SqueakImageContext image;
    private ScheduledThreadPoolExecutor executor;
    private final ArrayDeque<Integer> semaphoresToSignal = new ArrayDeque<>();

    private boolean isActive = true;
    protected long nextWakeupTick;
    protected boolean interruptPending;
    private boolean pendingFinalizationSignals;

    /**
     * `shouldTrigger` is set to `true` by a dedicated thread. To guarantee atomicity, it would be
     * necessary to mark this field as `volatile` or use an `AtomicBoolean`. However, such a field
     * cannot be moved by the Graal compiler during compilation. Since atomicity is not needed for
     * the interrupt handler mechanism, we can use a standard boolean here for better compilation.
     */
    private boolean shouldTrigger;

    @CompilationFinal private PointersObject interruptSemaphore;
    private PointersObject timerSemaphore;
    private ScheduledFuture<?> interruptChecks;

    private int count;

    private InterruptHandlerState(final SqueakImageContext image) {
        this.image = image;
        if (image.options.disableInterruptHandler) {
            image.printToStdOut("Interrupt handler disabled...");
        }
    }

    public static InterruptHandlerState create(final SqueakImageContext image) {
        return new InterruptHandlerState(image);
    }

    @TruffleBoundary
    public void start() {
        if (image.options.disableInterruptHandler) {
            return;
        }
        final Object interruptSema = image.getSpecialObject(SPECIAL_OBJECT.THE_INTERRUPT_SEMAPHORE);
        if (interruptSema instanceof PointersObject) {
            setInterruptSemaphore((PointersObject) interruptSema);
        } else {
            assert interruptSema == NilObject.SINGLETON;
        }
        final Object timerSema = image.getSpecialObject(SPECIAL_OBJECT.THE_TIMER_SEMAPHORE);
        if (timerSema instanceof PointersObject) {
            setTimerSemaphore((PointersObject) timerSema);
        } else {
            assert timerSema == NilObject.SINGLETON;
        }
        executor = new ScheduledThreadPoolExecutor(1);
        executor.setRemoveOnCancelPolicy(true);
        interruptChecks = executor.scheduleWithFixedDelay(() -> {
            shouldTrigger = interruptPending() || nextWakeUpTickTrigger() || pendingFinalizationSignals() || hasSemaphoresToSignal();
        }, INTERRUPT_CHECKS_EVERY_N_MILLISECONDS, INTERRUPT_CHECKS_EVERY_N_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    @TruffleBoundary
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    public void setInterruptPending() {
        interruptPending = true;
    }

    public void setNextWakeupTick(final long msTime) {
        LogUtils.INTERRUPTS.finer(() -> {
            if (nextWakeupTick != 0) {
                return (msTime != 0 ? "Changing nextWakeupTick to " + msTime + " from " : "Resetting nextWakeupTick from ") + nextWakeupTick + " after " + count + " checks";
            } else {
                return (msTime != 0 ? "Setting nextWakeupTick to " + msTime : "Resetting nextWakeupTick when it was already 0") + " after " + count + " checks";
            }
        });
        nextWakeupTick = msTime;
        count = 0;
    }

    public long getNextWakeupTick() {
        return nextWakeupTick;
    }

    public boolean disabled() {
        return image.options.disableInterruptHandler;
    }

    public boolean isActive() {
        return isActive;
    }

    public void activate() {
        isActive = true;
    }

    public void deactivate() {
        isActive = false;
    }

    protected boolean interruptPending() {
        return interruptPending;
    }

    protected boolean nextWakeUpTickTrigger() {
        if (nextWakeupTick != 0) {
            final long time = System.currentTimeMillis();
            count++;
            if (time >= nextWakeupTick) {
                LogUtils.INTERRUPTS.finer(() -> "Reached nextWakeupTick: " + nextWakeupTick + " after " + count + " checks");
                return true;
            }
        }
        return false;
    }

    public void setPendingFinalizations(final boolean value) {
        pendingFinalizationSignals = value;
    }

    protected boolean pendingFinalizationSignals() {
        return pendingFinalizationSignals;
    }

    protected boolean hasSemaphoresToSignal() {
        return !semaphoresToSignal.isEmpty();
    }

    protected Integer nextSemaphoreToSignal() {
        return semaphoresToSignal.pollFirst();
    }

    public static int getInterruptChecksEveryNms() {
        return INTERRUPT_CHECKS_EVERY_N_MILLISECONDS;
    }

    @TruffleBoundary
    public void signalSemaphoreWithIndex(final int index) {
        semaphoresToSignal.addLast(index);
    }

    public boolean isActiveAndShouldTrigger() {
        return isActive && shouldTrigger();
    }

    public boolean shouldTrigger() {
        if (shouldTrigger) {
            shouldTrigger = false;
            return true;
        } else {
            return false;
        }
    }

    public PointersObject getInterruptSemaphore() {
        return interruptSemaphore;
    }

    public void setInterruptSemaphore(final PointersObject interruptSemaphore) {
        this.interruptSemaphore = interruptSemaphore;
    }

    public PointersObject getTimerSemaphore() {
        return timerSemaphore;
    }

    public void setTimerSemaphore(final PointersObject timerSemaphore) {
        this.timerSemaphore = timerSemaphore;
    }

    /*
     * TESTING
     */

    public void reset() {
        CompilerAsserts.neverPartOfCompilation("Resetting interrupt handler only supported for testing purposes");
        isActive = true;
        nextWakeupTick = 0;
        count = 0;
        if (interruptChecks != null) {
            interruptChecks.cancel(true);
        }
        shutdown();
        interruptPending = false;
        pendingFinalizationSignals = false;
        clearWeakPointersQueue();
        semaphoresToSignal.clear();
    }

    private void clearWeakPointersQueue() {
        while (image.weakPointersQueue.poll() != null) {
            // Poll until empty.
        }
    }
}
