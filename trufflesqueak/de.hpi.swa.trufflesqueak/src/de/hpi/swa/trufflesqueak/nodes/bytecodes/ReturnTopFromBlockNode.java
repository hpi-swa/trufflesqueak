package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import de.hpi.swa.trufflesqueak.instrumentation.PrettyPrintVisitor;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class ReturnTopFromBlockNode extends ReturnNode {
    public ReturnTopFromBlockNode(CompiledCodeObject method, int idx) {
        super(method, idx);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        valueNode = stack.pop();
        super.interpretOn(stack, statements);
    }

    @Override
    public void prettyPrintOn(PrettyPrintVisitor b) {
        b.visit(valueNode);
    }
}
