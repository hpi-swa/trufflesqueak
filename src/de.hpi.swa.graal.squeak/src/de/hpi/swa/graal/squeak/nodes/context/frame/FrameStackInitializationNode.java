/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

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
                code = FrameAccess.getMethod(frame);
                initialSP = code.getNumTemps();
            } else {
                code = closure.getCompiledBlock();
                initialSP = code.getNumArgsAndCopied();
            }
            numArgs = code.getNumArgsAndCopied();
            writeNodes = new FrameSlotWriteNode[initialSP];
            for (int i = 0; i < writeNodes.length; i++) {
                writeNodes[i] = insert(FrameSlotWriteNode.create(code.getStackSlot(i)));
            }
        }

        CompilerDirectives.isCompilationConstant(writeNodes.length);
        final Object[] arguments = frame.getArguments();
        assert arguments.length == FrameAccess.expectedArgumentSize(numArgs);
        for (int i = 0; i < numArgs; i++) {
            writeNodes[i].executeWrite(frame, arguments[FrameAccess.getArgumentStartIndex() + i]);
        }
        // Initialize remaining temporary variables with nil in newContext.
        for (int i = numArgs; i < writeNodes.length; i++) {
            writeNodes[i].executeWrite(frame, NilObject.SINGLETON);
        }
        FrameAccess.setStackPointer(frame, stackPointerSlot, writeNodes.length);
    }
}
