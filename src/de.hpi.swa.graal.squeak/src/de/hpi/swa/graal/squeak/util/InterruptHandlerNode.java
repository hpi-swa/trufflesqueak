package de.hpi.swa.graal.squeak.util;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.process.SignalSemaphoreNode;

public final class InterruptHandlerNode extends Node {
    @CompilationFinal private static final int INTERRUPT_CHECK_COUNTER_SIZE = 1000;
    @CompilationFinal private static final int INTERRUPT_CHECKS_EVERY_NMS = 3;
    @CompilationFinal private final SqueakImageContext image;
    @CompilationFinal private final boolean disabled;
    private final ConditionProfile countingProfile = ConditionProfile.createCountingProfile();
    private int interruptCheckCounter = 0;
    private int interruptCheckCounterFeedbackReset = INTERRUPT_CHECK_COUNTER_SIZE;
    private long nextPollTick = 0;
    private long nextWakeupTick = 0;
    private long lastTick = 0;
    private boolean interruptPending = false;
    private boolean disabledTemporarily = false;
    private boolean pendingFinalizationSignals = false;
    @Child private SignalSemaphoreNode signalSemaporeNode;

    public static InterruptHandlerNode create(final SqueakImageContext image, final SqueakConfig config) {
        return new InterruptHandlerNode(image, config);
    }

    protected InterruptHandlerNode(final SqueakImageContext image, final SqueakConfig config) {
        this.image = image;
        disabled = config.disableInterruptHandler();
        if (disabled) {
            image.getOutput().println("Interrupt handler disabled...");
        }
        signalSemaporeNode = SignalSemaphoreNode.create(image);
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

    public void reset() { // for testing purposes
        CompilerDirectives.transferToInterpreterAndInvalidate();
        interruptCheckCounter = 0;
        interruptCheckCounterFeedbackReset = INTERRUPT_CHECK_COUNTER_SIZE;
        nextPollTick = 0;
        nextWakeupTick = 0;
        lastTick = 0;
        interruptPending = false;
        disabledTemporarily = false;
        pendingFinalizationSignals = false;
    }

    /*
     * Check for interrupts on sends and backward jumps. TODO: call on backward jumps
     */
    public void sendOrBackwardJumpTrigger(final VirtualFrame frame) {
        if (disabled || disabledTemporarily) {
            return;
        }
        // Decrement counter in separate if-statement (should not happen at all when disabled).
        if (countingProfile.profile(interruptCheckCounter-- > 0)) {
            return; // only really check every 100 times or so
        }
        executeCheck(frame);
    }

    public void executeCheck(final VirtualFrame frame) {
        final long now = System.currentTimeMillis();
        if (now < lastTick) { // millisecond clock wrapped"
            nextPollTick = now + (nextPollTick - lastTick);
            if (nextWakeupTick != 0) {
                nextWakeupTick = now + (nextWakeupTick - lastTick);
            }
        }
        // Feedback logic attempts to keep interrupt response around 3ms...
        if ((now - lastTick) < getInterruptChecksEveryNms()) {
            interruptCheckCounterFeedbackReset += 10;
        } else {
            if (interruptCheckCounterFeedbackReset <= INTERRUPT_CHECK_COUNTER_SIZE) {
                interruptCheckCounterFeedbackReset = INTERRUPT_CHECK_COUNTER_SIZE;
            } else {
                interruptCheckCounterFeedbackReset -= 12;
            }
        }
        interruptCheckCounter = interruptCheckCounterFeedbackReset;
        lastTick = now; // used to detect wrap around of millisecond clock
        if (interruptPending) {
            interruptPending = false; // reset interrupt flag
            signalSemaporeIfNotNil(frame, SPECIAL_OBJECT_INDEX.TheInterruptSemaphore);
        }
        if ((nextWakeupTick != 0) && (now >= nextWakeupTick)) {
            nextWakeupTick = 0; // reset timer interrupt
            signalSemaporeIfNotNil(frame, SPECIAL_OBJECT_INDEX.TheTimerSemaphore);
        }
        if (pendingFinalizationSignals) { // signal any pending finalizations
            pendingFinalizationSignals = false;
            signalSemaporeIfNotNil(frame, SPECIAL_OBJECT_INDEX.TheFinalizationSemaphore);
        }
    }

    private void signalSemaporeIfNotNil(final VirtualFrame frame, final int semaphoreIndex) {
        final Object semaphoreObject = image.specialObjectsArray.at0(semaphoreIndex);
        if (semaphoreObject != image.nil) {
            signalSemaporeNode.executeSignal(frame, (PointersObject) semaphoreObject);
        }
    }

    public static int getInterruptChecksEveryNms() {
        return INTERRUPT_CHECKS_EVERY_NMS;
    }
}
