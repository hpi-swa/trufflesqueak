/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.Frame;

import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class FrameStackTopNode extends AbstractNode {
    @Child private FrameSlotReadNode readNode;

    public static FrameStackTopNode create() {
        return new FrameStackTopNode();
    }

    public Object execute(final Frame frame) {
        if (readNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final int stackPointer = FrameAccess.getStackPointerSlow(frame) - 1;
            readNode = FrameSlotReadNode.create(FrameAccess.getStackSlot(frame, stackPointer));
        }
        return readNode.executeRead(frame);
    }
}
