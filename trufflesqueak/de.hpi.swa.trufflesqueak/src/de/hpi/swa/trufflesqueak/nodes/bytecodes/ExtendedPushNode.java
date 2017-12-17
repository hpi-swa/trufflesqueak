package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.push.PushLiteralConstantNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.push.PushLiteralVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.push.PushReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.push.PushTemporaryLocationNode;

public class ExtendedPushNode extends ExtendedAccess {
    private ExtendedPushNode() {
    }

    public static SqueakBytecodeNode create(CompiledCodeObject code, int index, int numBytecodes, int nextByte) {
        int variableIndex = variableIndex(nextByte);
        switch (variableType(nextByte)) {
            case 0:
                return new PushReceiverVariableNode(code, index, numBytecodes, variableIndex);
            case 1:
                return new PushTemporaryLocationNode(code, index, numBytecodes, variableIndex);
            case 2:
                return new PushLiteralConstantNode(code, index, numBytecodes, variableIndex);
            case 3:
                return new PushLiteralVariableNode(code, index, numBytecodes, variableIndex);
            default:
                throw new RuntimeException("unexpected type for ExtendedPush");
        }
    }
}
