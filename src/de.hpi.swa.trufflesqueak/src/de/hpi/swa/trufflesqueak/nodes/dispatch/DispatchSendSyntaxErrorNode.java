/*
 * Copyright (c) 2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakSyntaxError;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

final class DispatchSendSyntaxErrorNode extends AbstractDispatchExceptionalNode {
    @Child private FrameStackReadNode arg1Node;

    DispatchSendSyntaxErrorNode(final VirtualFrame frame, final NativeObject selector, final int numArgs) {
        super(selector, numArgs);
        final int stackPointer = FrameAccess.getStackPointer(frame);
        final int receiverIndex = stackPointer - 1 - numArgs + 1;
        arg1Node = FrameStackReadNode.create(frame, receiverIndex, true);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();
        throw new SqueakSyntaxError((PointersObject) arg1Node.executeRead(frame));
    }
}
