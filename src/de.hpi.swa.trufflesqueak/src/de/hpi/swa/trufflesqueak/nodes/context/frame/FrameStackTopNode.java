/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@NodeInfo(cost = NodeCost.NONE)
public final class FrameStackTopNode extends AbstractNode {
    @CompilationFinal private int stackPointer;
    @CompilationFinal private FrameSlot stackSlot;

    public static FrameStackTopNode create() {
        return new FrameStackTopNode();
    }

    public Object execute(final Frame frame) {
        if (stackSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackPointer = FrameAccess.getStackPointerSlow(frame) - 1;
            stackSlot = FrameAccess.getStackSlot(frame);
        }
        return FrameAccess.getStackAt(frame, stackSlot, stackPointer);
    }
}
