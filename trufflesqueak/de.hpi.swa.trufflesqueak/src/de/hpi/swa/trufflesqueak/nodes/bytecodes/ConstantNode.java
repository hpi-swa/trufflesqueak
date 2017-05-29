package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.instrumentation.PrettyPrintVisitor;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class ConstantNode extends SqueakBytecodeNode {
    public final Object constant;

    public ConstantNode(CompiledCodeObject method, int idx, Object obj) {
        super(method, idx);
        constant = obj;
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        stack.add(this);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return constant;
    }

    @Override
    public void accept(PrettyPrintVisitor b) {
        b.visit(this);
    }
}
