/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.util;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.nodes.process.SignalSemaphoreNode;

public abstract class InterruptHandlerNode extends Node {
    protected final SqueakImageContext image;

    @Child private SignalSemaphoreNode signalSemaporeNode;

    protected InterruptHandlerNode(final CompiledCodeObject code) {
        image = code.image;
        signalSemaporeNode = SignalSemaphoreNode.create(code);
    }

    public static InterruptHandlerNode create(final CompiledCodeObject code) {
        return InterruptHandlerNodeGen.create(code);
    }

    public abstract void executeTrigger(VirtualFrame frame);

    @Specialization(guards = {"!image.externalObjectsArray.isEmptyType()"})
    protected final void doFullCheck(final VirtualFrame frame) {
        performChecks(frame);
        checkSemaphoresToSignal(frame);
    }

    @Specialization(guards = {"image.externalObjectsArray.isEmptyType()"})
    protected final void doCheck(final VirtualFrame frame) {
        performChecks(frame);
    }

    private void performChecks(final VirtualFrame frame) {
        final InterruptHandlerState istate = image.interrupt;
        if (istate.interruptPending()) {
            istate.interruptPending = false; // reset interrupt flag
            signalSemaporeNode.executeSignal(frame, istate.getInterruptSemaphore());
        }
        if (istate.nextWakeUpTickTrigger()) {
            istate.nextWakeupTick = 0; // reset timer interrupt
            signalSemaporeNode.executeSignal(frame, istate.getTimerSemaphore());
        }
        if (istate.pendingFinalizationSignals()) { // signal any pending finalizations
            istate.setPendingFinalizations(false);
            signalSemaporeIfNotNil(frame, SPECIAL_OBJECT.THE_FINALIZATION_SEMAPHORE);
        }
    }

    private void checkSemaphoresToSignal(final VirtualFrame frame) {
        final Object[] semaphores = image.externalObjectsArray.getObjectStorage();
        Integer semaIndex;
        while ((semaIndex = image.interrupt.nextSemaphoreToSignal()) != null) {
            final Object semaphore = semaphores[semaIndex - 1];
            signalSemaporeIfNotNil(frame, semaphore);
        }
    }

    private void signalSemaporeIfNotNil(final VirtualFrame frame, final int semaphoreIndex) {
        signalSemaporeIfNotNil(frame, image.getSpecialObject(semaphoreIndex));
    }

    private void signalSemaporeIfNotNil(final VirtualFrame frame, final Object semaphore) {
        signalSemaporeNode.executeSignal(frame, semaphore);
    }
}
