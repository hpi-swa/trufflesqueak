/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
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

    public static final class ArgumentOnStackNode extends AbstractArgumentNode {
        private final int argumentIndex;

        @Child private FrameStackReadNode readNode;

        public ArgumentOnStackNode(final int argumentIndex) {
            this.argumentIndex = argumentIndex;
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                final int stackPointer = FrameAccess.getStackPointer(frame);
                readNode = insert(FrameStackReadNode.create(frame, stackPointer + argumentIndex, false));
            }
            return readNode.executeRead(frame);
        }
    }

    public static final class ArgumentNode extends AbstractArgumentNode {
        private final int argumentIndex;

        public ArgumentNode(final int argumentIndex) {
            // argumentIndex == 0 returns receiver
            this.argumentIndex = FrameAccess.getReceiverStartIndex() + argumentIndex;
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return frame.getArguments()[argumentIndex];
        }
    }
}
