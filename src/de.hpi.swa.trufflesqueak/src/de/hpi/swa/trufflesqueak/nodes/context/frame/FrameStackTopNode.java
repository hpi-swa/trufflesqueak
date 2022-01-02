/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
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
    @Child private FrameStackReadNode readNode;

    public static FrameStackTopNode create() {
        return new FrameStackTopNode();
    }

    public Object execute(final Frame frame) {
        if (readNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final int stackPointer = FrameAccess.getStackPointer(frame) - 1;
            readNode = FrameStackReadNode.create(frame, stackPointer, false);
        }
        return readNode.executeRead(frame);
    }
}
