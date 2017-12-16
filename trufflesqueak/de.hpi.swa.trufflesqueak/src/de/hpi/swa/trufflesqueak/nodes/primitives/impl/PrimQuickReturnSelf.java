package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;

public class PrimQuickReturnSelf extends PrimitiveNode {
    @Child private ReceiverNode receiverNode = new ReceiverNode();

    public PrimQuickReturnSelf(CompiledMethodObject code) {
        super(code);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return receiverNode.execute(frame);
    }
}