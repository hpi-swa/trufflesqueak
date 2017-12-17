package de.hpi.swa.trufflesqueak.nodes.primitives.impl.quickreturn;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;

public class PrimQuickReturnSelf extends PrimitiveNode {
    @Child private FrameReceiverNode receiverNode;

    public PrimQuickReturnSelf(CompiledMethodObject code) {
        super(code);
        receiverNode = new FrameReceiverNode(code);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return receiverNode.executeGeneric(frame);
    }
}