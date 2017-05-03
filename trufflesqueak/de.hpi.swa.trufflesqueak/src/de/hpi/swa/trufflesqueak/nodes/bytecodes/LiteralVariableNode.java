package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNodeGen;

public class LiteralVariableNode extends SqueakBytecodeNode {
    @Child SqueakNode valueNode;

    public LiteralVariableNode(CompiledMethodObject cm, int idx, int literalIndex) {
        super(cm, idx);
        valueNode = ObjectAtNodeGen.create(idx, new LiteralConstantNode(cm, idx, literalIndex));
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
