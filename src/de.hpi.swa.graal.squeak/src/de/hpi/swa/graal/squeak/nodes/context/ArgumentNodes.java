/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.NotProvided;

public final class ArgumentNodes {
    public abstract static class AbstractArgumentNode extends AbstractNode {
        public abstract Object execute(VirtualFrame frame);

        public static final AbstractArgumentNode create(final int argumentIndex, final int numArguments) {
            if (argumentIndex <= numArguments) {
                return new ArgumentNode(argumentIndex);
            } else {
                return new ArgumentNotProvidedNode();
            }
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
