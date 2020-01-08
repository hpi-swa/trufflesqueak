/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.Frame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class FrameStackTopNode extends AbstractNodeWithCode {
    @Child private FrameSlotReadNode readNode;

    protected FrameStackTopNode(final CompiledCodeObject code) {
        super(code);
    }

    public static FrameStackTopNode create(final CompiledCodeObject code) {
        return new FrameStackTopNode(code);
    }

    public Object execute(final Frame frame) {
        if (readNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final int stackPointer = FrameAccess.getStackPointer(frame, code) - 1;
            readNode = FrameSlotReadNode.create(code.getStackSlot(stackPointer));
        }
        return readNode.executeRead(frame);
    }
}
