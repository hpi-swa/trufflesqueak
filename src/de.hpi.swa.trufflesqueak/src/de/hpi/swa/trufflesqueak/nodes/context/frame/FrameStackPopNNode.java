/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class FrameStackPopNNode extends AbstractNode {

    public static FrameStackPopNNode create(final int numPop) {
        if (numPop == 0) {
            return new FrameStackPop0Node();
        } else if (numPop == 1) {
            return new FrameStackPop1Node();
        } else {
            return new FrameStackPopMultipleNode(numPop);
        }
    }

    public abstract Object[] execute(VirtualFrame frame);

    @NodeInfo(cost = NodeCost.NONE)
    private static final class FrameStackPop0Node extends FrameStackPopNNode {
        @Override
        public Object[] execute(final VirtualFrame frame) {
            return ArrayUtils.EMPTY_ARRAY;
        }
    }

    private abstract static class FrameStackPopNWithFieldsNode extends FrameStackPopNNode {
        @CompilationFinal protected FrameSlot stackPointerSlot;
        @CompilationFinal protected int stackPointer;
        @CompilationFinal protected FrameSlot stackSlot;

        protected final void ensureInitialized(final VirtualFrame frame, final int numPop) {
            if (stackPointerSlot == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointerSlot = FrameAccess.getStackPointerSlot(frame);
                stackPointer = FrameAccess.getStackPointer(frame, stackPointerSlot) - numPop;
                assert stackPointer >= 0 : "Bad stack pointer";
                stackSlot = FrameAccess.getStackSlot(frame);
            }
        }

    }

    @NodeInfo(cost = NodeCost.NONE)
    private static final class FrameStackPop1Node extends FrameStackPopNWithFieldsNode {
        @Override
        public Object[] execute(final VirtualFrame frame) {
            ensureInitialized(frame, 1);
            FrameAccess.setStackPointer(frame, stackPointerSlot, stackPointer);
            return new Object[]{FrameAccess.getStackAt(frame, stackSlot, stackPointer)};
        }
    }

    private static final class FrameStackPopMultipleNode extends FrameStackPopNWithFieldsNode {
        private final int numPop;

        private FrameStackPopMultipleNode(final int numPop) {
            this.numPop = numPop;
        }

        @Override
        public Object[] execute(final VirtualFrame frame) {
            ensureInitialized(frame, numPop);
            FrameAccess.setStackPointer(frame, stackPointerSlot, stackPointer);
            return Arrays.copyOfRange(FrameAccess.getStack(frame, stackSlot), stackPointer, stackPointer + numPop);
        }
    }
}
