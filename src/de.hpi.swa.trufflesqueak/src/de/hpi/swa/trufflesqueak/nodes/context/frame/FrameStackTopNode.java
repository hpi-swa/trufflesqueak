/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class FrameStackTopNode extends AbstractNode {
    @Child private FrameStackReadNode readNode;

    public static FrameStackTopNode create() {
        return new FrameStackTopNode();
    }

    public Object execute(final VirtualFrame frame) {
        if (readNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final int stackPointer = FrameAccess.getStackPointer(frame) - 1;
            readNode = FrameStackReadNode.create(frame, stackPointer, false);
        }
        return readNode.executeRead(frame);
    }
}
