package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelfSelector;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SingleExtendedSuperNode;

public abstract class DoubleExtendedDoAnythingNode {
    public static class StoreIntoReceiverVariableNode extends PopIntoReceiverVariableNode {
        public StoreIntoReceiverVariableNode(CompiledCodeObject method, int idx, int receiverIdx) {
            super(method, idx, receiverIdx);
        }

    }

    public static class StoreIntoAssociationNode extends SqueakBytecodeNode {
        private final Object literal;
        private SqueakNode valueNode;
        @Child SqueakNode storeNode;

        public StoreIntoAssociationNode(CompiledCodeObject method, int idx, int literalIndex) {
            super(method, idx);
            literal = method.getLiteral(literalIndex);
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            // TODO Auto-generated method stub
            return null;
        }
    }

    public static SqueakBytecodeNode create(CompiledCodeObject method, int idx, int second, int third) {
        int opType = second >> 5;
        switch (opType) {
            case 0:
                return new SendSelfSelector(method, idx, method.getLiteral(third), second & 31);
            case 1:
                return new SingleExtendedSuperNode(method, idx, third, second & 31);
            case 2:
                return new PushReceiverVariableNode(method, idx, third);
            case 3:
                return new PushLiteralConstantNode(method, idx, third);
            case 4:
                return new PushLiteralVariableNode(method, idx, third);
            case 5:
                return new StoreIntoReceiverVariableNode(method, idx, third);
            case 6:
                return new PopIntoReceiverVariableNode(method, idx, third);
            case 7:
                return new StoreIntoAssociationNode(method, idx, third);
            default:
                return new UnknownBytecodeNode(method, idx, second);
        }
    }
}
