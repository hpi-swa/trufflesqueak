package de.hpi.swa.graal.squeak.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.process.SignalSemaphoreNode;

public final class InterruptHandlerNode extends Node {
    @CompilationFinal private static final int INTERRUPT_CHECKS_EVERY_N_MILLISECONDS = 3;
    @CompilationFinal private final SqueakImageContext image;
    @CompilationFinal private final boolean disabled;
    @CompilationFinal private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    @CompilationFinal private final ConditionProfile countingProfile = ConditionProfile.createCountingProfile();
    @Child private SignalSemaphoreNode signalSemaporeNode;
    private long nextWakeupTick = 0;
    private boolean interruptPending = false;
    private boolean disabledTemporarily = false;
    private boolean pendingFinalizationSignals = false;
    private volatile boolean shouldTrigger = false;

    public static InterruptHandlerNode create(final SqueakImageContext image, final SqueakConfig config) {
        return new InterruptHandlerNode(image, config);
    }

    protected InterruptHandlerNode(final SqueakImageContext image, final SqueakConfig config) {
        this.image = image;

        disabled = config.disableInterruptHandler();
        if (disabled) {
            image.getOutput().println("Interrupt handler disabled...");
        }
    }

    public void initializeSignalSemaphoreNode(final CompiledCodeObject method) {
        signalSemaporeNode = SignalSemaphoreNode.create(method);
    }

    public void start() {
        if (disabled) {
            return;
        }
        executor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                shouldTrigger = true;
            }
        }, INTERRUPT_CHECKS_EVERY_N_MILLISECONDS, INTERRUPT_CHECKS_EVERY_N_MILLISECONDS, TimeUnit.MILLISECONDS);
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

    public void disable() {
        disabledTemporarily = true;
    }

    public void enable() {
        disabledTemporarily = false;
    }

    public void setPendingFinalizations() {
        pendingFinalizationSignals = true;
    }

    public void sendOrBackwardJumpTrigger(final VirtualFrame frame) {
        if (disabled) {
            return; //
        }
        if (CompilerDirectives.inCompiledCode() && !CompilerDirectives.inCompilationRoot()) {
            return; // do not trigger in inlined code
        }
        if (disabledTemporarily) {
            return;
        }
        if (countingProfile.profile(!shouldTrigger)) {
            return;
        }
        executeCheck(frame.materialize());
    }

    @TruffleBoundary
    public void executeCheck(final MaterializedFrame frame) {
        shouldTrigger = false;
        if (interruptPending) {
            interruptPending = false; // reset interrupt flag
            signalSemaporeIfNotNil(frame, SPECIAL_OBJECT_INDEX.TheInterruptSemaphore);
        }
        if ((nextWakeupTick != 0) && (System.currentTimeMillis() >= nextWakeupTick)) {
            nextWakeupTick = 0; // reset timer interrupt
            signalSemaporeIfNotNil(frame, SPECIAL_OBJECT_INDEX.TheTimerSemaphore);
        }
        if (pendingFinalizationSignals) { // signal any pending finalizations
            pendingFinalizationSignals = false;
            signalSemaporeIfNotNil(frame, SPECIAL_OBJECT_INDEX.TheFinalizationSemaphore);
        }
    }

    private void signalSemaporeIfNotNil(final MaterializedFrame frame, final int semaphoreIndex) {
        final Object semaphoreObject = image.specialObjectsArray.at0(semaphoreIndex);
        if (semaphoreObject != image.nil) {
            signalSemaporeNode.executeSignal(frame, (PointersObject) semaphoreObject);
        }
    }

    public static int getInterruptChecksEveryNms() {
        return INTERRUPT_CHECKS_EVERY_N_MILLISECONDS;
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
