package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.AbstractStackNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopStackNode;

public class PopIntoReceiverVariableNode extends AbstractStoreIntoReceiverVariableNode {

    public PopIntoReceiverVariableNode(CompiledCodeObject code, int index, int numBytecodes, int receiverIndex) {
        super(code, index, numBytecodes, receiverIndex);
    }

    @Override
    protected AbstractStackNode getValueNode() {
        return new PopStackNode(code);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        storeNode.executeWrite(frame);
    }

    @Override
    public String toString() {
        return "popIntoRcvr: " + receiverIndex;
    }
}
