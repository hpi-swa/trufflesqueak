package de.hpi.swa.graal.squeak.util;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.process.SignalSemaphoreNode;

@ImportStatic(TruffleOptions.class)
public abstract class InterruptHandlerNode extends Node {
    protected final SqueakImageContext image;
    protected final InterruptHandlerState istate;

    @Child private SignalSemaphoreNode signalSemaporeNode;

    public static InterruptHandlerNode create(final CompiledCodeObject code) {
        return InterruptHandlerNodeGen.create(code);
    }

    protected InterruptHandlerNode(final CompiledCodeObject code) {
        image = code.image;
        istate = image.interrupt;
        signalSemaporeNode = SignalSemaphoreNode.create(code);
    }

    public abstract void executeTrigger(VirtualFrame frame);

    @Specialization(guards = {"AOT", "image.hasDisplay()"})
    protected final void doFullCheckAOT(final VirtualFrame frame) {
        image.getDisplay().forceUpdate();
        performCheck(frame);
    }

    @Specialization(guards = {"!AOT || !image.hasDisplay()"})
    protected final void doFullCheck(final VirtualFrame frame) {
        performCheck(frame);
    }

    private void performCheck(final VirtualFrame frame) {
        istate.shouldTrigger = false;
        if (interruptPending()) {
            istate.interruptPending = false; // reset interrupt flag
            final PointersObject interruptSemaphore = istate.getInterruptSemaphore();
            if (interruptSemaphore != null) {
                signalSemaporeNode.executeSignal(frame, interruptSemaphore);
            }
        }
        if (nextWakeUpTickTrigger()) {
            istate.nextWakeupTick = 0; // reset timer interrupt
            final PointersObject timerSemaphore = istate.getTimerSemaphore();
            if (timerSemaphore != null) {
                signalSemaporeNode.executeSignal(frame, timerSemaphore);
            }
        }
        if (pendingFinalizationSignals()) { // signal any pending finalizations
            istate.pendingFinalizationSignals = false;
            signalSemaporeIfNotNil(frame, SPECIAL_OBJECT_INDEX.TheFinalizationSemaphore);
        }
        if (hasSemaphoresToSignal()) {
            final Object[] semaphores = image.externalObjectsArray.getPointers();
            while (hasSemaphoresToSignal()) {
                final int semaIndex = nextSemaphoreToSignal();
                final Object semaphore = semaphores[semaIndex - 1];
                signalSemaporeIfNotNil(frame, semaphore);
            }
        }
    }

    protected final boolean interruptPending() {
        return istate.interruptPending;
    }

    protected final boolean nextWakeUpTickTrigger() {
        return (istate.nextWakeupTick != 0) && (System.currentTimeMillis() >= istate.nextWakeupTick);
    }

    protected final boolean pendingFinalizationSignals() {
        return istate.pendingFinalizationSignals;
    }

    @TruffleBoundary
    protected final boolean hasSemaphoresToSignal() {
        return !istate.semaphoresToSignal.isEmpty();
    }

    @TruffleBoundary
    private int nextSemaphoreToSignal() {
        return istate.semaphoresToSignal.removeFirst();
    }

    private void signalSemaporeIfNotNil(final VirtualFrame frame, final int semaphoreIndex) {
        signalSemaporeIfNotNil(frame, image.specialObjectsArray.at0(semaphoreIndex));
    }

    private void signalSemaporeIfNotNil(final VirtualFrame frame, final Object semaphore) {
        signalSemaporeNode.executeSignal(frame, semaphore);
    }
}
