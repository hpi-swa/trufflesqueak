package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class ReturnTopFromBlockNode extends ReturnNode {
    public ReturnTopFromBlockNode(CompiledMethodObject cm, int idx) {
        super(cm, idx);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        valueNode = stack.pop();
        super.interpretOn(stack, statements);
    }
}
