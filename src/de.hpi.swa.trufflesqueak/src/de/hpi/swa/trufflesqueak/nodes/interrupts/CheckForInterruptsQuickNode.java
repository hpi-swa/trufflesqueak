/*
 * Copyright (c) 2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interrupts;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.trufflesqueak.nodes.process.SignalSemaphoreNode;
import de.hpi.swa.trufflesqueak.util.LogUtils;

public abstract class CheckForInterruptsQuickNode extends Node {
    private static final int MIN_NUMBER_OF_BYTECODE_FOR_INTERRUPT_CHECKS = 32;

    public static CheckForInterruptsQuickNode create(final CompiledCodeObject code) {
        final SqueakImageContext image = code.getSqueakClass().getImage();
        /*
         * Only check for interrupts if method is relatively large. Avoid check if primitive method
         * or if a closure is activated (effectively what #primitiveClosureValueNoContextSwitch is
         * for).
         */
        if (image.interruptHandlerDisabled() || code.hasPrimitive() || //
                        code.getBytes().length < MIN_NUMBER_OF_BYTECODE_FOR_INTERRUPT_CHECKS || //
                        /* FullBlockClosure or normal closure */
                        code.isCompiledBlock() || code.hasOuterMethod()) {
            return NoCheckForInterruptsNode.SINGLETON;
        } else {
            return new CheckForInterruptsQuickImplNode(image);
        }
    }

    public abstract void execute(VirtualFrame frame);

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
    }

    private static final class CheckForInterruptsQuickImplNode extends CheckForInterruptsQuickNode {
        private final Object[] specialObjects;
        private final CheckForInterruptsState istate;

        @Child private SignalSemaphoreNode signalSemaporeNode = SignalSemaphoreNode.create();

        protected CheckForInterruptsQuickImplNode(final SqueakImageContext image) {
            specialObjects = image.specialObjectsArray.getObjectStorage();
            istate = image.interrupt;
        }

        @Override
        public void execute(final VirtualFrame frame) {
            if (CompilerDirectives.inCompiledCode() && !CompilerDirectives.inCompilationRoot() || !istate.shouldTriggerNoTimer()) {
                return;
            }
            /* Exclude interrupts case from compilation. */
            CompilerDirectives.transferToInterpreter();
            if (istate.interruptPending()) {
                LogUtils.INTERRUPTS.fine("User interrupt");
                istate.interruptPending = false; // reset interrupt flag
                signalSemaporeNode.executeSignal(frame, istate.getInterruptSemaphore());
            }
            // TODO: timer interrupts disabled in quick checkForInterrupts. That is also why
            // shouldTriggerNoTimer is used.
            // if (istate.nextWakeUpTickTrigger()) {
            // LogUtils.INTERRUPTS.fine("Timer interrupt");
            // istate.nextWakeupTick = 0; // reset timer interrupt
            // signalSemaporeNode.executeSignal(frame, istate.getTimerSemaphore());
            // }
            if (istate.pendingFinalizationSignals()) { // signal any pending finalizations
                LogUtils.INTERRUPTS.fine("Finalization interrupt");
                istate.setPendingFinalizations(false);
                signalSemaporeNode.executeSignal(frame, specialObjects[SPECIAL_OBJECT.THE_FINALIZATION_SEMAPHORE]);
            }
            if (istate.hasSemaphoresToSignal()) {
                LogUtils.INTERRUPTS.fine("Semaphore interrupt");
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
}
