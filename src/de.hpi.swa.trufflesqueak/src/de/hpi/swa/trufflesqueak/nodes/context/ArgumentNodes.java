/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class ArgumentNodes {
    public abstract static class AbstractArgumentNode extends AbstractNode {
        public abstract Object execute(VirtualFrame frame);

        public static final AbstractArgumentNode create(final int argumentIndex, final boolean useStack) {
            if (useStack) {
                return new ArgumentOnStackNode(argumentIndex);
            } else {
                return new ArgumentNode(argumentIndex);
            }
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class ArgumentOnStackNode extends AbstractArgumentNode {
        private final int argumentIndex;

        @Child private FrameSlotReadNode readNode;

        public ArgumentOnStackNode(final int argumentIndex) {
            this.argumentIndex = argumentIndex; // argumentIndex == 0 returns receiver
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                final CompiledCodeObject code = FrameAccess.getMethodOrBlock(frame);
                final int stackPointer = FrameAccess.getStackPointer(frame, code);
                readNode = insert(FrameSlotReadNode.create(code.getStackSlot(stackPointer + argumentIndex)));
            }
            return readNode.executeRead(frame);
        }
    }

    public static final class ArgumentNode extends AbstractArgumentNode {
        private final int argumentIndex;

        public ArgumentNode(final int argumentIndex) {
            this.argumentIndex = argumentIndex; // argumentIndex == 0 returns receiver
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return FrameAccess.getArgument(frame, argumentIndex);
        }
    }
}
