/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.util;

import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.nodes.process.SignalSemaphoreNode;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

public final class InterruptHandlerNode extends Node {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, InterruptHandlerNode.class);
    private static final boolean isLoggingEnabled = LOG.isLoggable(Level.FINE);

    @Child private SignalSemaphoreNode signalSemaporeNode;
    private final Object[] specialObjects;
    private final InterruptHandlerState istate;

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
            /* Exclude user interrupt case from compilation. */
            CompilerDirectives.transferToInterpreter();
            istate.interruptPending = false; // reset interrupt flag
            if (isLoggingEnabled) {
                LOG.fine(() -> "Signalling interrupt semaphore @" + Integer.toHexString(istate.getInterruptSemaphore().hashCode()) + " in interrupt handler");
            }
            signalSemaporeNode.executeSignal(frame, istate.getInterruptSemaphore());
        }
        if (istate.nextWakeUpTickTrigger()) {
            nextWakeupTickProfile.enter();
            istate.nextWakeupTick = 0; // reset timer interrupt
            if (isLoggingEnabled) {
                LOG.fine(() -> "Signalling timer semaphore @" + Integer.toHexString(istate.getTimerSemaphore().hashCode()) + " in interrupt handler");
            }
            signalSemaporeNode.executeSignal(frame, istate.getTimerSemaphore());
        }
        if (istate.pendingFinalizationSignals()) { // signal any pending finalizations
            pendingFinalizationSignalsProfile.enter();
            istate.setPendingFinalizations(false);
            if (isLoggingEnabled) {
                LOG.fine(() -> "Signalling finalizations semaphore @" + Integer.toHexString(specialObjects[SPECIAL_OBJECT.THE_FINALIZATION_SEMAPHORE].hashCode()) + " in interrupt handler");
            }
            signalSemaporeNode.executeSignal(frame, specialObjects[SPECIAL_OBJECT.THE_FINALIZATION_SEMAPHORE]);
        }
        if (istate.hasSemaphoresToSignal()) {
            hasSemaphoresToSignalProfile.enter();
            final ArrayObject externalObjects = (ArrayObject) specialObjects[SPECIAL_OBJECT.EXTERNAL_OBJECTS_ARRAY];
            if (!externalObjects.isEmptyType()) { // signal external semaphores
                final Object[] semaphores = externalObjects.getObjectStorage();
                Integer semaIndex;
                while ((semaIndex = istate.nextSemaphoreToSignal()) != null) {
                    final Object semaphore = semaphores[semaIndex - 1];
                    if (isLoggingEnabled) {
                        LOG.fine(() -> "Signalling external semaphore @" + Integer.toHexString(semaphore.hashCode()) + " in interrupt handler");
                    }
                    signalSemaporeNode.executeSignal(frame, semaphore);
                }
            }
        }
    }
}
