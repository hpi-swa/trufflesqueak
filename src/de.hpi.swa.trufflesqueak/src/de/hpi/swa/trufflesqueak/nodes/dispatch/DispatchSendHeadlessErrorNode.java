/*
 * Copyright (c) 2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

final class DispatchSendHeadlessErrorNode extends AbstractDispatchExceptionalNode {
    @Child private FrameStackReadNode receiverNode;

    DispatchSendHeadlessErrorNode(final VirtualFrame frame, final NativeObject selector, final int numArgs) {
        super(selector, numArgs);
        final int stackPointer = FrameAccess.getStackPointer(frame);
        final int receiverIndex = stackPointer - 1 - numArgs;
        receiverNode = FrameStackReadNode.create(frame, receiverIndex, false);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        final Object receiver = receiverNode.executeRead(frame);
        CompilerDirectives.transferToInterpreter();
        final ClassObject receiverClass = SqueakObjectClassNode.executeUncached(receiver);
        throw new SqueakException(MiscUtils.format("%s>>#%s detected in headless mode. Aborting...", receiverClass.getSqueakClassName(), selector.asStringUnsafe()), this);
    }
}
