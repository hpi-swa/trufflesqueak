/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
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
public final class EnterCodeNode extends RootNode {
    private static final int MIN_NUMBER_OF_BYTECODE_FOR_INTERRUPT_CHECKS = 32;

    private final CompiledCodeObject code;
    @CompilationFinal private int initialPC;
    @CompilationFinal private int initialSP;

    @Children private FrameStackWriteNode[] writeTempNodes;
    @Child private AbstractExecuteContextNode executeContextNode;
    @Child private InterruptHandlerNode interruptHandlerNode;
    @Child private GetOrCreateContextNode getOrCreateContextNode;
    @Child private MaterializeContextOnMethodExitNode materializeContextOnMethodExitNode = MaterializeContextOnMethodExitNode.create();

    protected EnterCodeNode(final SqueakLanguage language, final CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        this.code = code;
        executeContextNode = ExecuteContextNode.create(code);
        /*
         * Only check for interrupts if method is relatively large. Avoid check if a closure is
         * activated (effectively what #primitiveClosureValueNoContextSwitch is for). Also, skip
         * timer interrupts here as they trigger too often, which causes a lot of context switches
         * and therefore materialization and deopts. Timer inputs are currently handled in
         * primitiveRelinquishProcessor (#230) only.
         */
        interruptHandlerNode = code.isCompiledBlock() || code.getBytes().length < MIN_NUMBER_OF_BYTECODE_FOR_INTERRUPT_CHECKS ? null : InterruptHandlerNode.createOrNull(false);
    }

    public static EnterCodeNode create(final SqueakLanguage language, final CompiledCodeObject code) {
        return new EnterCodeNode(language, code);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        try {
            initializeFrame(frame);
            if (interruptHandlerNode != null) {
                interruptHandlerNode.executeTrigger(frame);
            }
            return executeContextNode.executeFresh(frame, initialPC);
        } catch (final NonVirtualReturn | ProcessSwitch nvr) {
            /** {@link getGetOrCreateContextNode()} acts as {@link BranchProfile} */
            getGetOrCreateContextNode().executeGet(frame).markEscaped();
            throw nvr;
        } finally {
            materializeContextOnMethodExitNode.execute(frame);
        }
    }

    @ExplodeLoop
    private void initializeFrame(final VirtualFrame frame) {
        if (writeTempNodes == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final BlockClosureObject closure = FrameAccess.getClosure(frame);
            final int numArgs;
            if (closure == null) {
                initialPC = code.getInitialPC();
                if (code.hasPrimitive()) { // skip primitive bytecode
                    initialPC += CallPrimitiveNode.NUM_BYTECODES;
                }
                initialSP = FrameAccess.getCodeObject(frame).getNumTemps();
                numArgs = code.getNumArgs();
            } else {
                initialPC = (int) closure.getStartPC();
                initialSP = closure.getNumTemps();
                numArgs = (int) (closure.getNumArgs() + closure.getNumCopied());
            }
            writeTempNodes = new FrameStackWriteNode[initialSP - numArgs];
            for (int i = 0; i < writeTempNodes.length; i++) {
                writeTempNodes[i] = insert(FrameStackWriteNode.create(frame, numArgs + i));
                assert writeTempNodes[i] instanceof FrameSlotWriteNode;
            }
        }
        // TODO: avoid nilling out of temp slots to allow specializations
        for (int i = 0; i < writeTempNodes.length; i++) {
            writeTempNodes[i].executeWrite(frame, NilObject.SINGLETON);
        }
        FrameAccess.setInstructionPointer(frame, code, initialPC);
        FrameAccess.setStackPointer(frame, code, initialSP);
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
