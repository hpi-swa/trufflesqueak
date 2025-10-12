/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

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

    public abstract int numPop();

    private static final class FrameStackPop0Node extends FrameStackPopNNode {
        @Override
        public Object[] execute(final VirtualFrame frame) {
            return ArrayUtils.EMPTY_ARRAY;
        }

        @Override
        public int numPop() {
            return 0;
        }
    }

    private static final class FrameStackPop1Node extends FrameStackPopNNode {
        @CompilationFinal private int stackPointer = -1;
        @Child private FrameStackReadNode readNode;

        @Override
        public Object[] execute(final VirtualFrame frame) {
            if (stackPointer == -1) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointer = FrameAccess.getStackPointer(frame) - 1;
                assert stackPointer >= 0 : "Bad stack pointer";
                readNode = insert(FrameStackReadNode.create(frame, stackPointer, true));
            }
            FrameAccess.setStackPointer(frame, stackPointer);
            return new Object[]{readNode.executeRead(frame)};
        }

        @Override
        public int numPop() {
            return 1;
        }
    }

    private static final class FrameStackPopMultipleNode extends FrameStackPopNNode {
        @CompilationFinal private int stackPointer = -1;
        @Children private FrameStackReadNode[] readNodes;

        private FrameStackPopMultipleNode(final int numPop) {
            readNodes = new FrameStackReadNode[numPop];
        }

        @Override
        @ExplodeLoop
        public Object[] execute(final VirtualFrame frame) {
            final int numPop = numPop();
            if (stackPointer == -1) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointer = FrameAccess.getStackPointer(frame) - numPop;
                assert stackPointer >= 0 : "Bad stack pointer";
                for (int i = 0; i < numPop; i++) {
                    readNodes[i] = insert(FrameStackReadNode.create(frame, stackPointer + i, true));
                }
            }
            FrameAccess.setStackPointer(frame, stackPointer);
            final Object[] result = new Object[numPop];
            for (int i = 0; i < numPop; i++) {
                result[i] = readNodes[i].executeRead(frame);
            }
            return result;
        }

        @Override
        public int numPop() {
            return readNodes.length;
        }
    }
}
