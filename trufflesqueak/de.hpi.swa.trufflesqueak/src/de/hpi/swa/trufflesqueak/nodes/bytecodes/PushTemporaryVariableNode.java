package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public class PushTemporaryVariableNode extends SqueakBytecodeNode {
    @Child FrameSlotReadNode tempNode;

    public PushTemporaryVariableNode(CompiledCodeObject code, int idx, int tempIndex) {
        super(code, idx);
        if (code.getNumStackSlots() <= tempIndex) {
            // sometimes we'll decode more bytecodes than we have slots ... that's fine
        } else {
            tempNode = FrameSlotReadNode.create(code.getStackSlot(tempIndex));
        }
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, tempNode.executeRead(frame));
    }
}
