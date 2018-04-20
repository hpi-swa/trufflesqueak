package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;
import de.hpi.swa.graal.squeak.nodes.context.MethodLiteralNode;
import de.hpi.swa.graal.squeak.nodes.context.ObjectAtPutNode;
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
            storeNode = ObjectAtPutNode.create(ASSOCIATION.VALUE, new MethodLiteralNode(code, variableIndex), getValueNode());
        }

        @Override
        public String toString() {
            return getTypeName() + "IntoLit: " + variableIndex;
        }
    }

    private abstract static class AbstractStoreIntoNode extends AbstractBytecodeNode {
        @Child protected ObjectAtPutNode storeNode;

        private AbstractStoreIntoNode(final CompiledCodeObject code, final int index, final int numBytecodes) {
            super(code, index, numBytecodes);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
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
            storeNode = ObjectAtPutNode.create(receiverIndex, ReceiverNode.create(code), getValueNode());
        }

        @Override
        public String toString() {
            return getTypeName() + "IntoRcvr: " + receiverIndex;
        }
    }

    private abstract static class AbstractStoreIntoRemoteTempNode extends AbstractStoreIntoNode {
        @CompilationFinal private final long indexInArray;
        @CompilationFinal private final long indexOfArray;

        private AbstractStoreIntoRemoteTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long indexInArray, final long indexOfArray) {
            super(code, index, numBytecodes);
            this.indexInArray = indexInArray;
            this.indexOfArray = indexOfArray;
            storeNode = ObjectAtPutNode.create(indexInArray, TemporaryReadNode.create(code, indexOfArray), getValueNode());
        }

        @Override
        public String toString() {
            return getTypeName() + "IntoTemp: " + indexInArray + " inVectorAt: " + indexOfArray;
        }
    }

    private abstract static class AbstractStoreIntoTempNode extends AbstractBytecodeNode {
        @Child TemporaryWriteNode storeNode;
        protected final long tempIndex;

        private AbstractStoreIntoTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long tempIndex) {
            super(code, index, numBytecodes);
            this.tempIndex = tempIndex;
            this.storeNode = TemporaryWriteNode.create(code, tempIndex);
        }

        protected abstract String getTypeName();

        @Override
        public String toString() {
            return getTypeName() + "IntoTemp: " + tempIndex;
        }
    }

    public static class PopIntoAssociationNode extends AbstractStoreIntoAssociationNode {
        @Child private StackPopNode popNode;

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

    public static class PopIntoReceiverVariableNode extends AbstractStoreIntoReceiverVariableNode {

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

    public static class PopIntoRemoteTempNode extends AbstractStoreIntoRemoteTempNode {

        public PopIntoRemoteTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long indexInArray, final long indexOfArray) {
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

    public static class PopIntoTemporaryLocationNode extends AbstractStoreIntoTempNode {
        @Child private StackPopNode popNode;

        public PopIntoTemporaryLocationNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long tempIndex) {
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

    public static class StoreIntoAssociationNode extends AbstractStoreIntoAssociationNode {
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

    public static class StoreIntoReceiverVariableNode extends AbstractStoreIntoReceiverVariableNode {
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

    public static class StoreIntoRemoteTempNode extends AbstractStoreIntoRemoteTempNode {

        public StoreIntoRemoteTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long indexInArray, final long indexOfArray) {
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

    public static class StoreIntoTempNode extends AbstractStoreIntoTempNode {
        @Child private StackTopNode topNode;

        public StoreIntoTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long tempIndex) {
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
