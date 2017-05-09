package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNodeGen;

public class LiteralVariableNode extends SqueakBytecodeNode {
    @Child SqueakNode valueNode;

    public LiteralVariableNode(CompiledCodeObject method, int idx, int literalIndex) {
        super(method, idx);
        valueNode = ObjectAtNodeGen.create(1, new LiteralConstantNode(method, idx, literalIndex));
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        stack.add(this);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return valueNode.executeGeneric(frame);
    }
}
