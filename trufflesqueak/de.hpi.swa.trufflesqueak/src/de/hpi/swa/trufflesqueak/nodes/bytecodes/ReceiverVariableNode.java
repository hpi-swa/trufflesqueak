package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNodeGen;

public class ReceiverVariableNode extends SqueakBytecodeNode {
    @Child SqueakNode fetchNode;

    public ReceiverVariableNode(CompiledCodeObject method, int idx, int i) {
        super(method, idx);
        fetchNode = ObjectAtNodeGen.create(i, new ReceiverNode(method, idx));
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        stack.add(this);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return fetchNode.executeGeneric(frame);
    }
}
