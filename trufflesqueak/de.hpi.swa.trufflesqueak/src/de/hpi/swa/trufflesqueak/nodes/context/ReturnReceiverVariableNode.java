package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;

public class ReturnReceiverVariableNode extends SqueakBytecodeNode {
    @Child private ObjectAtNode fetchNode;
    @Child private ReceiverNode receiverNode = new ReceiverNode();

    public ReturnReceiverVariableNode(CompiledCodeObject code, int index, int varIndex) {
        super(code, index);
        fetchNode = ObjectAtNode.create(varIndex);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return fetchNode.executeWith(receiverNode.execute(frame));
    }
}
