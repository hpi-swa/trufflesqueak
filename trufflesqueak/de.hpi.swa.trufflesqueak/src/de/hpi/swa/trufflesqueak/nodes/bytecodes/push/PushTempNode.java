package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public class PushTempNode extends SqueakBytecodeNode {
    @Child FrameSlotReadNode tempNode;
    private final int tempIndex;

    public PushTempNode(CompiledCodeObject code, int index, int numBytecodes, int tempIndex) {
        super(code, index, numBytecodes);
        this.tempIndex = tempIndex;
        if (code.getNumStackSlots() <= tempIndex) {
            // sometimes we'll decode more bytecodes than we have slots ... that's fine
        } else {
            tempNode = FrameSlotReadNode.create(code.getTempSlot(tempIndex));
        }
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, tempNode.executeRead(frame));
    }

    @Override
    public String toString() {
        return "pushTemp: " + this.tempIndex;
    }
}
