package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class ExtendedPushNode extends ExtendedAccess {
    private ExtendedPushNode() {
    }

    public static SqueakBytecodeNode create(CompiledCodeObject code, int idx, int i) {
        switch (extractType(i)) {
            case 0:
                return new PushReceiverNode(code, idx);
            case 1:
                return new PushTemporaryVariableNode(code, idx, extractIndex(i));
            case 2:
                return new PushLiteralConstantNode(code, idx, extractIndex(i));
            case 3:
                return new PushLiteralVariableNode(code, idx, extractIndex(i));
            default:
                throw new RuntimeException("unexpected type for ExtendedPush");
        }
    }
}
