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
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class FrameStackPopNode extends AbstractNodeWithCode {
    @CompilationFinal private int stackPointer = -1;

    @Child private FrameSlotReadNode readNode;

    protected FrameStackPopNode(final CompiledCodeObject code) {
        super(code);
    }

    public static FrameStackPopNode create(final CompiledCodeObject code) {
        return new FrameStackPopNode(code);
    }

    public Object execute(final VirtualFrame frame) {
        if (readNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackPointer = FrameAccess.getStackPointer(frame, code) - 1;
            readNode = insert(FrameSlotReadNode.create(code, stackPointer));
        }
        FrameAccess.setStackPointer(frame, code, stackPointer);
        return readNode.executeRead(frame);
    }
}
