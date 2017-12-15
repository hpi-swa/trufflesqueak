package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class PopIntoRemoteTempNode extends StoreIntoRemoteTempNode {
    public PopIntoRemoteTempNode(CompiledCodeObject code, int index, int numBytecodes, int indexInArray, int indexOfArray) {
        super(code, index, numBytecodes, indexInArray, indexOfArray);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return storeNode.executeWrite(getTempArray(frame), pop(frame));
    }

    @Override
    public String toString() {
        return String.format("popIntoTemp: %d inVectorAt: %d", indexInArray, indexOfArray);
    }
}
