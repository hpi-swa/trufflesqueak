/*
 * Copyright (c) 2021-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interrupts;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DenyReplace;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.process.SignalSemaphoreNode;

public abstract class CheckForInterruptsQuickNode extends AbstractNode {
    private static final int MIN_NUMBER_OF_BYTECODE_FOR_INTERRUPT_CHECKS = 32;

    public static final CheckForInterruptsQuickNode createForSend(final CompiledCodeObject code) {
        /*
         * Only check for interrupts if method is relatively large. Avoid check if primitive method
         * or if a closure is activated (effectively what #primitiveClosureValueNoContextSwitch is
         * for).
         */

        if (SqueakImageContext.getSlow().interruptHandlerDisabled() || code.hasPrimitive() || //
                        code.getBytes().length < MIN_NUMBER_OF_BYTECODE_FOR_INTERRUPT_CHECKS || //
                        /* FullBlockClosure or normal closure */
                        code.isCompiledBlock() || code.hasOuterMethod()) {
            return NoCheckForInterruptsNode.SINGLETON;
        } else {
            return CheckForInterruptsQuickImplNode.SINGLETON;
        }
    }

    public abstract void execute(VirtualFrame frame);

    @DenyReplace
    private static final class NoCheckForInterruptsNode extends CheckForInterruptsQuickNode {
        private static final NoCheckForInterruptsNode SINGLETON = new NoCheckForInterruptsNode();

        @Override
        public void execute(final VirtualFrame frame) {
            // nothing to do
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
            return SINGLETON;
        }
    }

    @DenyReplace
    public static final class CheckForInterruptsQuickImplNode extends CheckForInterruptsQuickNode {
        private static final CheckForInterruptsQuickImplNode SINGLETON = new CheckForInterruptsQuickImplNode();

        private CheckForInterruptsQuickImplNode() {
        }

        @NeverDefault
        public static CheckForInterruptsQuickImplNode create() {
            return SINGLETON;
        }

        @Override
        public void execute(final VirtualFrame frame) {
            final SqueakImageContext image = getContext();
            final CheckForInterruptsState istate = image.interrupt;
            if (istate.shouldSkip()) {
                return;
            }
            /* Exclude interrupts case from compilation. */
            CompilerDirectives.transferToInterpreter();
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
             * OpenSmalltalk VM signals all semaphores and switches to the highest priority process.
             * If we do not do this, small Delays in a loop in the image will prevent the code after
             * the wake-up-tick handler from getting executed (finalizations, for example).
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
}
