package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelfSelector;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SingleExtendedSuperNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNodeGen;

public abstract class DoubleExtendedDoAnythingNode {
    public static class StoreIntoReceiverVariableNode extends PopIntoReceiverVariableNode {
        public StoreIntoReceiverVariableNode(CompiledMethodObject cm, int idx, int receiverIdx) {
            super(cm, idx, receiverIdx);
        }

        @Override
        public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
            super.interpretOn(stack, statements);
            stack.push(statements.pop());
        }
    }

    public static class StoreIntoAssociationNode extends SqueakBytecodeNode {
        private final BaseSqueakObject literal;
        @Child SqueakNode storeNode;

        public StoreIntoAssociationNode(CompiledMethodObject cm, int idx, int literalIndex) {
            super(cm, idx);
            literal = cm.getLiteral(literalIndex);
        }

        @Override
        public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
            storeNode = ObjectAtPutNodeGen.create(method, 1, new ConstantNode(method, index, literal), stack.pop());
            stack.push(this);
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            // TODO Auto-generated method stub
            return null;
        }

    }

    public static SqueakBytecodeNode create(CompiledMethodObject cm, int idx, int second, int third) {
        int opType = second >> 5;
        switch (opType) {
            case 0:
                return new SendSelfSelector(cm, idx, cm.getLiteral(third), second & 31);
            case 1:
                return new SingleExtendedSuperNode(cm, idx, third, second & 31);
            case 2:
                return new ReceiverNode(cm, idx);
            case 3:
                return new LiteralConstantNode(cm, idx, third);
            case 4:
                return new LiteralVariableNode(cm, idx, third);
            case 5:
                return new StoreIntoReceiverVariableNode(cm, idx, third);
            case 6:
                return new PopIntoReceiverVariableNode(cm, idx, third);
            case 7:
                return new StoreIntoAssociationNode(cm, idx, third);
            default:
                return new UnknownBytecodeNode(cm, idx, second);
        }
    }
}
