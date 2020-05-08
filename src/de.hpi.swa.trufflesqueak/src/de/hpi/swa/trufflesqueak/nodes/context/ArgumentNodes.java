/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.NotProvided;

public final class ArgumentNodes {
    public abstract static class AbstractArgumentNode extends AbstractNode {
        public abstract Object execute(VirtualFrame frame);

        public static final AbstractArgumentNode create(final int argumentIndex, final int numArguments, final boolean useStack) {
            if (argumentIndex <= numArguments) {
                if (useStack) {
                    return new ArgumentOnStackNode(argumentIndex);
                } else {
                    return new ArgumentNode(argumentIndex);
                }
            } else {
                return new ArgumentNotProvidedNode();
            }
        }
    }

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
                final CompiledCodeObject blockOrMethod = FrameAccess.getBlockOrMethod(frame);
                final int stackPointer = FrameAccess.getStackPointer(frame, blockOrMethod);
                readNode = insert(FrameSlotReadNode.create(blockOrMethod.getStackSlot(stackPointer + argumentIndex)));
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

    private static final class ArgumentNotProvidedNode extends AbstractArgumentNode {
        @Override
        public NotProvided execute(final VirtualFrame frame) {
            return NotProvided.SINGLETON;
        }
    }
}
