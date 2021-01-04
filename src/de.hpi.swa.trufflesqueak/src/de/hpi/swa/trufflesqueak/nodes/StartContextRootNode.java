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
import de.hpi.swa.trufflesqueak.nodes.bytecodes.MiscellaneousBytecodes.CallPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.InterruptHandlerNode;

@NodeInfo(language = SqueakLanguageConfig.ID, cost = NodeCost.NONE)
public final class StartContextRootNode extends RootNode {
    private static final int MIN_NUMBER_OF_BYTECODE_FOR_INTERRUPT_CHECKS = 32;

    private final CompiledCodeObject code;
    @CompilationFinal private int initialPC;
    @CompilationFinal private int initialSP;

    @Children private FrameStackWriteNode[] writeTempNodes;
    @Child private InterruptHandlerNode interruptHandlerNode;
    @Child private AbstractExecuteContextNode executeBytecodeNode;
    @Child private GetOrCreateContextNode getOrCreateContextNode;
    @Child private MaterializeContextOnMethodExitNode materializeContextOnMethodExitNode = MaterializeContextOnMethodExitNode.create();

    protected StartContextRootNode(final SqueakLanguage language, final CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        this.code = code;
        /*
         * Only check for interrupts if method is relatively large. Avoid check if a closure is
         * activated (effectively what #primitiveClosureValueNoContextSwitch is for). Also, skip
         * timer interrupts here as they trigger too often, which causes a lot of context switches
         * and therefore materialization and deopts. Time r inputs are currently handled in
         * primitiveRelinquishProcessor (#230) only.
         */
        interruptHandlerNode = code.isCompiledBlock() || code.getBytes().length < MIN_NUMBER_OF_BYTECODE_FOR_INTERRUPT_CHECKS ? null : InterruptHandlerNode.createOrNull(false);
        executeBytecodeNode = new ExecuteBytecodeNode(code);
    }

    public static StartContextRootNode create(final SqueakLanguage language, final CompiledCodeObject code) {
        return new StartContextRootNode(language, code);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        initializeFrame(frame);
        try {
            if (interruptHandlerNode != null) {
                interruptHandlerNode.executeTrigger(frame);
            }
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
                if (code.hasPrimitive()) { // skip primitive bytecode
                    initialPC += CallPrimitiveNode.NUM_BYTECODES;
                }
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
            getOrCreateContextNode = insert(GetOrCreateContextNode.create(false));
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
