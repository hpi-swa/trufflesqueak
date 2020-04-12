/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class CreateFrameArgumentsNode extends AbstractNodeWithCode {
    @CompilationFinal private int stackPointer = -1;
    @Children FrameSlotReadNode[] receiverAndArgumentsNodes;

    private CreateFrameArgumentsNode(final CompiledCodeObject code, final int argumentCount) {
        super(code);
        receiverAndArgumentsNodes = new FrameSlotReadNode[1 + argumentCount];
    }

    public static CreateFrameArgumentsNode create(final CompiledCodeObject code, final int argumentCount) {
        return new CreateFrameArgumentsNode(code, argumentCount);
    }

    public Object[] execute(final VirtualFrame frame, final CompiledMethodObject method, final Object sender) {
        if (stackPointer == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackPointer = FrameAccess.getStackPointer(frame, code) - receiverAndArgumentsNodes.length;
            assert stackPointer >= 0 : "Bad stack pointer";
            for (int i = 0; i < receiverAndArgumentsNodes.length; i++) {
                receiverAndArgumentsNodes[i] = insert(FrameSlotReadNode.create(code, stackPointer + i));
            }
        }
        FrameAccess.setStackPointer(frame, code, stackPointer);
        return FrameAccess.newWith(frame, method, sender, null, receiverAndArgumentsNodes);
    }
}
