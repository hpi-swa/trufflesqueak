package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;

public class StoreIntoTempNode extends SqueakBytecodeNode {
    @Child FrameSlotWriteNode storeNode;
    protected final int tempIndex;

    public StoreIntoTempNode(CompiledCodeObject code, int index, int numBytecodes, int tempIndex) {
        super(code, index, numBytecodes);
        assert code.getNumStackSlots() > tempIndex;
        this.tempIndex = tempIndex;
        this.storeNode = FrameSlotWriteNode.create(code.getTempSlot(tempIndex));
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return storeNode.executeWrite(frame, top(frame));
    }

    @Override
    public String toString() {
        return "storeIntoTemp: " + tempIndex;
    }
}
