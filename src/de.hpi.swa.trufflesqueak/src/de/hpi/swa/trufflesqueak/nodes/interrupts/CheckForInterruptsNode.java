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
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodesFactory.AbstractPointersObjectReadNodeGen;
import de.hpi.swa.trufflesqueak.nodes.process.SignalSemaphoreForInterruptNode;
import de.hpi.swa.trufflesqueak.nodes.process.TransferToNode;
import de.hpi.swa.trufflesqueak.nodes.process.TransferToNodeGen;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class CheckForInterruptsNode extends AbstractNode {

    /**
     * Shared signaling logic used by all Nodes.
     */
    protected static PointersObject signalSemaphoresCached(final CheckForInterruptsState istate, final Object[] specialObjects, final SignalSemaphoreForInterruptNode signalNode,
                    final PointersObject activeProcess, final boolean activeProcessYields) {
        PointersObject nextActiveProcess = activeProcess;
        boolean nextActiveProcessYields = activeProcessYields;

        if (istate.tryInterruptPending()) {
            final PointersObject result = signalNode.executeSignal(specialObjects[SPECIAL_OBJECT.THE_INTERRUPT_SEMAPHORE], nextActiveProcess, nextActiveProcessYields);
            if (result != nextActiveProcess) {
                nextActiveProcessYields = true;
                nextActiveProcess = result;
            }
        }
        if (istate.tryWakeUpTickTrigger()) {
            final PointersObject result = signalNode.executeSignal(specialObjects[SPECIAL_OBJECT.THE_TIMER_SEMAPHORE], nextActiveProcess, nextActiveProcessYields);
            if (result != nextActiveProcess) {
                nextActiveProcessYields = true;
                nextActiveProcess = result;
            }
        }
        if (istate.tryPendingFinalizations()) {
            final PointersObject result = signalNode.executeSignal(specialObjects[SPECIAL_OBJECT.THE_FINALIZATION_SEMAPHORE], nextActiveProcess, nextActiveProcessYields);
            if (result != nextActiveProcess) {
                nextActiveProcessYields = true;
                nextActiveProcess = result;
            }
        }
        if (istate.trySemaphoresToSignal()) {
            final ArrayObject externalObjects = (ArrayObject) specialObjects[SPECIAL_OBJECT.EXTERNAL_OBJECTS_ARRAY];
            if (!externalObjects.isEmptyType()) {
                final Object[] semaphores = externalObjects.getObjectStorage();
                Integer semaIndex;
                while ((semaIndex = istate.nextSemaphoreToSignal()) != null) {
                    final PointersObject result = signalNode.executeSignal(semaphores[semaIndex - 1], nextActiveProcess, nextActiveProcessYields);
                    if (result != nextActiveProcess) {
                        nextActiveProcessYields = true;
                        nextActiveProcess = result;
                    }
                }
            }
        }
        return nextActiveProcess;
    }

    protected static PointersObject signalSemaphoresUncached(final SqueakImageContext image, final CheckForInterruptsState istate, final Object[] specialObjects, final PointersObject activeProcess,
                    final boolean activeProcessYields) {
        PointersObject nextActiveProcess = activeProcess;
        boolean nextActiveProcessYields = activeProcessYields;

        if (istate.tryInterruptPending()) {
            final PointersObject result = SignalSemaphoreForInterruptNode.executeUncached(image, specialObjects[SPECIAL_OBJECT.THE_INTERRUPT_SEMAPHORE], nextActiveProcess, nextActiveProcessYields);
            if (result != nextActiveProcess) {
                nextActiveProcessYields = true;
                nextActiveProcess = result;
            }
        }
        if (istate.tryWakeUpTickTrigger()) {
            final PointersObject result = SignalSemaphoreForInterruptNode.executeUncached(image, specialObjects[SPECIAL_OBJECT.THE_TIMER_SEMAPHORE], nextActiveProcess, nextActiveProcessYields);
            if (result != nextActiveProcess) {
                nextActiveProcessYields = true;
                nextActiveProcess = result;
            }
        }
        if (istate.tryPendingFinalizations()) {
            final PointersObject result = SignalSemaphoreForInterruptNode.executeUncached(image, specialObjects[SPECIAL_OBJECT.THE_FINALIZATION_SEMAPHORE], nextActiveProcess, nextActiveProcessYields);
            if (result != nextActiveProcess) {
                nextActiveProcessYields = true;
                nextActiveProcess = result;
            }
        }
        if (istate.trySemaphoresToSignal()) {
            final ArrayObject externalObjects = (ArrayObject) specialObjects[SPECIAL_OBJECT.EXTERNAL_OBJECTS_ARRAY];
            if (!externalObjects.isEmptyType()) {
                final Object[] semaphores = externalObjects.getObjectStorage();
                Integer semaIndex;
                while ((semaIndex = istate.nextSemaphoreToSignal()) != null) {
                    final PointersObject result = SignalSemaphoreForInterruptNode.executeUncached(image, semaphores[semaIndex - 1], nextActiveProcess, nextActiveProcessYields);
                    if (result != nextActiveProcess) {
                        nextActiveProcessYields = true;
                        nextActiveProcess = result;
                    }
                }
            }
        }
        return nextActiveProcess;
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

            final PointersObject originalActiveProcess = image.getActiveProcessSlow();
            final PointersObject nextActiveProcess = signalSemaphoresUncached(image, image.interrupt, specialObjects, originalActiveProcess, image.flags.preemptionYields());

            if (nextActiveProcess != originalActiveProcess) {
                TransferToNode.executeUncached(frame, nextActiveProcess);
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

                final PointersObject originalActiveProcess = image.getActiveProcessSlow();
                final PointersObject nextActiveProcess = signalSemaphoresUncached(image, istate, specialObjects, originalActiveProcess, image.flags.preemptionYields());

                if (nextActiveProcess != originalActiveProcess) {
                    TransferToNode.executeUncached(frame, nextActiveProcess);
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
        @Child private SignalSemaphoreForInterruptNode signalSemaphoreNode;
        @Child private AbstractPointersObjectReadNode readNode;
        @Child private TransferToNode transferToNode;

        private final SqueakImageContext image;
        private final CheckForInterruptsState istate;
        private final Object[] specialObjects;

        private CheckForInterruptsFullNode(final SqueakImageContext image) {
            this.image = image;
            istate = image.interrupt;
            specialObjects = image.specialObjectsArray.getObjectStorage();
            signalSemaphoreNode = SignalSemaphoreForInterruptNode.create();
            readNode = AbstractPointersObjectReadNodeGen.create();
            transferToNode = TransferToNodeGen.create();
        }

        @NeverDefault
        public static CheckForInterruptsFullNode create() {
            return new CheckForInterruptsFullNode(SqueakImageContext.getSlow());
        }

        public void execute(final VirtualFrame frame) {
            if (istate.shouldSkip()) {
                return;
            }
            final PointersObject originalActiveProcess = readNode.executePointers(image.getScheduler(), PROCESS_SCHEDULER.ACTIVE_PROCESS);
            final PointersObject nextActiveProcess = signalSemaphoresCached(istate, specialObjects, signalSemaphoreNode, originalActiveProcess, image.flags.preemptionYields());

            if (nextActiveProcess != originalActiveProcess) {
                transferToNode.execute(frame, nextActiveProcess);
                throw ProcessSwitch.SINGLETON;
            }
        }
    }
}
