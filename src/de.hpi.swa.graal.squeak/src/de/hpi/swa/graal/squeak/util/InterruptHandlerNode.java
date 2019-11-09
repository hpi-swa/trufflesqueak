/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.util;

import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ControlPrimitives;
import de.hpi.swa.graal.squeak.nodes.process.SignalSemaphoreNode;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

public abstract class InterruptHandlerNode extends Node {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, ControlPrimitives.class);
    protected final Object[] specialObjects;
    private final InterruptHandlerState istate;

    @Child private SignalSemaphoreNode signalSemaporeNode;

    protected InterruptHandlerNode(final CompiledCodeObject code) {
        specialObjects = code.image.specialObjectsArray.getObjectStorage();
        istate = code.image.interrupt;
        signalSemaporeNode = SignalSemaphoreNode.create(code);
    }

    public static InterruptHandlerNode create(final CompiledCodeObject code) {
        return InterruptHandlerNodeGen.create(code);
    }

    public abstract void executeTrigger(VirtualFrame frame);

    protected final boolean externalObjectsIsEmpty() {
        return ((ArrayObject) specialObjects[SPECIAL_OBJECT.EXTERNAL_OBJECTS_ARRAY]).isEmptyType();
    }

    @Specialization(guards = {"!externalObjectsIsEmpty()"})
    protected final void doFullCheck(final VirtualFrame frame) {
        performChecks(frame);
        checkSemaphoresToSignal(frame);
    }

    @Specialization(guards = {"externalObjectsIsEmpty()"})
    protected final void doCheck(final VirtualFrame frame) {
        performChecks(frame);
    }

    private void performChecks(final VirtualFrame frame) {
        if (istate.interruptPending()) {
            istate.interruptPending = false; // reset interrupt flag
            LOG.fine(() -> "Signalling interrupt semaphore @" + istate.getInterruptSemaphore().hashCode() + " in interrupt handler");
            signalSemaporeNode.executeSignal(frame, istate.getInterruptSemaphore());
        }
        if (istate.nextWakeUpTickTrigger()) {
            istate.nextWakeupTick = 0; // reset timer interrupt
            LOG.fine(() -> "Signalling timer semaphore @" + istate.getTimerSemaphore().hashCode() + " in interrupt handler");
            signalSemaporeNode.executeSignal(frame, istate.getTimerSemaphore());
        }
        if (istate.pendingFinalizationSignals()) { // signal any pending finalizations
            istate.setPendingFinalizations(false);
            LOG.fine(() -> "Signalling finalizations semaphore @" + specialObjects[SPECIAL_OBJECT.THE_FINALIZATION_SEMAPHORE].hashCode() + " in interrupt handler");
            signalSemaporeNode.executeSignal(frame, specialObjects[SPECIAL_OBJECT.THE_FINALIZATION_SEMAPHORE]);
        }
    }

    private void checkSemaphoresToSignal(final VirtualFrame frame) {
        final Object[] semaphores = ((ArrayObject) specialObjects[SPECIAL_OBJECT.EXTERNAL_OBJECTS_ARRAY]).getObjectStorage();
        Integer semaIndex;
        while ((semaIndex = istate.nextSemaphoreToSignal()) != null) {
            final Object semaphore = semaphores[semaIndex - 1];
            LOG.fine(() -> "Signalling external semaphore @" + semaphore.hashCode() + " in interrupt handler");
            signalSemaporeNode.executeSignal(frame, semaphore);
        }
    }
}
