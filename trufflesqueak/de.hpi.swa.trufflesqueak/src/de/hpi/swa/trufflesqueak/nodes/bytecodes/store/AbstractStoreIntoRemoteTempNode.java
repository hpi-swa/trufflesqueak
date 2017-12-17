package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.StoreIntoRemoteTemporaryLocationNode;

public abstract class AbstractStoreIntoRemoteTempNode extends AbstractBytecodeNode {
    @Child protected StoreIntoRemoteTemporaryLocationNode storeNode;

    public AbstractStoreIntoRemoteTempNode(CompiledCodeObject code, int index, int numBytecodes, int indexInArray, int indexOfArray) {
        super(code, index, numBytecodes);
        storeNode = new StoreIntoRemoteTemporaryLocationNode(code, indexInArray, indexOfArray);
    }

    @Override
    public String toString() {
        return String.format("%sIntoTemp: %d inVectorAt: %d", getTypeName(),
                        storeNode.getIndexInArray(), storeNode.getIndexOfArray());
    }

    protected abstract String getTypeName();
}
