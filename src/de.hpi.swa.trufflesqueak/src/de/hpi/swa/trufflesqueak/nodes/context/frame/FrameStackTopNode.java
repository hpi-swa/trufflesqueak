/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@NodeInfo(cost = NodeCost.NONE)
public final class FrameStackTopNode extends AbstractNode {
    @Child private FrameSlotReadNode readNode;

    public static FrameStackTopNode create() {
        return new FrameStackTopNode();
    }

    public Object execute(final Frame frame) {
        if (readNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final int stackPointer = FrameAccess.getStackPointerSlow(frame) - 1;
            readNode = FrameSlotReadNode.create(FrameAccess.getStackSlotSlow(frame, stackPointer));
        }
        return readNode.executeRead(frame);
    }
}
