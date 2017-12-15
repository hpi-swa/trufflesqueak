package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class PopIntoReceiverVariableNode extends StoreIntoReceiverVariableNode {

    public PopIntoReceiverVariableNode(CompiledCodeObject code, int index, int numBytecodes, int receiverIndex) {
        super(code, index, numBytecodes, receiverIndex);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return storeNode.executeWrite(frame, pop(frame));
    }

    @Override
    public String toString() {
        return "popIntoRcvr: " + receiverIndex;
    }
}
