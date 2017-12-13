package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelfSelector;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SingleExtendedSuperNode;

public abstract class DoubleExtendedDoAnythingNode {
    public static class StoreIntoReceiverVariableNode extends StoreAndPopReceiverVariableNode {
        public StoreIntoReceiverVariableNode(CompiledCodeObject method, int idx, int receiverIdx) {
            super(method, idx, receiverIdx);
        }
    }

    public static SqueakBytecodeNode create(CompiledCodeObject code, int index, int second, int third) {
        int opType = second >> 5;
        switch (opType) {
            case 0:
                return new SendSelfSelector(code, index, code.getLiteral(third), second & 31);
            case 1:
                return new SingleExtendedSuperNode(code, index, third, second & 31);
            case 2:
                return new PushReceiverVariableNode(code, index, third);
            case 3:
                return new PushLiteralConstantNode(code, index, third);
            case 4:
                return new PushLiteralVariableNode(code, index, third);
            case 5:
                return new StoreIntoReceiverVariableNode(code, index, third);
            case 6:
                return new StoreAndPopReceiverVariableNode(code, index, third);
            case 7:
                return new StoreIntoAssociationNode(code, index, third);
            default:
                return new UnknownBytecodeNode(code, index, second);
        }
    }
}
