package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.WriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNode;

public class StoreIntoReceiverVariableNode extends SqueakBytecodeNode {
    @Child WriteNode storeNode;
    protected final int receiverIndex;

    public StoreIntoReceiverVariableNode(CompiledCodeObject code, int index, int numBytecodes, int receiverIndex) {
        super(code, index, numBytecodes);
        this.receiverIndex = receiverIndex;
        storeNode = ObjectAtPutNode.create(receiverIndex, new FrameReceiverNode(code));
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return storeNode.executeWrite(frame, top(frame));
    }

    @Override
    public String toString() {
        return "storeIntoRcvr: " + receiverIndex;
    }
}
