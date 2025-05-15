/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interrupts;

import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.trufflesqueak.nodes.process.SignalSemaphoreNode;

public final class CheckForInterruptsFullNode extends Node {
    @Child private SignalSemaphoreNode signalSemaporeNode;

    private final Object[] specialObjects;
    private final CheckForInterruptsState istate;

    private CheckForInterruptsFullNode(final SqueakImageContext image) {
        specialObjects = image.specialObjectsArray.getObjectStorage();
        istate = image.interrupt;
        signalSemaporeNode = SignalSemaphoreNode.create();
    }

    @NeverDefault
    public static CheckForInterruptsFullNode create() {
        return new CheckForInterruptsFullNode(SqueakImageContext.getSlow());
    }

    public void execute(final VirtualFrame frame) {
        if (istate.shouldSkip()) {
            return;
        }
        ProcessSwitch processSwitch = null;
        if (istate.tryInterruptPending()) {
            final ProcessSwitch ps = signalSemaporeNode.executeSignal(frame, this, specialObjects[SPECIAL_OBJECT.THE_INTERRUPT_SEMAPHORE]);
            if (ps != null) {
                processSwitch = ps;
            }
        }
        if (istate.tryWakeUpTickTrigger()) {
            final ProcessSwitch ps = signalSemaporeNode.executeSignal(frame, this, specialObjects[SPECIAL_OBJECT.THE_TIMER_SEMAPHORE]);
            if (ps != null) {
                processSwitch = ps;
            }
        }
        if (istate.tryPendingFinalizations()) {
            final ProcessSwitch ps = signalSemaporeNode.executeSignal(frame, this, specialObjects[SPECIAL_OBJECT.THE_FINALIZATION_SEMAPHORE]);
            if (ps != null) {
                processSwitch = ps;
            }
        }
        if (istate.trySemaphoresToSignal()) {
            final ArrayObject externalObjects = (ArrayObject) specialObjects[SPECIAL_OBJECT.EXTERNAL_OBJECTS_ARRAY];
            if (!externalObjects.isEmptyType()) { // signal external semaphores
                final Object[] semaphores = externalObjects.getObjectStorage();
                Integer semaIndex;
                while ((semaIndex = istate.nextSemaphoreToSignal()) != null) {
                    final ProcessSwitch ps = signalSemaporeNode.executeSignal(frame, this, semaphores[semaIndex - 1]);
                    if (ps != null) {
                        processSwitch = ps;
                    }
                }
            }
        }
        /*
         * OpenSmalltalk VM signals all semaphores and switches to the highest priority process.
         * If we do not do this, small Delays in a loop in the image will prevent the code after the
         * wake-up-tick handler from getting executed (finalizations, for example).
         */
        if (processSwitch != null) {
            throw processSwitch;
        }
    }
}