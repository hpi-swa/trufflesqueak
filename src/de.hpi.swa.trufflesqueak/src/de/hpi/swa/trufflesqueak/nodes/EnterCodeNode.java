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
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.InterruptHandlerNode;

@NodeInfo(language = SqueakLanguageConfig.ID, cost = NodeCost.NONE)
public final class EnterCodeNode extends RootNode {
    private static final int MIN_NUMBER_OF_BYTECODE_FOR_INTERRUPT_CHECKS = 32;

    private final CompiledCodeObject code;

    @CompilationFinal private int initialPC = -1;
    @CompilationFinal private int startPC = -1;
    @CompilationFinal private int numArgs;
    @Children private FrameSlotWriteNode[] writeNodes;
    @Child private AbstractExecuteContextNode executeContextNode;
    @Child private InterruptHandlerNode interruptHandlerNode;

    protected EnterCodeNode(final SqueakLanguage language, final CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        this.code = code;
        executeContextNode = ExecuteContextNode.create(code, false);
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
        if (interruptHandlerNode != null) {
            interruptHandlerNode.executeTrigger(frame);
        }
        initializeFrame(frame);
        return executeContextNode.executeFresh(frame);
    }

    @ExplodeLoop
    private void initializeFrame(final VirtualFrame frame) {
        if (writeNodes == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final int initialSP;
            final BlockClosureObject closure = FrameAccess.getClosure(frame);
            if (closure == null) {
                initialPC = code.getInitialPC();
                startPC = initialPC;
                initialSP = code.getNumTemps();
                numArgs = code.getNumArgs();
            } else {
                initialPC = closure.getCompiledBlock().getInitialPC();
                startPC = (int) closure.getStartPC();
                initialSP = closure.getNumTemps();
                numArgs = (int) (closure.getNumArgs() + closure.getNumCopied());
            }
            writeNodes = new FrameSlotWriteNode[initialSP];
            for (int i = 0; i < writeNodes.length; i++) {
                writeNodes[i] = insert(FrameSlotWriteNode.create(code.getStackSlot(i)));
            }
        }
        CompilerAsserts.partialEvaluationConstant(writeNodes.length);
        final Object[] arguments = frame.getArguments();
        assert arguments.length == FrameAccess.expectedArgumentSize(numArgs);
        for (int i = 0; i < numArgs; i++) {
            writeNodes[i].executeWrite(frame, arguments[FrameAccess.getArgumentStartIndex() + i]);
        }
        // Initialize remaining temporary variables with nil in newContext.
        for (int i = numArgs; i < writeNodes.length; i++) {
            writeNodes[i].executeWrite(frame, NilObject.SINGLETON);
        }
        FrameAccess.setInstructionPointer(frame, code, startPC);
        FrameAccess.setStackPointer(frame, code, writeNodes.length);
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
