/*
 * Copyright (c) 2025-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class PushToStackNode extends AbstractNode {
    @CompilationFinal private int stackPointer = -1;
    @CompilationFinal private int stackSlot = -1;

    @NeverDefault
    public static PushToStackNode create() {
        return new PushToStackNode();
    }

    public void execute(final VirtualFrame frame, final Object value) {
        if (stackPointer == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackPointer = FrameAccess.getStackPointer(frame) + 1;
            assert stackPointer <= CONTEXT.MAX_STACK_SIZE : "Bad stack pointer";
            stackSlot = FrameAccess.toStackSlotIndex(frame, stackPointer - 1);
        }
        FrameAccess.setStackPointer(frame, stackPointer);
        FrameAccess.setSlotValue(frame, stackSlot, value);
    }
}
