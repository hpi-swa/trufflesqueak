package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.RemoteTempBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNode;

public abstract class AbstractStoreIntoRemoteTempNode extends RemoteTempBytecodeNode {
    @Child ObjectAtPutNode storeNode;

    public AbstractStoreIntoRemoteTempNode(CompiledCodeObject code, int index, int numBytecodes, int indexInArray, int indexOfArray) {
        super(code, index, numBytecodes, indexInArray, indexOfArray);
        storeNode = ObjectAtPutNode.create(indexInArray);
    }
}
