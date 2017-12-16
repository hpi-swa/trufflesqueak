package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopStackNode;

public class PopIntoRemoteTempNode extends AbstractStoreIntoRemoteTempNode {
    @Child private PopStackNode popNode;

    public PopIntoRemoteTempNode(CompiledCodeObject code, int index, int numBytecodes, int indexInArray, int indexOfArray) {
        super(code, index, numBytecodes, indexInArray, indexOfArray);
        popNode = new PopStackNode(code);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        storeNode.executeWrite(getTempArray(frame), popNode.execute(frame));
    }

    @Override
    public String toString() {
        return String.format("popIntoTemp: %d inVectorAt: %d", indexInArray, indexOfArray);
    }
}
