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
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@NodeInfo(cost = NodeCost.NONE)
public final class FrameStackPushNode extends AbstractNode {
    @CompilationFinal private FrameSlot stackPointerSlot;
    @CompilationFinal private int stackPointer = -1;

    @Child private FrameSlotWriteNode writeNode;

    public static FrameStackPushNode create() {
        return new FrameStackPushNode();
    }

    public void execute(final VirtualFrame frame, final Object value) {
        if (stackPointerSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackPointerSlot = FrameAccess.getStackPointerSlot(frame);
            stackPointer = FrameAccess.getStackPointer(frame, stackPointerSlot) + 1;
            assert stackPointer <= CONTEXT.MAX_STACK_SIZE : "Bad stack pointer";
            writeNode = insert(FrameSlotWriteNode.create(FrameAccess.getStackSlotSlow(frame, stackPointer - 1)));
        }
        FrameAccess.setStackPointer(frame, stackPointerSlot, stackPointer);
        writeNode.executeWrite(frame, value);
    }
}
