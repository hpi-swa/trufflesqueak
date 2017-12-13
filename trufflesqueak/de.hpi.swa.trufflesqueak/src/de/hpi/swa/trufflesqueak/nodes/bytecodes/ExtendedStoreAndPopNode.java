package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class ExtendedStoreAndPopNode extends ExtendedAccess {
    private ExtendedStoreAndPopNode() {
    }

    public static SqueakBytecodeNode create(CompiledCodeObject code, int index, int nextByte) {
        int variableIndex = variableIndex(nextByte);
        switch (variableType(nextByte)) {
            case 0:
                return new StoreAndPopReceiverVariableNode(code, index, variableIndex);
            case 1:
                return new StoreAndPopTemporaryVariableNode(code, index, variableIndex);
            case 2:
                return new UnknownBytecodeNode(code, index, -1);
            case 3:
                return new StoreAndPopIntoAssociationNode(code, index, variableIndex);
            default:
                throw new RuntimeException("illegal ExtendedStore bytecode");
        }
    }
}
