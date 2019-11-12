/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.util;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.nodes.process.SignalSemaphoreNode;

public final class InterruptHandlerNode extends Node {
    @Child private SignalSemaphoreNode signalSemaporeNode;

    private final Object[] specialObjects;
    private final InterruptHandlerState istate;

    private final BranchProfile interruptPendingProfile = BranchProfile.create();
    private final BranchProfile nextWakeupTickProfile = BranchProfile.create();
    private final BranchProfile pendingFinalizationSignalsProfile = BranchProfile.create();
    private final BranchProfile hasSemaphoresToSignalProfile = BranchProfile.create();

    protected InterruptHandlerNode(final CompiledCodeObject code) {
        specialObjects = code.image.specialObjectsArray.getObjectStorage();
        istate = code.image.interrupt;
        signalSemaporeNode = SignalSemaphoreNode.create(code);
    }

    public static InterruptHandlerNode create(final CompiledCodeObject code) {
        return new InterruptHandlerNode(code);
    }

    public void executeTrigger(final VirtualFrame frame) {
        if (istate.interruptPending()) {
            interruptPendingProfile.enter();
            istate.interruptPending = false; // reset interrupt flag
            signalSemaporeNode.executeSignal(frame, istate.getInterruptSemaphore());
        }
        if (istate.nextWakeUpTickTrigger()) {
            nextWakeupTickProfile.enter();
            istate.nextWakeupTick = 0; // reset timer interrupt
            signalSemaporeNode.executeSignal(frame, istate.getTimerSemaphore());
        }
        if (istate.pendingFinalizationSignals()) { // signal any pending finalizations
            pendingFinalizationSignalsProfile.enter();
            istate.setPendingFinalizations(false);
            signalSemaporeNode.executeSignal(frame, specialObjects[SPECIAL_OBJECT.THE_FINALIZATION_SEMAPHORE]);
        }
        if (istate.hasSemaphoresToSignal()) {
            hasSemaphoresToSignalProfile.enter();
            final ArrayObject externalObjects = (ArrayObject) specialObjects[SPECIAL_OBJECT.EXTERNAL_OBJECTS_ARRAY];
            if (!externalObjects.isEmptyType()) { // signal external semaphores
                final Object[] semaphores = externalObjects.getObjectStorage();
                Integer semaIndex;
                while ((semaIndex = istate.nextSemaphoreToSignal()) != null) {
                    signalSemaporeNode.executeSignal(frame, semaphores[semaIndex - 1]);
                }
            }
        }
    }
}
