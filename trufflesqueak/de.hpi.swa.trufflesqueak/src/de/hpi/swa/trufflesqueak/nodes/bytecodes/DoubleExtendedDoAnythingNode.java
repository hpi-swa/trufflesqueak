package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.push.PushLiteralConstantNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.push.PushLiteralVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.push.PushReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelfSelector;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SingleExtendedSuperNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.store.PopIntoReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.store.StoreIntoAssociationNode;

public abstract class DoubleExtendedDoAnythingNode {
    public static class StoreIntoReceiverVariableNode extends PopIntoReceiverVariableNode {
        public StoreIntoReceiverVariableNode(CompiledCodeObject method, int idx, int numBytecodes, int receiverIdx) {
            super(method, idx, numBytecodes, receiverIdx);
        }
    }

    public static SqueakBytecodeNode create(CompiledCodeObject code, int index, int numBytecodes, int second, int third) {
        int opType = second >> 5;
        switch (opType) {
            case 0:
                return new SendSelfSelector(code, index, numBytecodes, code.getLiteral(third), second & 31);
            case 1:
                return new SingleExtendedSuperNode(code, index, numBytecodes, third, second & 31);
            case 2:
                return new PushReceiverVariableNode(code, index, numBytecodes, third);
            case 3:
                return new PushLiteralConstantNode(code, index, numBytecodes, third);
            case 4:
                return new PushLiteralVariableNode(code, index, numBytecodes, third);
            case 5:
                return new StoreIntoReceiverVariableNode(code, index, numBytecodes, third);
            case 6:
                return new PopIntoReceiverVariableNode(code, index, numBytecodes, third);
            case 7:
                return new StoreIntoAssociationNode(code, index, numBytecodes, third);
            default:
                return new UnknownBytecodeNode(code, index, numBytecodes, second);
        }
    }
}
