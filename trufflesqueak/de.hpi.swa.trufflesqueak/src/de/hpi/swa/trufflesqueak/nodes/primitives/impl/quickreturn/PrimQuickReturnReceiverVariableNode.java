package de.hpi.swa.trufflesqueak.nodes.primitives.impl.quickreturn;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;

public class PrimQuickReturnReceiverVariableNode extends PrimitiveNode {
    private @Child FrameReceiverVariableNode receiverVariableNode;

    public PrimQuickReturnReceiverVariableNode(CompiledMethodObject code, int variableIndex) {
        super(code);
        receiverVariableNode = new FrameReceiverVariableNode(code, variableIndex);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return receiverVariableNode.executeGeneric(frame);
    }

}
