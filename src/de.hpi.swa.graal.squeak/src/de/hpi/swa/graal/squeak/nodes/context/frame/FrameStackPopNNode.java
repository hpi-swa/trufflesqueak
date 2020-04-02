/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class FrameStackPopNNode extends AbstractNodeWithCode {

    protected FrameStackPopNNode(final CompiledCodeObject code) {
        super(code);
    }

    public static FrameStackPopNNode create(final CompiledCodeObject code, final int numPop) {
        if (numPop == 0) {
            return new FrameStackPop0Node(code);
        } else if (numPop == 1) {
            return new FrameStackPop1Node(code);
        } else {
            return new FrameStackPopMultipleNode(code, numPop);
        }
    }

    public abstract Object[] execute(VirtualFrame frame);

    private static final class FrameStackPop0Node extends FrameStackPopNNode {

        private FrameStackPop0Node(final CompiledCodeObject code) {
            super(code);
        }

        @Override
        public Object[] execute(final VirtualFrame frame) {
            return ArrayUtils.EMPTY_ARRAY;
        }
    }

    private static final class FrameStackPop1Node extends FrameStackPopNNode {
        @CompilationFinal private int stackPointer = -1;
        @Child private FrameSlotReadNode readNode;

        private FrameStackPop1Node(final CompiledCodeObject code) {
            super(code);
        }

        @Override
        public Object[] execute(final VirtualFrame frame) {
            if (stackPointer == -1) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointer = FrameAccess.getStackPointer(frame, code) - 1;
                assert stackPointer >= 0 : "Bad stack pointer";
                readNode = insert(FrameSlotReadNode.create(code, stackPointer));
            }
            FrameAccess.setStackPointer(frame, code, stackPointer);
            return new Object[]{readNode.executeRead(frame)};
        }
    }

    private static final class FrameStackPopMultipleNode extends FrameStackPopNNode {
        @CompilationFinal private int stackPointer = -1;
        @Children private FrameSlotReadNode[] readNodes;

        private FrameStackPopMultipleNode(final CompiledCodeObject code, final int numPop) {
            super(code);
            readNodes = new FrameSlotReadNode[numPop];
        }

        @Override
        @ExplodeLoop
        public Object[] execute(final VirtualFrame frame) {
            if (stackPointer == -1) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointer = FrameAccess.getStackPointer(frame, code) - readNodes.length;
                assert stackPointer >= 0 : "Bad stack pointer";
                for (int i = 0; i < readNodes.length; i++) {
                    readNodes[i] = insert(FrameSlotReadNode.create(code, stackPointer + i));
                }
            }
            FrameAccess.setStackPointer(frame, code, stackPointer);
            final Object[] result = new Object[readNodes.length];
            for (int i = 0; i < readNodes.length; i++) {
                result[i] = readNodes[i].executeRead(frame);
            }
            return result;
        }
    }
}
