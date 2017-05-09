package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class ExtendedPushNode extends ExtendedAccess {
    public ExtendedPushNode(CompiledCodeObject method, int idx, int i) {
        super(method, idx, i);
    }

    @Override
    public SqueakNode createActualNode(int idx, int type, Stack<SqueakNode> stack) {
        switch (type) {
            case 0:
                return new ReceiverNode(method, index);
            case 1:
                return new TemporaryVariableNode(method, index, idx);
            case 2:
                return new LiteralConstantNode(method, index, idx);
            case 3:
                return new LiteralVariableNode(method, index, idx);
            default:
                throw new RuntimeException("unexpected type for ExtendedPush");
        }
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        super.interpretOn(stack, statements);
        stack.add(this);
    }
}
