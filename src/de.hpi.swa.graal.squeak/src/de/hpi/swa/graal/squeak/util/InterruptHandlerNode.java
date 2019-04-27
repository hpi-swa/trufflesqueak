package de.hpi.swa.graal.squeak.util;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.process.SignalSemaphoreNode;

@ImportStatic(SqueakImageContext.class)
public abstract class InterruptHandlerNode extends Node {
    protected final SqueakImageContext image;
    private final InterruptHandlerState istate;

    @Child private SignalSemaphoreNode signalSemaporeNode;

    protected InterruptHandlerNode(final CompiledCodeObject code) {
        image = code.image;
        istate = image.interrupt;
        signalSemaporeNode = SignalSemaphoreNode.create(code);
    }

    public static InterruptHandlerNode create(final CompiledCodeObject code) {
        return InterruptHandlerNodeGen.create(code);
    }

    public abstract void executeTrigger(VirtualFrame frame);

    @Specialization(guards = {"isAOT()", "image.hasDisplay()", "!image.externalObjectsArray.isEmptyType()"})
    protected final void doFullCheckAOT(final VirtualFrame frame) {
        image.getDisplay().pollEvents();
        performChecks(frame);
        checkSemaphoresToSignal(frame);
    }

    @Specialization(guards = {"isAOT()", "image.hasDisplay()", "image.externalObjectsArray.isEmptyType()"})
    protected final void doCheckAOT(final VirtualFrame frame) {
        image.getDisplay().pollEvents();
        performChecks(frame);
    }

    @Specialization(guards = {"!isAOT() || !image.hasDisplay()", "!image.externalObjectsArray.isEmptyType()"})
    protected final void doFullCheck(final VirtualFrame frame) {
        performChecks(frame);
        checkSemaphoresToSignal(frame);
    }

    @Specialization(guards = {"!isAOT() || !image.hasDisplay()", "image.externalObjectsArray.isEmptyType()"})
    protected final void doCheck(final VirtualFrame frame) {
        performChecks(frame);
    }

    private void performChecks(final VirtualFrame frame) {
        if (istate.interruptPending()) {
            istate.interruptPending = false; // reset interrupt flag
            final PointersObject interruptSemaphore = istate.getInterruptSemaphore();
            signalSemaporeNode.executeSignal(frame, interruptSemaphore);
        }
        if (istate.nextWakeUpTickTrigger()) {
            istate.nextWakeupTick = 0; // reset timer interrupt
            final PointersObject timerSemaphore = istate.getTimerSemaphore();
            signalSemaporeNode.executeSignal(frame, timerSemaphore);
        }
        if (istate.pendingFinalizationSignals()) { // signal any pending finalizations
            istate.setPendingFinalizations(false);
            signalSemaporeIfNotNil(frame, SPECIAL_OBJECT.THE_FINALIZATION_SEMAPHORE);
        }
    }

    private void checkSemaphoresToSignal(final VirtualFrame frame) {
        if (istate.hasSemaphoresToSignal()) {
            assert !image.externalObjectsArray.isEmptyType();
            final Object[] semaphores = image.externalObjectsArray.getObjectStorage();
            while (istate.hasSemaphoresToSignal()) {
                final int semaIndex = istate.nextSemaphoreToSignal();
                final Object semaphore = semaphores[semaIndex - 1];
                signalSemaporeIfNotNil(frame, semaphore);
            }
        }
    }

    private void signalSemaporeIfNotNil(final VirtualFrame frame, final int semaphoreIndex) {
        signalSemaporeIfNotNil(frame, image.specialObjectsArray.at0Object(semaphoreIndex));
    }

    private void signalSemaporeIfNotNil(final VirtualFrame frame, final Object semaphore) {
        signalSemaporeNode.executeSignal(frame, semaphore);
    }
}
