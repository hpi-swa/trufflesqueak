package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public class TemporaryVariableNode extends SqueakBytecodeNode {
    @Child SqueakNode tempNode;

    public TemporaryVariableNode(CompiledCodeObject method, int idx, int tempIndex) {
        super(method, idx);
        if (method.stackSlots.length <= tempIndex) {
            // sometimes we'll decode more bytecodes than we have slots ... that's fine
            tempNode = new UnknownBytecodeNode(method, idx, tempIndex);
        } else {
            tempNode = FrameSlotReadNode.create(method, method.stackSlots[tempIndex]);
        }
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
