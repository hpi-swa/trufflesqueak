package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public class TemporaryVariableNode extends SqueakBytecodeNode {
    @Child SqueakNode tempNode;

    public TemporaryVariableNode(CompiledMethodObject cm, int idx, int tempIndex) {
        super(cm, idx);
        tempNode = FrameSlotReadNode.create(cm, cm.stackSlots[tempIndex]);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        // TODO: Handle the case of chained LiteralVariableBinding assignments
        stack.add(this);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return tempNode.executeGeneric(frame);
    }
}
