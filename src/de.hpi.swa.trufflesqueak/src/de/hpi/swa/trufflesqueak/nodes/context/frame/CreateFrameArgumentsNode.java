/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class CreateFrameArgumentsNode extends AbstractNode {
    @CompilationFinal private FrameSlot stackPointerSlot;
    @CompilationFinal private int stackPointer;
    @CompilationFinal private FrameSlot stackSlot;
    private final int numReceiverAndArguments;

    private CreateFrameArgumentsNode(final int argumentCount) {
        numReceiverAndArguments = 1 + argumentCount;
    }

    public static CreateFrameArgumentsNode create(final int argumentCount) {
        return new CreateFrameArgumentsNode(argumentCount);
    }

    public Object[] execute(final VirtualFrame frame, final CompiledCodeObject method, final Object sender) {
        if (stackPointerSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackPointerSlot = FrameAccess.getStackPointerSlot(frame);
            stackPointer = FrameAccess.getStackPointer(frame, stackPointerSlot) - numReceiverAndArguments;
            assert stackPointer >= 0 : "Bad stack pointer";
            stackSlot = FrameAccess.getStackSlot(frame);
        }
        FrameAccess.setStackPointer(frame, stackPointerSlot, stackPointer);
        return FrameAccess.newWith(frame, method, sender, stackSlot, stackPointer, numReceiverAndArguments);
    }
}
