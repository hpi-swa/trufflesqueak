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

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.model.PointersObject;

public final class InterruptHandlerState {
    private static final int INTERRUPT_CHECKS_EVERY_N_MILLISECONDS = 3;

    public final SqueakImageContext image;
    public final boolean disabled;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    protected final Deque<Integer> semaphoresToSignal = new ArrayDeque<>();

    protected long nextWakeupTick = 0;
    protected boolean interruptPending = false;
    public boolean disabledTemporarily = false;
    protected boolean pendingFinalizationSignals = false;
    public volatile boolean shouldTrigger = false;

    @CompilationFinal private PointersObject interruptSemaphore;
    private PointersObject timerSemaphore;

    public static InterruptHandlerState create(final SqueakImageContext image) {
        return new InterruptHandlerState(image);
    }

    protected InterruptHandlerState(final SqueakImageContext image) {
        this.image = image;
        disabled = image.config.disableInterruptHandler();
        if (disabled) {
            image.printToStdOut("Interrupt handler disabled...");
        }
    }

    @TruffleBoundary
    public void start() {
        if (disabled) {
            return;
        }
        final Object interruptSema = image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.TheInterruptSemaphore);
        if (interruptSema instanceof PointersObject) {
            setInterruptSemaphore((PointersObject) interruptSema);
        }
        final Object timerSema = image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.TheTimerSemaphore);
        if (timerSema instanceof PointersObject) {
            setTimerSemaphore((PointersObject) timerSema);
        }
        executor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                shouldTrigger = true;
            }
        }, INTERRUPT_CHECKS_EVERY_N_MILLISECONDS, INTERRUPT_CHECKS_EVERY_N_MILLISECONDS, TimeUnit.MILLISECONDS);
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
        return disabledTemporarily;
    }

    public void disable() {
        disabledTemporarily = true;
    }

    public void enable() {
        disabledTemporarily = false;
    }

    public void setPendingFinalizations() {
        pendingFinalizationSignals = true;
    }

    @TruffleBoundary
    private boolean hasSemaphoresToSignal() {
        return semaphoresToSignal.isEmpty();
    }

    @TruffleBoundary
    private int nextSemaphoreToSignal() {
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
        if (disabledTemporarily) {
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
        disabledTemporarily = false;
        pendingFinalizationSignals = false;
    }
}
