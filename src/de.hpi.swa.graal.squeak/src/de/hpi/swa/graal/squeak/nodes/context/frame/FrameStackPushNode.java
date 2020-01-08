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
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class FrameStackPushNode extends AbstractNodeWithCode {
    @CompilationFinal private int stackPointer = -1;

    @Child private FrameSlotWriteNode writeNode;

    protected FrameStackPushNode(final CompiledCodeObject code) {
        super(code);
    }

    public static FrameStackPushNode create(final CompiledCodeObject code) {
        return new FrameStackPushNode(code);
    }

    public void execute(final VirtualFrame frame, final Object value) {
        if (writeNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackPointer = FrameAccess.getStackPointer(frame, code) + 1;
            assert stackPointer <= CONTEXT.MAX_STACK_SIZE : "Bad stack pointer";
            writeNode = insert(FrameSlotWriteNode.create(code.getStackSlot(stackPointer - 1)));
        }
        FrameAccess.setStackPointer(frame, code, stackPointer);
        writeNode.executeWrite(frame, value);
    }
}
