package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.ReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;

public class PushReceiverNode extends SqueakBytecodeNode {
    @Child private PushStackNode pushNode;
    @Child private ReceiverNode receiverNode;

    public PushReceiverNode(CompiledCodeObject code, int index) {
        super(code, index);
        pushNode = new PushStackNode(code);
        receiverNode = new ReceiverNode(code);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        pushNode.executeWrite(frame, receiverNode.execute(frame));
    }

    @Override
    public String toString() {
        return "self";
    }
}
