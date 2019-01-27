package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;
import de.hpi.swa.graal.squeak.nodes.context.MethodLiteralNode;
import de.hpi.swa.graal.squeak.nodes.context.SqueakObjectAtPutAndMarkContextsNode;
import de.hpi.swa.graal.squeak.nodes.context.ReceiverNode;
import de.hpi.swa.graal.squeak.nodes.context.TemporaryReadNode;
import de.hpi.swa.graal.squeak.nodes.context.TemporaryWriteNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPopNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackTopNode;

public final class StoreBytecodes {

    private abstract static class AbstractStoreIntoAssociationNode extends AbstractStoreIntoNode {
        protected final long variableIndex;

        private AbstractStoreIntoAssociationNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long variableIndex) {
            super(code, index, numBytecodes);
            this.variableIndex = variableIndex;
            storeNode = SqueakObjectAtPutAndMarkContextsNode.create(ASSOCIATION.VALUE, new MethodLiteralNode(code, variableIndex), getValueNode());
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return getTypeName() + "IntoLit: " + variableIndex;
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    private abstract static class AbstractStoreIntoNode extends AbstractBytecodeNode {
        @Child protected SqueakObjectAtPutAndMarkContextsNode storeNode;

        private AbstractStoreIntoNode(final CompiledCodeObject code, final int index, final int numBytecodes) {
            super(code, index, numBytecodes);
        }

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(frame);
        }

        protected abstract String getTypeName();

        protected abstract SqueakNode getValueNode();
    }

    private abstract static class AbstractStoreIntoReceiverVariableNode extends AbstractStoreIntoNode {
        protected final long receiverIndex;

        private AbstractStoreIntoReceiverVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long receiverIndex) {
            super(code, index, numBytecodes);
            this.receiverIndex = receiverIndex;
            storeNode = SqueakObjectAtPutAndMarkContextsNode.create(receiverIndex, ReceiverNode.create(code), getValueNode());
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return getTypeName() + "IntoRcvr: " + receiverIndex;
        }
    }

    private abstract static class AbstractStoreIntoRemoteTempNode extends AbstractStoreIntoNode {
        private final int indexInArray;
        private final int indexOfArray;

        private AbstractStoreIntoRemoteTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int indexInArray, final int indexOfArray) {
            super(code, index, numBytecodes);
            this.indexInArray = indexInArray;
            this.indexOfArray = indexOfArray;
            storeNode = SqueakObjectAtPutAndMarkContextsNode.create(indexInArray, TemporaryReadNode.create(code, indexOfArray), getValueNode());
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return getTypeName() + "IntoTemp: " + indexInArray + " inVectorAt: " + indexOfArray;
        }
    }

    private abstract static class AbstractStoreIntoTempNode extends AbstractBytecodeNode {
        @Child protected TemporaryWriteNode storeNode;
        protected final int tempIndex;

        private AbstractStoreIntoTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int tempIndex) {
            super(code, index, numBytecodes);
            this.tempIndex = tempIndex;
            this.storeNode = TemporaryWriteNode.create(code, tempIndex);
        }

        protected abstract String getTypeName();

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return getTypeName() + "IntoTemp: " + tempIndex;
        }
    }

    public static final class PopIntoAssociationNode extends AbstractStoreIntoAssociationNode {
        public PopIntoAssociationNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long variableIndex) {
            super(code, index, numBytecodes, variableIndex);
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }

        @Override
        protected SqueakNode getValueNode() {
            return StackPopNode.create(code);
        }
    }

    public static final class PopIntoReceiverVariableNode extends AbstractStoreIntoReceiverVariableNode {

        public PopIntoReceiverVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long receiverIndex) {
            super(code, index, numBytecodes, receiverIndex);
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }

        @Override
        protected SqueakNode getValueNode() {
            return StackPopNode.create(code);
        }
    }

    public static final class PopIntoRemoteTempNode extends AbstractStoreIntoRemoteTempNode {

        public PopIntoRemoteTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int indexInArray, final int indexOfArray) {
            super(code, index, numBytecodes, indexInArray, indexOfArray);
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }

        @Override
        protected SqueakNode getValueNode() {
            return StackPopNode.create(code);
        }
    }

    public static final class PopIntoTemporaryLocationNode extends AbstractStoreIntoTempNode {
        @Child private StackPopNode popNode;

        public PopIntoTemporaryLocationNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int tempIndex) {
            super(code, index, numBytecodes, tempIndex);
            popNode = StackPopNode.create(code);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(frame, popNode.executeRead(frame));
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }
    }

    public static final class StoreIntoAssociationNode extends AbstractStoreIntoAssociationNode {
        @Child private StackTopNode topNode;

        public StoreIntoAssociationNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long variableIndex) {
            super(code, index, numBytecodes, variableIndex);
        }

        @Override
        protected String getTypeName() {
            return "store";
        }

        @Override
        protected SqueakNode getValueNode() {
            return StackTopNode.create(code);
        }
    }

    public static final class StoreIntoReceiverVariableNode extends AbstractStoreIntoReceiverVariableNode {
        @Child private StackTopNode topNode;

        public StoreIntoReceiverVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long receiverIndex) {
            super(code, index, numBytecodes, receiverIndex);
        }

        @Override
        protected String getTypeName() {
            return "store";
        }

        @Override
        protected SqueakNode getValueNode() {
            return StackTopNode.create(code);
        }
    }

    public static final class StoreIntoRemoteTempNode extends AbstractStoreIntoRemoteTempNode {

        public StoreIntoRemoteTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int indexInArray, final int indexOfArray) {
            super(code, index, numBytecodes, indexInArray, indexOfArray);
        }

        @Override
        protected String getTypeName() {
            return "store";
        }

        @Override
        protected SqueakNode getValueNode() {
            return StackTopNode.create(code);
        }
    }

    public static final class StoreIntoTempNode extends AbstractStoreIntoTempNode {
        @Child private StackTopNode topNode;

        public StoreIntoTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int tempIndex) {
            super(code, index, numBytecodes, tempIndex);
            topNode = StackTopNode.create(code);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(frame, topNode.executeRead(frame));
        }

        @Override
        protected String getTypeName() {
            return "store";
        }
    }
}
