/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.interrupts.CheckForInterruptsQuickNode;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@NodeInfo(language = SqueakLanguageConfig.ID, cost = NodeCost.NONE)
public final class StartContextRootNode extends RootNode {
    private final CompiledCodeObject code;
    @CompilationFinal private int initialPC;
    @CompilationFinal private int initialSP;

    @Children private FrameStackWriteNode[] writeTempNodes;
    @Child private CheckForInterruptsQuickNode interruptHandlerNode;
    @Child private AbstractExecuteContextNode executeBytecodeNode;
    @Child private GetOrCreateContextNode getOrCreateContextNode;
    @Child private MaterializeContextOnMethodExitNode materializeContextOnMethodExitNode = MaterializeContextOnMethodExitNode.create();

    public StartContextRootNode(final SqueakLanguage language, final CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        this.code = code;
        interruptHandlerNode = CheckForInterruptsQuickNode.create(code);
        executeBytecodeNode = new ExecuteBytecodeNode(code);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        initializeFrame(frame);
        try {
            interruptHandlerNode.execute(frame);
            return executeBytecodeNode.execute(frame, initialPC);
        } catch (final NonVirtualReturn | ProcessSwitch nvr) {
            /** {@link getGetOrCreateContextNode()} acts as {@link BranchProfile} */
            getGetOrCreateContextNode().executeGet(frame).markEscaped();
            throw nvr;
        } finally {
            materializeContextOnMethodExitNode.execute(frame);
        }
    }

    @ExplodeLoop
    public void initializeFrame(final VirtualFrame frame) {
        if (writeTempNodes == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final BlockClosureObject closure = FrameAccess.getClosure(frame);
            final int numArgs = FrameAccess.getNumArguments(frame);
            if (closure == null) {
                initialPC = code.getInitialPC();
                initialSP = code.getNumTemps();
                assert numArgs == code.getNumArgs();
            } else {
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
        FrameAccess.setInstructionPointer(frame, code, initialPC);
        FrameAccess.setStackPointer(frame, code, initialSP);

        // TODO: avoid nilling out of temp slots to allow slot specializations
        // Initialize remaining temporary variables with nil in newContext.
        for (int i = 0; i < writeTempNodes.length; i++) {
            writeTempNodes[i].executeWrite(frame, NilObject.SINGLETON);
        }
    }

    private GetOrCreateContextNode getGetOrCreateContextNode() {
        if (getOrCreateContextNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getOrCreateContextNode = insert(GetOrCreateContextNode.create());
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
        return code.toString();
    }

    @Override
    public boolean isCloningAllowed() {
        return true;
    }
}
