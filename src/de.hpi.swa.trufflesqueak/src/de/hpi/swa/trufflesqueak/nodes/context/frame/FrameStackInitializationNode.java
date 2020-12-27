/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class FrameStackInitializationNode extends AbstractNode {
    @CompilationFinal private FrameSlot stackPointerSlot;
    @CompilationFinal private int numArgs;
    @Children private FrameSlotWriteNode[] writeNodes;

    public static FrameStackInitializationNode create() {
        return new FrameStackInitializationNode();
    }

    @ExplodeLoop
    public void executeInitialize(final VirtualFrame frame) {
        if (stackPointerSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackPointerSlot = FrameAccess.getStackPointerSlot(frame);
            final CompiledCodeObject code;
            final int initialSP;
            final BlockClosureObject closure = FrameAccess.getClosure(frame);
            if (closure == null) {
                code = FrameAccess.getCodeObject(frame);
                initialSP = code.getNumTemps();
                numArgs = code.getNumArgs();
            } else {
                code = closure.getCompiledBlock();
                initialSP = closure.getNumTemps();
                numArgs = (int) (closure.getNumArgs() + closure.getNumCopied());
            }
            writeNodes = new FrameSlotWriteNode[initialSP - numArgs];
            for (int i = 0; i < writeNodes.length; i++) {
                writeNodes[i] = insert(FrameSlotWriteNode.create(frame, initialSP + i));
            }
        }
        CompilerAsserts.partialEvaluationConstant(writeNodes.length);
        final Object[] arguments = frame.getArguments();
        assert arguments.length == FrameAccess.expectedArgumentSize(numArgs);
        // Initialize remaining temporary variables with nil in newContext.
        for (int i = 0; i < writeNodes.length; i++) {
            writeNodes[i].executeWrite(frame, NilObject.SINGLETON);
        }
        FrameAccess.setStackPointer(frame, stackPointerSlot, numArgs + writeNodes.length);
    }
}
