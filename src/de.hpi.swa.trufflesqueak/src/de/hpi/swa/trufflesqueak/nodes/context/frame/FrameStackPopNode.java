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

import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@NodeInfo(cost = NodeCost.NONE)
public final class FrameStackPopNode extends AbstractNode {
    @CompilationFinal private FrameSlot stackPointerSlot;
    @CompilationFinal private int stackPointer;
    @CompilationFinal private FrameSlot stackSlot;

    public static FrameStackPopNode create() {
        return new FrameStackPopNode();
    }

    public Object execute(final VirtualFrame frame) {
        if (stackPointerSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackPointerSlot = FrameAccess.getStackPointerSlot(frame);
            stackPointer = FrameAccess.getStackPointer(frame, stackPointerSlot) - 1;
            stackSlot = FrameAccess.getStackSlot(frame);
        }
        FrameAccess.setStackPointer(frame, stackPointerSlot, stackPointer);
        return FrameAccess.getStackAt(frame, stackSlot, stackPointer);
    }
}
