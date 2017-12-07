package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class ExtendedPushNode extends ExtendedAccess {
    private ExtendedPushNode() {
    }

    public static SqueakBytecodeNode create(CompiledCodeObject code, int index, int nextByte) {
        int variableIndex = variableIndex(nextByte);
        switch (variableType(nextByte)) {
            case 0:
                return new PushReceiverVariableNode(code, index, variableIndex);
            case 1:
                return new PushTemporaryVariableNode(code, index, variableIndex);
            case 2:
                return new PushLiteralConstantNode(code, index, variableIndex);
            case 3:
                return new PushLiteralVariableNode(code, index, variableIndex);
            default:
                throw new RuntimeException("unexpected type for ExtendedPush");
        }
    }
}
