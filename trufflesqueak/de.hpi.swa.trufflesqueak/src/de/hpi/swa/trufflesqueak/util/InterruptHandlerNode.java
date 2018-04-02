package de.hpi.swa.trufflesqueak.util;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.SqueakConfig;
import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.process.SignalSemaphoreNode;

public class InterruptHandlerNode extends Node {
    @CompilationFinal private final SqueakImageContext image;
    @CompilationFinal private static final int interruptCheckCounterSize = 1000;
    @CompilationFinal public static final int interruptChecksEveryNms = 3;
    private int interruptCheckCounter = 0;
    private static int interruptCheckCounterFeedbackReset = interruptCheckCounterSize;
    private long nextPollTick = 0;
    private long nextWakeupTick = 0;
    private long lastTick = 0;
    private boolean interruptPending = false;
    private boolean disabled = false;
    private int pendingFinalizationSignals = 0;
    @Child private SignalSemaphoreNode signalSemaporeNode;

    public static InterruptHandlerNode create(SqueakImageContext image, SqueakConfig config) {
        if (config.disableInterruptHandler()) {
            return new DummyInterruptHandlerNode(image);
        } else {
            return new InterruptHandlerNode(image);
        }
    }

    protected InterruptHandlerNode(SqueakImageContext image) {
        this.image = image;
        signalSemaporeNode = SignalSemaphoreNode.create(image);
    }

    public void setInterruptPending() {
        interruptPending = true;
    }

    public void nextWakeupTick(long msTime) {
        nextWakeupTick = msTime;
    }

    public void setDisabled(boolean value) {
        disabled = value;
    }

    /*
     * Check for interrupts on sends and backward jumps.
     */
    public void sendOrBackwardJumpTrigger(VirtualFrame frame) { // TODO: call on backward jumps
        if (disabled || interruptCheckCounter-- > 0) {
            return; // only really check every 100 times or so
        }
        executeCheck(frame);
    }

    public void executeCheck(VirtualFrame frame) { // Check for interrupts at sends and backward jumps
        long now = System.currentTimeMillis();
        if (now < lastTick) { // millisecond clock wrapped"
            nextPollTick = now + (nextPollTick - lastTick);
            if (nextWakeupTick != 0) {
                nextWakeupTick = now + (nextWakeupTick - lastTick);
            }
        }
        // Feedback logic attempts to keep interrupt response around 3ms...
        if ((now - lastTick) < interruptChecksEveryNms) {
            interruptCheckCounterFeedbackReset += 10;
        } else {
            if (interruptCheckCounterFeedbackReset <= interruptCheckCounterSize) {
                interruptCheckCounterFeedbackReset = interruptCheckCounterSize;
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
        if (pendingFinalizationSignals > 0) { // signal any pending finalizations
            pendingFinalizationSignals = 0;
            signalSemaporeIfNotNil(frame, SPECIAL_OBJECT_INDEX.TheFinalizationSemaphore);
        }
    }

    private void signalSemaporeIfNotNil(VirtualFrame frame, int semaphoreIndex) {
        Object semaphoreObject = image.specialObjectsArray.at0(semaphoreIndex);
        if (semaphoreObject != image.nil) {
            signalSemaporeNode.executeSignal(frame, (PointersObject) semaphoreObject);
        }
    }

    protected static final class DummyInterruptHandlerNode extends InterruptHandlerNode {
        protected DummyInterruptHandlerNode(SqueakImageContext image) {
            super(image);
            image.getOutput().println("Interrupt handler disabled...");
        }

        @Override
        public void sendOrBackwardJumpTrigger(VirtualFrame frame) {
        }

        @Override
        public void executeCheck(VirtualFrame frame) {
        }
    }
}
