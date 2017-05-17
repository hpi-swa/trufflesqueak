package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.instrumentation.PrettyPrintVisitor;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public abstract class ReturnNode extends SqueakBytecodeNode {
    @Child SqueakNode valueNode;

    public ReturnNode(CompiledCodeObject method, int idx) {
        super(method, idx);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        statements.push(this);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new LocalReturn(valueNode.executeGeneric(frame));
    }

    @Override
    public void prettyPrintOn(PrettyPrintVisitor b) {
        b.append("^ ");
        b.visit(valueNode);
    }

    @Override
    public boolean isReturn() {
        return true;
    }
}
