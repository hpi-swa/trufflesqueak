package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class ExtendedStoreAndPopNode extends ExtendedAccess {
    private ExtendedStoreAndPopNode() {
    }

    public static SqueakBytecodeNode create(CompiledCodeObject code, int index, int numBytecodes, int nextByte) {
        int variableIndex = variableIndex(nextByte);
        switch (variableType(nextByte)) {
            case 0:
                return new StoreAndPopReceiverVariableNode(code, index, numBytecodes, variableIndex);
            case 1:
                return new StoreAndPopTemporaryVariableNode(code, index, numBytecodes, variableIndex);
            case 2:
                return new UnknownBytecodeNode(code, index, numBytecodes, nextByte);
            case 3:
                return new StoreAndPopIntoAssociationNode(code, index, numBytecodes, variableIndex);
            default:
                throw new RuntimeException("illegal ExtendedStore bytecode");
        }
    }
}
