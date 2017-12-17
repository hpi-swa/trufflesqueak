package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.store.StoreIntoAssociationNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.store.StoreIntoReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.store.StoreIntoTempNode;

public class ExtendedStoreNode extends ExtendedAccess {

    private ExtendedStoreNode() {
    }

    public static AbstractBytecodeNode create(CompiledCodeObject code, int index, int numBytecodes, int nextByte) {
        int variableIndex = variableIndex(nextByte);
        switch (variableType(nextByte)) {
            case 0:
                return new StoreIntoReceiverVariableNode(code, index, numBytecodes, variableIndex);
            case 1:
                return new StoreIntoTempNode(code, index, numBytecodes, variableIndex);
            case 2:
                return new UnknownBytecodeNode(code, index, numBytecodes, nextByte);
            case 3:
                return new StoreIntoAssociationNode(code, index, numBytecodes, variableIndex);
            default:
                throw new RuntimeException("illegal ExtendedStore bytecode");
        }
    }
}
