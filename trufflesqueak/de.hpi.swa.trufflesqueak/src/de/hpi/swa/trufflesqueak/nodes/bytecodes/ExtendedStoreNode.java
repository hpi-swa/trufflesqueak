package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNodeGen;

public class ExtendedStoreNode extends ExtendedAccess {
    public ExtendedStoreNode(CompiledCodeObject method, int index, int i) {
        super(method, index, i);
    }

    @Override
    public SqueakNode createActualNode(int idx, int type, Stack<SqueakNode> stack) {
        SqueakNode top = stack.pop();
        switch (type) {
            case 0:
                return ObjectAtPutNodeGen.create(method, idx, FrameSlotReadNode.receiver(method), top);
            case 1:
                return FrameSlotWriteNode.temp(method, idx, top);
            case 2:
                throw new RuntimeException("illegal ExtendedStore bytecode: variable type 2");
            case 3:
                return ObjectAtPutNodeGen.create(method, 1, new MethodLiteralNode(method, idx), top);
            default:
                throw new RuntimeException("illegal ExtendedStore bytecode");
        }
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        super.interpretOn(stack, statements);
        stack.add(this);
    }
}
