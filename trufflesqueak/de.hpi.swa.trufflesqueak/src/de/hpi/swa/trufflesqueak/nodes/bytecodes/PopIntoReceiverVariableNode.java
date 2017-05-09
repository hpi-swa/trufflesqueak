package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNodeGen;

public class PopIntoReceiverVariableNode extends SqueakBytecodeNode {
    @Child SqueakNode storeNode;

    final int receiverIndex;

    public PopIntoReceiverVariableNode(CompiledCodeObject method, int idx, int receiverIdx) {
        super(method, idx);
        receiverIndex = receiverIdx;
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        storeNode = ObjectAtPutNodeGen.create(method, receiverIndex, new ReceiverNode(method, index), stack.pop());
        statements.push(this);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return storeNode.executeGeneric(frame);
    }
}
