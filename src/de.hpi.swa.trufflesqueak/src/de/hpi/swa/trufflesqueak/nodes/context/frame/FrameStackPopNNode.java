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

    @NodeInfo(cost = NodeCost.NONE)
    private static final class FrameStackPop1Node extends FrameStackPopNNode {
        @CompilationFinal private FrameSlot stackPointerSlot;
        @CompilationFinal private int stackPointer;
        @Child private FrameSlotReadNode readNode;

        @Override
        public Object[] execute(final VirtualFrame frame) {
            if (stackPointerSlot == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointerSlot = FrameAccess.getStackPointerSlot(frame);
                stackPointer = FrameAccess.getStackPointer(frame, stackPointerSlot) - 1;
                assert stackPointer >= 0 : "Bad stack pointer";
                readNode = insert(FrameSlotReadNode.create(frame, stackPointer));
            }
            FrameAccess.setStackPointer(frame, stackPointerSlot, stackPointer);
            return new Object[]{readNode.executeRead(frame)};
        }
    }

    private static final class FrameStackPopMultipleNode extends FrameStackPopNNode {
        @CompilationFinal private FrameSlot stackPointerSlot;
        @CompilationFinal private int stackPointer;
        @Children private FrameSlotReadNode[] readNodes;

        private FrameStackPopMultipleNode(final int numPop) {
            readNodes = new FrameSlotReadNode[numPop];
        }

        @Override
        @ExplodeLoop
        public Object[] execute(final VirtualFrame frame) {
            if (stackPointerSlot == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointerSlot = FrameAccess.getStackPointerSlot(frame);
                stackPointer = FrameAccess.getStackPointer(frame, stackPointerSlot) - readNodes.length;
                assert stackPointer >= 0 : "Bad stack pointer";
                for (int i = 0; i < readNodes.length; i++) {
                    readNodes[i] = insert(FrameSlotReadNode.create(frame, stackPointer + i));
                }
            }
            FrameAccess.setStackPointer(frame, stackPointerSlot, stackPointer);
            final Object[] result = new Object[readNodes.length];
            for (int i = 0; i < readNodes.length; i++) {
                result[i] = readNodes[i].executeRead(frame);
            }
            return result;
        }
    }
}
