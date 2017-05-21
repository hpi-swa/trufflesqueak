package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;

public class LiteralConstantNode extends SqueakBytecodeNode {
    @Child SqueakNode literalNode;

    public LiteralConstantNode(CompiledCodeObject method, int idx, int literalIdx) {
        super(method, idx);
        literalNode = new MethodLiteralNode(method, literalIdx);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        stack.add(this);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return literalNode.executeGeneric(frame);
    }
}
