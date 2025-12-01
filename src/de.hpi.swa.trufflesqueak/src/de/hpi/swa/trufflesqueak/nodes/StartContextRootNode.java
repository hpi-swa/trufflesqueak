/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.Returns.CannotReturnToTarget;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextWithFrameNode;
import de.hpi.swa.trufflesqueak.nodes.interrupts.CheckForInterruptsQuickNode;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

@NodeInfo(language = SqueakLanguageConfig.ID)
public final class StartContextRootNode extends AbstractRootNode {
    @CompilationFinal private int initialPC;
    @CompilationFinal private int initialSP;
    @CompilationFinal private byte numTempSlots;
    @CompilationFinal private byte tempStackSlotStartIndex;
    @CompilationFinal private Assumption doesNotNeedThisContext;

    @Child private CheckForInterruptsQuickNode interruptHandlerNode;
    @Child private GetOrCreateContextWithFrameNode getOrCreateContextNode;

    public StartContextRootNode(final SqueakImageContext image, final CompiledCodeObject code) {
        super(image, code);
        interruptHandlerNode = CheckForInterruptsQuickNode.createForSend(code);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        ensureInitialized(frame);
        initializeFrame(frame);
        final SqueakImageContext image = SqueakImageContext.get(this);
        final boolean isCountableStackFrame = CompilerDirectives.inInterpreter() || CompilerDirectives.inCompilationRoot();
        try {
            if (isCountableStackFrame && image.enteringContextExceedsDepth()) {
                CompilerDirectives.transferToInterpreter();
                // Suspend current context and throw ProcessSwitch to unwind Java stack and resume
                final ContextObject activeContext = GetOrCreateContextWithFrameNode.executeUncached(frame);
                AbstractPointersObjectWriteNode.executeUncached(image.getActiveProcessSlow(), PROCESS.SUSPENDED_CONTEXT, activeContext);
                throw ProcessSwitch.SINGLETON;
            }
            interruptHandlerNode.execute(frame);
            return executeBytecodeNode.execute(frame, initialPC, initialSP);
        } catch (final NonVirtualReturn | ProcessSwitch | CannotReturnToTarget nvr) {
            /* {@link getGetOrCreateContextNode()} acts as {@link BranchProfile} */
            getGetOrCreateContextNode().executeGet(frame);
            throw nvr;
        } finally {
            if (isCountableStackFrame) {
                image.exitingContext();
            }
        }
    }

    private void ensureInitialized(final VirtualFrame frame) {
        if (doesNotNeedThisContext == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final int numArgs = FrameAccess.getNumArguments(frame);
            final CompiledCodeObject code = getCode();
            doesNotNeedThisContext = code.getDoesNotNeedThisContextAssumption();
            if (!FrameAccess.hasClosure(frame)) {
                initialPC = 0;
                initialSP = code.getNumTemps();
                assert numArgs == code.getNumArgs();
            } else {
                final BlockClosureObject closure = FrameAccess.getClosure(frame);
                initialPC = (int) closure.getStartPC() - code.getInitialPC();
                initialSP = closure.getNumTemps();
                assert numArgs == closure.getNumArgs() + closure.getNumCopied();
            }
            numTempSlots = MiscUtils.toByteExact(initialSP - numArgs);
            tempStackSlotStartIndex = MiscUtils.toByteExact(FrameAccess.toStackSlotIndex(frame, numArgs));
        }
    }

    @ExplodeLoop
    private void initializeFrame(final VirtualFrame frame) {
        if (!doesNotNeedThisContext.isValid()) {
            getGetOrCreateContextNode().executeGet(frame);
        }
        FrameAccess.setInstructionPointer(frame, initialPC);
        FrameAccess.setStackPointer(frame, initialSP);

        // TODO: avoid nilling out of temp slots to allow slot specializations
        // Initialize remaining temporary variables with nil in newContext.
        final FrameDescriptor descriptor = frame.getFrameDescriptor();
        for (int i = 0; i < numTempSlots; i++) {
            final int slotIndex = tempStackSlotStartIndex + i;
            descriptor.setSlotKind(slotIndex, FrameSlotKind.Object);
            frame.setObject(slotIndex, NilObject.SINGLETON);
        }
    }

    private GetOrCreateContextWithFrameNode getGetOrCreateContextNode() {
        if (getOrCreateContextNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getOrCreateContextNode = insert(GetOrCreateContextWithFrameNode.create());
        }
        return getOrCreateContextNode;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return getCode().toString();
    }
}
