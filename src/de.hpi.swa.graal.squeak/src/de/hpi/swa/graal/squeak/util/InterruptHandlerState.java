package de.hpi.swa.graal.squeak.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.SqueakOptions;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.model.PointersObject;

public final class InterruptHandlerState {
    private static final int INTERRUPT_CHECKS_EVERY_N_MILLISECONDS = 3;

    public final SqueakImageContext image;
    private final boolean disabled;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    protected final Deque<Integer> semaphoresToSignal = new ArrayDeque<>();

    protected long nextWakeupTick = 0;
    protected boolean interruptPending = false;
    private boolean isActive = false;
    protected boolean pendingFinalizationSignals = false;
    public volatile boolean shouldTrigger = false;

    @CompilationFinal private PointersObject interruptSemaphore;
    private PointersObject timerSemaphore;

    public static InterruptHandlerState create(final SqueakImageContext image) {
        return new InterruptHandlerState(image);
    }

    protected InterruptHandlerState(final SqueakImageContext image) {
        this.image = image;
        disabled = SqueakOptions.getOption(image.env, SqueakOptions.DisableInterruptHandler);
        if (disabled) {
            image.printToStdOut("Interrupt handler disabled...");
        }
    }

    @TruffleBoundary
    public void start() {
        if (disabled) {
            return;
        }
        final Object interruptSema = image.specialObjectsArray.at0Object(SPECIAL_OBJECT.THE_INTERRUPT_SEMAPHORE);
        if (interruptSema instanceof PointersObject) {
            setInterruptSemaphore((PointersObject) interruptSema);
        }
        final Object timerSema = image.specialObjectsArray.at0Object(SPECIAL_OBJECT.THE_TIMER_SEMAPHORE);
        if (timerSema instanceof PointersObject) {
            setTimerSemaphore((PointersObject) timerSema);
        }
        executor.scheduleWithFixedDelay(() -> shouldTrigger = true,
                        INTERRUPT_CHECKS_EVERY_N_MILLISECONDS, INTERRUPT_CHECKS_EVERY_N_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    @TruffleBoundary
    public void shutdown() {
        executor.shutdown();
    }

    public void setInterruptPending() {
        interruptPending = true;
    }

    public void setNextWakeupTick(final long msTime) {
        nextWakeupTick = msTime;
    }

    public long getNextWakeupTick() {
        return nextWakeupTick;
    }

    public boolean disabled() {
        return disabled;
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
        return (nextWakeupTick != 0) && (System.currentTimeMillis() >= nextWakeupTick);
    }

    public void setPendingFinalizations(final boolean value) {
        pendingFinalizationSignals = value;
    }

    protected boolean pendingFinalizationSignals() {
        return pendingFinalizationSignals;
    }

    @TruffleBoundary
    protected boolean hasSemaphoresToSignal() {
        return !semaphoresToSignal.isEmpty();
    }

    @TruffleBoundary
    protected int nextSemaphoreToSignal() {
        return semaphoresToSignal.removeFirst();
    }

    public static int getInterruptChecksEveryNms() {
        return INTERRUPT_CHECKS_EVERY_N_MILLISECONDS;
    }

    @TruffleBoundary
    public void signalSemaphoreWithIndex(final int index) {
        semaphoresToSignal.addLast(index);
    }

    public boolean shouldTrigger() {
        if (CompilerDirectives.inCompiledCode() && !CompilerDirectives.inCompilationRoot()) {
            return false; // do not trigger in inlined code
        }
        if (!isActive) {
            return false;
        }
        return shouldTrigger;
    }

    public PointersObject getInterruptSemaphore() {
        return interruptSemaphore;
    }

    public void setInterruptSemaphore(final PointersObject interruptSemaphore) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
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
        nextWakeupTick = 0;
        interruptPending = false;
        isActive = false;
        pendingFinalizationSignals = false;
    }
}
