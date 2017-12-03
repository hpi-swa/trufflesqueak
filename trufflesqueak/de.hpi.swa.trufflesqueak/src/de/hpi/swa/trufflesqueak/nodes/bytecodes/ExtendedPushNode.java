package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class ExtendedPushNode extends ExtendedAccess {
    private ExtendedPushNode() {
    }

    public static SqueakBytecodeNode create(CompiledCodeObject code, int index, int i) {
        switch (extractType(i)) {
            case 0:
                return new PushReceiverNode(code, index);
            case 1:
                return new PushTemporaryVariableNode(code, index, extractIndex(i));
            case 2:
                return new PushLiteralConstantNode(code, index, extractIndex(i));
            case 3:
                return new PushLiteralVariableNode(code, index, extractIndex(i));
            default:
                throw new RuntimeException("unexpected type for ExtendedPush");
        }
    }
}
