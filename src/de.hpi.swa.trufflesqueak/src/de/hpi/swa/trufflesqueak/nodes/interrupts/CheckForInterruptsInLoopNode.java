/*
 * Copyright (c) 2025-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interrupts;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DenyReplace;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.process.SignalSemaphoreNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@DenyReplace
public final class CheckForInterruptsInLoopNode extends AbstractNode {
    private static final CheckForInterruptsInLoopNode SINGLETON = new CheckForInterruptsInLoopNode();

    public interface PossibleSendMarker {
    }

    private CheckForInterruptsInLoopNode() {
    }

    public static CheckForInterruptsInLoopNode createForLoop(final Object[] data, final int currentPC, final int numBytecodes, final int offset) {
        CompilerAsserts.neverPartOfCompilation();
        if (SqueakImageContext.getSlow().interruptHandlerDisabled()) {
            return null;
        }
        final int loopStart = currentPC + numBytecodes + offset;
        assert offset < 0 : "back jumps only";
        for (int i = loopStart; i < currentPC; i++) {
            /*
             * FIXME?: Search for call nodes but reject the ones from closure primitives as they do
             * not check for interrupts.
             */
            // if ((NodeUtil.findFirstNodeInstance(abs, DirectCallNode.class) != null ||
            // NodeUtil.findFirstNodeInstance(abs, IndirectCallNode.class) != null) &&
            // NodeUtil.findFirstNodeInstance(abs, AbstractClosurePrimitiveNode.class) == null) {
            if (data[i] instanceof PossibleSendMarker) {
                return null;
            }
        }
        return SINGLETON;
    }

    public void execute(final VirtualFrame frame, final int pc) {
        final SqueakImageContext image = getContext();
        final CheckForInterruptsState istate = image.interrupt;
        if (istate.shouldSkip()) {
            return;
        }
        /* Exclude interrupts case from compilation. */
        CompilerDirectives.transferToInterpreter();
        FrameAccess.setInstructionPointer(frame, pc);
        final Object[] specialObjects = image.specialObjectsArray.getObjectStorage();
        boolean switchToNewProcess = false;
        if (istate.tryInterruptPending()) {
            switchToNewProcess |= SignalSemaphoreNode.executeUncached(frame, image, specialObjects[SPECIAL_OBJECT.THE_INTERRUPT_SEMAPHORE]);
        }
        if (istate.tryWakeUpTickTrigger()) {
            switchToNewProcess |= SignalSemaphoreNode.executeUncached(frame, image, specialObjects[SPECIAL_OBJECT.THE_TIMER_SEMAPHORE]);
        }
        if (istate.tryPendingFinalizations()) {
            switchToNewProcess |= SignalSemaphoreNode.executeUncached(frame, image, specialObjects[SPECIAL_OBJECT.THE_FINALIZATION_SEMAPHORE]);
        }
        if (istate.trySemaphoresToSignal()) {
            final ArrayObject externalObjects = (ArrayObject) specialObjects[SPECIAL_OBJECT.EXTERNAL_OBJECTS_ARRAY];
            if (!externalObjects.isEmptyType()) { // signal external semaphores
                final Object[] semaphores = externalObjects.getObjectStorage();
                Integer semaIndex;
                while ((semaIndex = istate.nextSemaphoreToSignal()) != null) {
                    switchToNewProcess |= SignalSemaphoreNode.executeUncached(frame, image, semaphores[semaIndex - 1]);
                }
            }
        }
        /*
         * OpenSmalltalk VM signals all semaphores and switches to the highest priority process. If
         * we do not do this, small Delays in a loop in the image will prevent the code after the
         * wake-up-tick handler from getting executed (finalizations, for example).
         */
        if (switchToNewProcess) {
            throw ProcessSwitch.SINGLETON;
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
