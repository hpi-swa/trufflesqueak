package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;

public class StoreAndPopTemporaryVariableNode extends SqueakBytecodeNode {
    @Child FrameSlotWriteNode tempNode;
    private final int tempIndex;

    public StoreAndPopTemporaryVariableNode(CompiledCodeObject code, int index, int numBytecodes, int tempIndex) {
        super(code, index, numBytecodes);
        assert code.getNumStackSlots() > tempIndex;
        this.tempIndex = tempIndex;
        this.tempNode = FrameSlotWriteNode.create(code.getTempSlot(tempIndex));
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return tempNode.executeWrite(frame, pop(frame));
    }

    @Override
    public String toString() {
        return "popIntoTemp: " + tempIndex;
    }
}
