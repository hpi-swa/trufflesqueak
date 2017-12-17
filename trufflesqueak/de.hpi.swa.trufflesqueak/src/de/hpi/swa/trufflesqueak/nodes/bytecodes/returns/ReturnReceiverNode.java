package de.hpi.swa.trufflesqueak.nodes.bytecodes.returns;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameReceiverNode;

public class ReturnReceiverNode extends ReturnNode {
    @Child private FrameReceiverNode receiverNode;

    public ReturnReceiverNode(CompiledCodeObject code, int index) {
        super(code, index);
        receiverNode = new FrameReceiverNode(code);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        throw new LocalReturn(receiverNode.executeGeneric(frame));
    }

    @Override
    public String toString() {
        return "returnSelf";
    }
}
