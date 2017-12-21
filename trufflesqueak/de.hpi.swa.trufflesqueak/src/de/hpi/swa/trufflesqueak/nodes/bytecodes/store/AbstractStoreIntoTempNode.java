package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotWriteNode;

public abstract class AbstractStoreIntoTempNode extends AbstractBytecodeNode {
    @Child FrameSlotWriteNode storeNode;
    protected final int tempIndex;

    public AbstractStoreIntoTempNode(CompiledCodeObject code, int index, int numBytecodes, int tempIndex) {
        super(code, index, numBytecodes);
        this.tempIndex = tempIndex;
        this.storeNode = FrameSlotWriteNode.create(code.getTempSlot(tempIndex));
    }
}
