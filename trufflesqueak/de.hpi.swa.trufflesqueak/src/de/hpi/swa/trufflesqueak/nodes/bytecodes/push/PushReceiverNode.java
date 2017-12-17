package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameReceiverNode;

public class PushReceiverNode extends AbstractPushNode {
    @Child private FrameReceiverNode receiverNode;

    public PushReceiverNode(CompiledCodeObject code, int index) {
        super(code, index);
        receiverNode = new FrameReceiverNode(code);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        pushNode.executeWrite(frame, receiverNode.executeGeneric(frame));
    }

    @Override
    public String toString() {
        return "self";
    }
}
