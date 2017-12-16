package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;

public class FrameReceiverNode extends SqueakNodeWithCode {
    private @Child ReceiverNode receiverNode = new ReceiverNode();

    public FrameReceiverNode(CompiledCodeObject code) {
        super(code);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return receiverNode.execute(frame);
    }
}
