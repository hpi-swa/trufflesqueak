/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.CreateFrameArgumentNodes.UpdateStackPointerNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class AbstractDispatchNode extends AbstractNode {
    protected final NativeObject selector;
    protected final int argumentCount;

    @Children private FrameSlotReadNode[] receiverAndArgumentsNodes;
    @Child private UpdateStackPointerNode updateStackPointerNode = UpdateStackPointerNode.create();

    public AbstractDispatchNode(final NativeObject selector, final int argumentCount) {
        this.selector = selector;
        this.argumentCount = argumentCount;
    }

    public final NativeObject getSelector() {
        return selector;
    }

    public final int getArgumentCount() {
        return argumentCount;
    }

    protected final FrameSlotReadNode[] getReceiverAndArgumentsNodes(final VirtualFrame frame) {
        if (receiverAndArgumentsNodes == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            receiverAndArgumentsNodes = createReceiverAndArgumentsNodes(frame, argumentCount);
        }
        return receiverAndArgumentsNodes;
    }

    public static FrameSlotReadNode[] createReceiverAndArgumentsNodes(final VirtualFrame frame, final int argumentCount) {
        final FrameSlotReadNode[] receiverAndArgumentsNodes = new FrameSlotReadNode[1 + argumentCount];
        final int newStackPointer = FrameAccess.getStackPointerSlow(frame) - (1 + argumentCount);
        assert newStackPointer >= 0 : "Bad stack pointer";
        for (int i = 0; i < receiverAndArgumentsNodes.length; i++) {
            receiverAndArgumentsNodes[i] = FrameSlotReadNode.create(frame, newStackPointer + i);
        }
        return receiverAndArgumentsNodes;
    }

    protected final UpdateStackPointerNode getUpdateStackPointerNode() {
        return updateStackPointerNode;
    }
}
