/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
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
                return ArgumentNotProvidedNode.SINGLETON;
            }
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class ArgumentOnStackNode extends AbstractArgumentNode {
        private final int argumentIndex;
        @CompilationFinal private int stackPointer;
        @CompilationFinal private FrameSlot stackSlot;

        public ArgumentOnStackNode(final int argumentIndex) {
            this.argumentIndex = argumentIndex; // argumentIndex == 0 returns receiver
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            if (stackSlot == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointer = FrameAccess.getStackPointerSlow(frame);
                stackSlot = FrameAccess.getStackSlot(frame);
            }
            return FrameAccess.getStackAt(frame, stackSlot, stackPointer + argumentIndex);
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

    @NodeInfo(cost = NodeCost.NONE)
    private static final class ArgumentNotProvidedNode extends AbstractArgumentNode {
        private static final ArgumentNotProvidedNode SINGLETON = new ArgumentNotProvidedNode();

        @Override
        public NotProvided execute(final VirtualFrame frame) {
            return NotProvided.SINGLETON;
        }

        @Override
        public boolean isAdoptable() {
            return false; /* Allow sharing. */
        }
    }
}
