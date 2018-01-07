package de.hpi.swa.trufflesqueak.util;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.PointersObject;

public class InterruptHandler {
    private final SqueakImageContext image;
    private int interruptCheckCounter = 0;
    private int interruptCheckCounterFeedbackReset = 1000;
    private int interruptChecksEveryNms = 3;
    private int nextPollTick = 0;
    private int nextWakeupTick = 0;
    private int lastTick = 0;
    private boolean interruptPending = false;
    private int pendingFinalizationSignals = 0;

    public InterruptHandler(SqueakImageContext image) {
        this.image = image;
    }

    public void setInterruptPending() {
        interruptPending = true;
    }

    public void nextWakeupTick(int msTime) {
        nextWakeupTick = msTime;
    }

    public void checkForInterrupts(VirtualFrame frame) { // Check for interrupts at sends and backward jumps
        if (interruptCheckCounter-- > 0) {
            return; // only really check every 100 times or so
        }
        int now = (int) System.currentTimeMillis();
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
            if (interruptCheckCounterFeedbackReset <= 1000) {
                interruptCheckCounterFeedbackReset = 1000;
            } else {
                interruptCheckCounterFeedbackReset -= 12;
            }
        }
        interruptCheckCounter = interruptCheckCounterFeedbackReset;
        lastTick = now; // used to detect wrap around of millisecond clock
        Object semaphoreObject = image.nil;
        if (interruptPending) {
            interruptPending = false; // reset interrupt flag
            semaphoreObject = image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.TheInterruptSemaphore);
        }
        if ((nextWakeupTick != 0) && (now >= nextWakeupTick)) {
            nextWakeupTick = 0; // reset timer interrupt
            semaphoreObject = image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.TheTimerSemaphore);
        }
        if (pendingFinalizationSignals > 0) { // signal any pending finalizations
            pendingFinalizationSignals = 0;
            semaphoreObject = image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.TheFinalizationSemaphore);
        }
        if (!semaphoreObject.equals(image.nil)) {
            image.synchronousSignal(frame, (PointersObject) semaphoreObject);
        }
    }
}
