/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interrupts;

import com.oracle.truffle.api.CompilerAsserts;
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
import de.hpi.swa.trufflesqueak.nodes.dispatch.AbstractDispatchNode;
import de.hpi.swa.trufflesqueak.nodes.process.SignalSemaphoreNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class CheckForInterruptsNode extends AbstractNode {

    /**
     * Shared signaling logic used by all Nodes.
     */
    protected static boolean signalSemaphoresCached(final VirtualFrame frame, final CheckForInterruptsState istate, final Object[] specialObjects, final SignalSemaphoreNode signalNode) {
        boolean switchToNewProcess = false;

        if (istate.tryInterruptPending()) {
            switchToNewProcess |= signalNode.executeSignal(frame, specialObjects[SPECIAL_OBJECT.THE_INTERRUPT_SEMAPHORE]);
        }
        if (istate.tryWakeUpTickTrigger()) {
            switchToNewProcess |= signalNode.executeSignal(frame, specialObjects[SPECIAL_OBJECT.THE_TIMER_SEMAPHORE]);
        }
        if (istate.tryPendingFinalizations()) {
            switchToNewProcess |= signalNode.executeSignal(frame, specialObjects[SPECIAL_OBJECT.THE_FINALIZATION_SEMAPHORE]);
        }
        if (istate.trySemaphoresToSignal()) {
            final ArrayObject externalObjects = (ArrayObject) specialObjects[SPECIAL_OBJECT.EXTERNAL_OBJECTS_ARRAY];
            if (!externalObjects.isEmptyType()) {
                final Object[] semaphores = externalObjects.getObjectStorage();
                Integer semaIndex;
                while ((semaIndex = istate.nextSemaphoreToSignal()) != null) {
                    switchToNewProcess |= signalNode.executeSignal(frame, semaphores[semaIndex - 1]);
                }
            }
        }
        return switchToNewProcess;
    }

    protected static boolean signalSemaphoresUncached(final VirtualFrame frame, final SqueakImageContext image, final CheckForInterruptsState istate, final Object[] specialObjects) {
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
            if (!externalObjects.isEmptyType()) {
                final Object[] semaphores = externalObjects.getObjectStorage();
                Integer semaIndex;
                while ((semaIndex = istate.nextSemaphoreToSignal()) != null) {
                    switchToNewProcess |= SignalSemaphoreNode.executeUncached(frame, image, semaphores[semaIndex - 1]);
                }
            }
        }
        return switchToNewProcess;
    }

    @DenyReplace
    public static final class CheckForInterruptsInLoopNode extends CheckForInterruptsNode {
        private static final CheckForInterruptsInLoopNode SINGLETON = new CheckForInterruptsInLoopNode();

        private CheckForInterruptsInLoopNode() {
        }

        public static CheckForInterruptsInLoopNode createForLoop() {
            CompilerAsserts.neverPartOfCompilation();
            if (SqueakImageContext.getSlow().interruptHandlerDisabled()) {
                return null;
            }
            return SINGLETON;
        }

        public void execute(final VirtualFrame frame, final int pc, final int sp) {
            final SqueakImageContext image = getContext();
            if (image.interrupt.shouldSkip()) {
                return;
            }
            /* Exclude interrupts case from compilation. */
            CompilerDirectives.transferToInterpreter();
            FrameAccess.externalizePCAndSP(frame, pc, sp);
            final Object[] specialObjects = image.specialObjectsArray.getObjectStorage();
            if (signalSemaphoresUncached(frame, image, image.interrupt, specialObjects)) {
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

    public abstract static class CheckForInterruptsQuickNode extends CheckForInterruptsNode {
        private static final int MIN_NUMBER_OF_BYTECODE_FOR_INTERRUPT_CHECKS = 32;

        public static CheckForInterruptsQuickNode createForSend(final CompiledCodeObject code) {
            /*
             * Only check for interrupts if method is relatively large. Avoid check if primitive
             * method or if a closure is activated (effectively what
             * #primitiveClosureValueNoContextSwitch is for).
             */
            if (SqueakImageContext.getSlow().interruptHandlerDisabled() || code.hasPrimitive() || //
                            code.getBytes().length < MIN_NUMBER_OF_BYTECODE_FOR_INTERRUPT_CHECKS || //
                            /* FullBlockClosure or normal closure */
                            code.isCompiledBlock() || code.isShadowBlock()) {
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
                if (signalSemaphoresUncached(frame, image, istate, specialObjects)) {
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

    public static final class CheckForInterruptsFullNode extends Node {
        @Child private SignalSemaphoreNode signalSemaphoreNode;

        private final CheckForInterruptsState istate;
        private final Object[] specialObjects;

        private CheckForInterruptsFullNode(final SqueakImageContext image) {
            istate = image.interrupt;
            specialObjects = image.specialObjectsArray.getObjectStorage();
            signalSemaphoreNode = SignalSemaphoreNode.create();
        }

        @NeverDefault
        public static CheckForInterruptsFullNode create() {
            return new CheckForInterruptsFullNode(SqueakImageContext.getSlow());
        }

        public void execute(final VirtualFrame frame) {
            if (istate.shouldSkip()) {
                return;
            }
            if (signalSemaphoresCached(frame, istate, specialObjects, signalSemaphoreNode)) {
                throw ProcessSwitch.SINGLETON;
            }
        }
    }
}
