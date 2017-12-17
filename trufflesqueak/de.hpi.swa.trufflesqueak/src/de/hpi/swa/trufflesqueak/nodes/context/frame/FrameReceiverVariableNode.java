package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNode;

public class FrameReceiverVariableNode extends SqueakNode {
    @Child private ObjectAtNode fetchNode;
    @Child private FrameReceiverNode receiverNode;

    public FrameReceiverVariableNode(CompiledCodeObject code, int varIndex) {
        super();
        fetchNode = ObjectAtNode.create(varIndex);
        receiverNode = new FrameReceiverNode(code);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return fetchNode.executeWith(receiverNode.executeGeneric(frame));
    }
}
