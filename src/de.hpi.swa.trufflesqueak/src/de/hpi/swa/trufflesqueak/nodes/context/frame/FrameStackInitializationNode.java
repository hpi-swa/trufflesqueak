/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class FrameStackInitializationNode extends AbstractNode {
    @CompilationFinal private FrameSlot stackPointerSlot;
    @CompilationFinal private int initialSP;

    public static FrameStackInitializationNode create() {
        return new FrameStackInitializationNode();
    }

    @ExplodeLoop
    public void executeInitialize(final VirtualFrame frame) {
        if (stackPointerSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackPointerSlot = FrameAccess.getStackPointerSlot(frame);
            final BlockClosureObject closure = FrameAccess.getClosure(frame);
            if (closure == null) {
                initialSP = FrameAccess.getCodeObject(frame).getNumTemps();
            } else {
                initialSP = closure.getNumTemps();
            }
        }
        FrameAccess.setStackPointer(frame, stackPointerSlot, initialSP);
    }
}
