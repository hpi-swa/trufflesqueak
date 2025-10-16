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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
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
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextWithFrameNode;
import de.hpi.swa.trufflesqueak.nodes.interrupts.CheckForInterruptsQuickNode;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@NodeInfo(language = SqueakLanguageConfig.ID)
public final class StartContextRootNode extends AbstractRootNode {
    @CompilationFinal private int initialPC;
    @CompilationFinal private int initialSP;
    @CompilationFinal private Assumption doesNotNeedThisContext;

    @CompilationFinal private final SqueakImageContext image;

    @Children private FrameStackWriteNode[] writeTempNodes;
    @Child private CheckForInterruptsQuickNode interruptHandlerNode;
    @Child private AbstractExecuteContextNode executeBytecodeNode;
    @Child private GetOrCreateContextWithFrameNode getOrCreateContextNode;
    @Child private MaterializeContextOnMethodExitNode materializeContextOnMethodExitNode = MaterializeContextOnMethodExitNode.create();

    public StartContextRootNode(final SqueakLanguage language, final CompiledCodeObject code) {
        super(language, code);
        image = code.getSqueakClass().getImage();
        interruptHandlerNode = CheckForInterruptsQuickNode.createForSend(code);
        executeBytecodeNode = new ExecuteBytecodeNode(code);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        initializeFrame(frame);
        try {
            if (image.enteringContextExceedsDepth()) {
                CompilerDirectives.transferToInterpreter();
                // Suspend current context and throw ProcessSwitch to unwind Java stack and resume
                final ContextObject activeContext = GetOrCreateContextWithFrameNode.executeUncached(frame);
                AbstractPointersObjectWriteNode.executeUncached(image.getActiveProcessSlow(), PROCESS.SUSPENDED_CONTEXT, activeContext);
                throw ProcessSwitch.SINGLETON;
            }
            interruptHandlerNode.execute(frame);
            return executeBytecodeNode.execute(frame, initialPC);
        } catch (final NonVirtualReturn | ProcessSwitch | CannotReturnToTarget nvr) {
            /* {@link getGetOrCreateContextNode()} acts as {@link BranchProfile} */
            getGetOrCreateContextNode().executeGet(frame).markEscaped();
            throw nvr;
        } finally {
            image.exitingContext();
            materializeContextOnMethodExitNode.execute(frame);
        }
    }

    @ExplodeLoop
    private void initializeFrame(final VirtualFrame frame) {
        if (writeTempNodes == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final int numArgs = FrameAccess.getNumArguments(frame);
            final CompiledCodeObject code = getCode();
            doesNotNeedThisContext = code.getDoesNotNeedThisContextAssumption();
            if (!FrameAccess.hasClosure(frame)) {
                initialPC = code.getInitialPC();
                initialSP = code.getNumTemps();
                assert numArgs == code.getNumArgs();
            } else {
                final BlockClosureObject closure = FrameAccess.getClosure(frame);
                initialPC = (int) closure.getStartPC();
                initialSP = closure.getNumTemps();
                assert numArgs == closure.getNumArgs() + closure.getNumCopied();
            }
            writeTempNodes = new FrameStackWriteNode[initialSP - numArgs];
            for (int i = 0; i < writeTempNodes.length; i++) {
                writeTempNodes[i] = insert(FrameStackWriteNode.create(frame, numArgs + i));
                assert writeTempNodes[i] instanceof FrameSlotWriteNode;
            }
        }
        if (!doesNotNeedThisContext.isValid()) {
            getGetOrCreateContextNode().executeGet(frame);
        }
        FrameAccess.setInstructionPointer(frame, initialPC);
        FrameAccess.setStackPointer(frame, initialSP);

        // TODO: avoid nilling out of temp slots to allow slot specializations
        // Initialize remaining temporary variables with nil in newContext.
        for (final FrameStackWriteNode node : writeTempNodes) {
            node.executeWrite(frame, NilObject.SINGLETON);
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
    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return getCode().toString();
    }

    @Override
    public boolean isCloningAllowed() {
        return true;
    }
}
