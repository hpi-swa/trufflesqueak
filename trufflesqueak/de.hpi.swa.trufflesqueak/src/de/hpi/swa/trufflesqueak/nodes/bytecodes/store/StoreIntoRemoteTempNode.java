package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.TopStackNode;

public class StoreIntoRemoteTempNode extends AbstractStoreIntoRemoteTempNode {
    @Child private TopStackNode topNode;

    public StoreIntoRemoteTempNode(CompiledCodeObject code, int index, int numBytecodes, int indexInArray, int indexOfArray) {
        super(code, index, numBytecodes, indexInArray, indexOfArray);
        topNode = new TopStackNode(code);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        storeNode.executeWrite(getTempArray(frame), topNode.execute(frame));
    }

    @Override
    public String toString() {
        return String.format("storeIntoTemp: %d inVectorAt: %d", indexInArray, indexOfArray);
    }
}
