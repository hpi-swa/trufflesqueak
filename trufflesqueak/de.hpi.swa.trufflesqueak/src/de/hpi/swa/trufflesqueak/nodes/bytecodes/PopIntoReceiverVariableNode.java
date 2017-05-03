package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNodeGen;

public class PopIntoReceiverVariableNode extends SqueakBytecodeNode {
    @Child SqueakNode storeNode;

    final int receiverIndex;

    public PopIntoReceiverVariableNode(CompiledMethodObject cm, int idx, int receiverIdx) {
        super(cm, idx);
        receiverIndex = receiverIdx;
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        storeNode = ObjectAtPutNodeGen.create(method, receiverIndex, FrameSlotReadNode.receiver(method), stack.pop());
        statements.push(this);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return storeNode.executeGeneric(frame);
    }
}
