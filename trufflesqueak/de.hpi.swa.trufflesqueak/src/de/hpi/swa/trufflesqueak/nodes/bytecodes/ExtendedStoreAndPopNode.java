package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.store.PopIntoAssociationNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.store.PopIntoReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.store.PopIntoTempNode;

public class ExtendedStoreAndPopNode extends ExtendedAccess {
    private ExtendedStoreAndPopNode() {
    }

    public static SqueakBytecodeNode create(CompiledCodeObject code, int index, int numBytecodes, int nextByte) {
        int variableIndex = variableIndex(nextByte);
        switch (variableType(nextByte)) {
            case 0:
                return new PopIntoReceiverVariableNode(code, index, numBytecodes, variableIndex);
            case 1:
                return new PopIntoTempNode(code, index, numBytecodes, variableIndex);
            case 2:
                return new UnknownBytecodeNode(code, index, numBytecodes, nextByte);
            case 3:
                return new PopIntoAssociationNode(code, index, numBytecodes, variableIndex);
            default:
                throw new RuntimeException("illegal ExtendedStore bytecode");
        }
    }
}
