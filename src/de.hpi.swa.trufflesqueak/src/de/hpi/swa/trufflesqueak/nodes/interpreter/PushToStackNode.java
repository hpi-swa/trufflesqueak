/*
 * Copyright (c) 2025-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class PushToStackNode extends AbstractNode {
    @CompilationFinal private int stackPointer = -1;

    @NeverDefault
    public static PushToStackNode create() {
        return new PushToStackNode();
    }

    public void execute(final VirtualFrame frame, final Object value) {
        if (stackPointer == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackPointer = FrameAccess.getStackPointer(frame) + 1;
            assert stackPointer <= FrameAccess.getCodeObject(frame).getMaxStackSize() : "Bad stack pointer";
        }
        FrameAccess.setStackPointer(frame, stackPointer);
        FrameAccess.setStackValue(frame, stackPointer - 1, value);
    }
}
