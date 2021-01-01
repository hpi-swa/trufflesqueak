/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class FrameStackPointerIncrementNode extends AbstractNode {
    @CompilationFinal private FrameSlot stackPointerSlot;
    @CompilationFinal private int stackPointer = -1;

    public static FrameStackPointerIncrementNode create() {
        return new FrameStackPointerIncrementNode();
    }

    public void execute(final VirtualFrame frame) {
        if (stackPointerSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackPointerSlot = FrameAccess.getStackPointerSlot(frame);
            stackPointer = FrameAccess.getStackPointer(frame, stackPointerSlot) + 1;
            assert stackPointer <= CONTEXT.MAX_STACK_SIZE : "Bad stack pointer";
        }
        FrameAccess.setStackPointer(frame, stackPointerSlot, stackPointer);
    }
}
