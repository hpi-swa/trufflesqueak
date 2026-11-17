/*
 * Copyright (c) 2025-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class PushToStackNode extends AbstractNode {
    private static final int GENERIC = -2;

    @CompilationFinal private int stackPointer = -1;

    @NeverDefault
    public static PushToStackNode create() {
        return new PushToStackNode();
    }

    public void execute(final VirtualFrame frame, final Object value) {
        final int currentStackPointer = FrameAccess.getStackPointer(frame);
        if (stackPointer == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackPointer = currentStackPointer + 1;
            assert stackPointer <= FrameAccess.getCodeObject(frame).getMaxStackSize() : "Bad stack pointer";
        }
        if (stackPointer == currentStackPointer + 1) {
            FrameAccess.setStackPointer(frame, stackPointer);
            FrameAccess.setStackValue(frame, stackPointer - 1, value);
        } else {
            /*
             * This node is shared between senders with different stack depths (e.g., primitive
             * nodes used by indirect dispatch or by the uncached Bytecode DSL interpreter).
             */
            if (stackPointer != GENERIC) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointer = GENERIC;
            }
            pushGeneric(frame.materialize(), currentStackPointer, value);
        }
    }

    @TruffleBoundary
    private static void pushGeneric(final MaterializedFrame frame, final int currentStackPointer, final Object value) {
        FrameAccess.setStackPointer(frame, currentStackPointer + 1);
        FrameAccess.setStackValue(frame, currentStackPointer, value);
    }
}
