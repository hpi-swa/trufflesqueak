/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class FrameStackInitializationNode extends AbstractNode {
    private final int initialSP;
    private final int numArgs;
    private final FrameSlot stackPointerSlot;
    private final FrameSlot stackSlot;
    private final int numStackSlot;

    public FrameStackInitializationNode(final CompiledCodeObject code) {
        if (code.isCompiledMethod()) {
            initialSP = code.getNumTemps();
        } else {
            initialSP = code.getNumArgsAndCopied();
        }
        numArgs = code.getNumArgsAndCopied();
        stackPointerSlot = code.getStackPointerSlot();
        stackSlot = code.getStackSlot();
        numStackSlot = code.getNumStackSlots();
    }

    public static FrameStackInitializationNode create(final CompiledCodeObject code) {
        return new FrameStackInitializationNode(code);
    }

    public void executeInitialize(final VirtualFrame frame) {
        final Object[] arguments = frame.getArguments();
        assert arguments.length == FrameAccess.expectedArgumentSize(numArgs);
        assert FrameAccess.getStack(frame, stackSlot) == null : "Should never overwrite stack";
        FrameAccess.setStackPointer(frame, stackPointerSlot, initialSP);
        final Object[] stack = new Object[numStackSlot];
        FrameAccess.setStack(frame, stackSlot, stack);
        System.arraycopy(arguments, FrameAccess.getArgumentStartIndex(), stack, 0, numArgs);
        // Initialize remaining temporary variables with nil in newContext.
        FrameAccess.clearStack(frame, stackSlot, numArgs, initialSP);
    }
}
