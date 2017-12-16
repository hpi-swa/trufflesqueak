package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopStackNode;

public class PopIntoReceiverVariableNode extends AbstractStoreIntoReceiverVariableNode {
    @Child private PopStackNode popNode;

    public PopIntoReceiverVariableNode(CompiledCodeObject code, int index, int numBytecodes, int receiverIndex) {
        super(code, index, numBytecodes, receiverIndex);
        popNode = new PopStackNode(code);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        storeNode.executeWrite(frame, popNode.execute(frame));
    }

    @Override
    public String toString() {
        return "popIntoRcvr: " + receiverIndex;
    }
}
