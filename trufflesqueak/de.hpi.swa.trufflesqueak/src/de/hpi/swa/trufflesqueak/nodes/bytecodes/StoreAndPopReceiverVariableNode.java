package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.WriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNode;

public class StoreAndPopReceiverVariableNode extends SqueakBytecodeNode {
    @Child WriteNode storeNode;

    public StoreAndPopReceiverVariableNode(CompiledCodeObject code, int index, int numBytecodes, int receiverIndex) {
        super(code, index, numBytecodes);
        storeNode = ObjectAtPutNode.create(receiverIndex, new FrameReceiverNode(code));
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return storeNode.executeWrite(frame, pop(frame));
    }
}
