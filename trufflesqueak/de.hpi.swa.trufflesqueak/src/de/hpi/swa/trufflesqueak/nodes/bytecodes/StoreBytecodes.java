package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNode;
import de.hpi.swa.trufflesqueak.nodes.context.ReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.TemporaryReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopStackNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.TopStackNode;

public final class StoreBytecodes {

    private static abstract class AbstractStoreIntoAssociationNode extends AbstractStoreIntoNode {
        protected final int variableIndex;

        private AbstractStoreIntoAssociationNode(CompiledCodeObject code, int index, int numBytecodes, int variableIndex) {
            super(code, index, numBytecodes);
            this.variableIndex = variableIndex;
            storeNode = ObjectAtPutNode.create(ASSOCIATION.VALUE, new MethodLiteralNode(code, variableIndex), getValueNode());
        }

        @Override
        public String toString() {
            return String.format("%sIntoLit: %d", getTypeName(), variableIndex);
        }
    }

    private static abstract class AbstractStoreIntoNode extends AbstractBytecodeNode {
        @Child protected ObjectAtPutNode storeNode;

        private AbstractStoreIntoNode(CompiledCodeObject code, int index, int numBytecodes) {
            super(code, index, numBytecodes);
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            storeNode.executeWrite(frame);
        }

        protected abstract String getTypeName();

        protected abstract SqueakNode getValueNode();
    }

    private static abstract class AbstractStoreIntoReceiverVariableNode extends AbstractStoreIntoNode {
        protected final int receiverIndex;

        private AbstractStoreIntoReceiverVariableNode(CompiledCodeObject code, int index, int numBytecodes, int receiverIndex) {
            super(code, index, numBytecodes);
            this.receiverIndex = receiverIndex;
            storeNode = ObjectAtPutNode.create(receiverIndex, ReceiverNode.create(code), getValueNode());
        }

        @Override
        public String toString() {
            return String.format("%sIntoRcvr: %d", getTypeName(), receiverIndex);
        }
    }

    private static abstract class AbstractStoreIntoRemoteTempNode extends AbstractStoreIntoNode {
        @CompilationFinal private final int indexInArray;
        @CompilationFinal private final int indexOfArray;

        private AbstractStoreIntoRemoteTempNode(CompiledCodeObject code, int index, int numBytecodes, int indexInArray, int indexOfArray) {
            super(code, index, numBytecodes);
            this.indexInArray = indexInArray;
            this.indexOfArray = indexOfArray;
            storeNode = ObjectAtPutNode.create(indexInArray, TemporaryReadNode.create(code, indexOfArray), getValueNode());
        }

        @Override
        public String toString() {
            return String.format("%sIntoTemp: %d inVectorAt: %d", getTypeName(), this.indexInArray, this.indexOfArray);
        }
    }

    private static abstract class AbstractStoreIntoTempNode extends AbstractBytecodeNode {
        @Child FrameSlotWriteNode storeNode;
        protected final int tempIndex;

        private AbstractStoreIntoTempNode(CompiledCodeObject code, int index, int numBytecodes, int tempIndex) {
            super(code, index, numBytecodes);
            this.tempIndex = tempIndex;
            int stackIndex = code.convertTempIndexToStackIndex(tempIndex);
            if (stackIndex >= 0) {
                this.storeNode = FrameSlotWriteNode.create(code.getStackSlot(stackIndex));
            }
        }

        protected abstract String getTypeName();

        @Override
        public String toString() {
            return String.format("%sIntoTemp: %d", getTypeName(), tempIndex);
        }
    }

    public static class PopIntoAssociationNode extends AbstractStoreIntoAssociationNode {
        @Child private PopStackNode popNode;

        public PopIntoAssociationNode(CompiledCodeObject code, int index, int numBytecodes, int variableIndex) {
            super(code, index, numBytecodes, variableIndex);
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }

        @Override
        protected SqueakNode getValueNode() {
            return PopStackNode.create(code);
        }
    }

    public static class PopIntoReceiverVariableNode extends AbstractStoreIntoReceiverVariableNode {

        public PopIntoReceiverVariableNode(CompiledCodeObject code, int index, int numBytecodes, int receiverIndex) {
            super(code, index, numBytecodes, receiverIndex);
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }

        @Override
        protected SqueakNode getValueNode() {
            return PopStackNode.create(code);
        }
    }

    public static class PopIntoRemoteTempNode extends AbstractStoreIntoRemoteTempNode {

        public PopIntoRemoteTempNode(CompiledCodeObject code, int index, int numBytecodes, int indexInArray, int indexOfArray) {
            super(code, index, numBytecodes, indexInArray, indexOfArray);
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }

        @Override
        protected SqueakNode getValueNode() {
            return PopStackNode.create(code);
        }
    }

    public static class PopIntoTemporaryLocationNode extends AbstractStoreIntoTempNode {
        @Child private PopStackNode popNode;

        public PopIntoTemporaryLocationNode(CompiledCodeObject code, int index, int numBytecodes, int tempIndex) {
            super(code, index, numBytecodes, tempIndex);
            popNode = PopStackNode.create(code);
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            storeNode.executeWrite(frame, popNode.executeGeneric(frame));
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }
    }

    public static class StoreIntoAssociationNode extends AbstractStoreIntoAssociationNode {
        @Child private TopStackNode topNode;

        public StoreIntoAssociationNode(CompiledCodeObject code, int index, int numBytecodes, int variableIndex) {
            super(code, index, numBytecodes, variableIndex);
        }

        @Override
        protected String getTypeName() {
            return "store";
        }

        @Override
        protected SqueakNode getValueNode() {
            return TopStackNode.create(code);
        }
    }

    public static class StoreIntoReceiverVariableNode extends AbstractStoreIntoReceiverVariableNode {
        @Child private TopStackNode topNode;

        public StoreIntoReceiverVariableNode(CompiledCodeObject code, int index, int numBytecodes, int receiverIndex) {
            super(code, index, numBytecodes, receiverIndex);
        }

        @Override
        protected String getTypeName() {
            return "store";
        }

        @Override
        protected SqueakNode getValueNode() {
            return TopStackNode.create(code);
        }
    }

    public static class StoreIntoRemoteTempNode extends AbstractStoreIntoRemoteTempNode {

        public StoreIntoRemoteTempNode(CompiledCodeObject code, int index, int numBytecodes, int indexInArray, int indexOfArray) {
            super(code, index, numBytecodes, indexInArray, indexOfArray);
        }

        @Override
        protected String getTypeName() {
            return "store";
        }

        @Override
        protected SqueakNode getValueNode() {
            return TopStackNode.create(code);
        }
    }

    public static class StoreIntoTempNode extends AbstractStoreIntoTempNode {
        @Child private TopStackNode topNode;

        public StoreIntoTempNode(CompiledCodeObject code, int index, int numBytecodes, int tempIndex) {
            super(code, index, numBytecodes, tempIndex);
            topNode = TopStackNode.create(code);
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            storeNode.executeWrite(frame, topNode.executeGeneric(frame));
        }

        @Override
        protected String getTypeName() {
            return "store";
        }
    }
}
