package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.TopStackNode;

public class StoreIntoTempNode extends AbstractStoreIntoTempNode {
    @Child private TopStackNode topNode;

    public StoreIntoTempNode(CompiledCodeObject code, int index, int numBytecodes, int tempIndex) {
        super(code, index, numBytecodes, tempIndex);
        topNode = new TopStackNode(code);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return storeNode.executeWrite(frame, topNode.execute(frame));
    }

    @Override
    public String toString() {
        return "storeIntoTemp: " + tempIndex;
    }
}
