/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interrupts;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DenyReplace;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.process.SignalSemaphoreNode;
import de.hpi.swa.trufflesqueak.util.LogUtils;

@DenyReplace
public final class CheckForInterruptsInLoopNode extends AbstractNode {
    private static final CheckForInterruptsInLoopNode SINGLETON = new CheckForInterruptsInLoopNode();

    private CheckForInterruptsInLoopNode() {
    }

    @NeverDefault
    public static CheckForInterruptsInLoopNode create() {
        return SINGLETON;
    }

    public void execute(final VirtualFrame frame) {
        final SqueakImageContext image = getContext();
        final CheckForInterruptsState istate = image.interrupt;
        if (!istate.shouldTrigger()) {
            return;
        }
        /* Exclude interrupts case from compilation. */
        CompilerDirectives.transferToInterpreter();
        final Object[] specialObjects = image.specialObjectsArray.getObjectStorage();
        if (istate.interruptPending()) {
            LogUtils.INTERRUPTS.fine("User interrupt");
            istate.interruptPending = false; // reset interrupt flag
            SignalSemaphoreNode.executeUncached(frame, image, specialObjects[SPECIAL_OBJECT.THE_INTERRUPT_SEMAPHORE]);
        }
        if (istate.nextWakeUpTickTrigger()) {
            LogUtils.INTERRUPTS.fine("Timer interrupt");
            istate.nextWakeupTick = 0; // reset timer interrupt
            SignalSemaphoreNode.executeUncached(frame, image, specialObjects[SPECIAL_OBJECT.THE_TIMER_SEMAPHORE]);
        }
        if (istate.pendingFinalizationSignals()) { // signal any pending finalizations
            LogUtils.INTERRUPTS.fine("Finalization interrupt");
            istate.setPendingFinalizations(false);
            SignalSemaphoreNode.executeUncached(frame, image, specialObjects[SPECIAL_OBJECT.THE_FINALIZATION_SEMAPHORE]);
        }
        if (istate.hasSemaphoresToSignal()) {
            LogUtils.INTERRUPTS.fine("Semaphore interrupt");
            final ArrayObject externalObjects = (ArrayObject) specialObjects[SPECIAL_OBJECT.EXTERNAL_OBJECTS_ARRAY];
            if (!externalObjects.isEmptyType()) { // signal external semaphores
                final Object[] semaphores = externalObjects.getObjectStorage();
                Integer semaIndex;
                while ((semaIndex = istate.nextSemaphoreToSignal()) != null) {
                    SignalSemaphoreNode.executeUncached(frame, image, semaphores[semaIndex - 1]);
                }
            }
        }
    }

    @Override
    public boolean isAdoptable() {
        return false;
    }

    @Override
    public Node copy() {
        return SINGLETON;
    }

    @Override
    public Node deepCopy() {
        return copy();
    }
}
