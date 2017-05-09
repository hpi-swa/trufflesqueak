package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public class ReceiverNode extends SqueakBytecodeNode {
    @Child SqueakNode receiverNode;

    public ReceiverNode(CompiledCodeObject method, int idx) {
        super(method, idx);
        receiverNode = FrameSlotReadNode.receiver(method);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        stack.add(this);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return receiverNode.executeGeneric(frame);
    }

    @Override
    public void prettyPrintOn(StringBuilder b) {
        b.append("self");
    }
}
