package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.graal.squeak.nodes.context.SqueakObjectAtPutAndMarkContextsNode;
import de.hpi.swa.graal.squeak.nodes.context.TemporaryWriteMarkContextsNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackReadAndClearNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class StoreBytecodes {

    private abstract static class AbstractStoreIntoAssociationNode extends AbstractStoreIntoNode {
        protected final long variableIndex;

        private AbstractStoreIntoAssociationNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long variableIndex) {
            super(code, index, numBytecodes);
            this.variableIndex = variableIndex;
            storeNode = SqueakObjectAtPutAndMarkContextsNode.create(ASSOCIATION.VALUE);
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

        protected abstract String getTypeName();
    }

    private abstract static class AbstractStoreIntoReceiverVariableNode extends AbstractStoreIntoNode {
        protected final long receiverIndex;

        private AbstractStoreIntoReceiverVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long receiverIndex) {
            super(code, index, numBytecodes);
            this.receiverIndex = receiverIndex;
            storeNode = SqueakObjectAtPutAndMarkContextsNode.create(receiverIndex);
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

        @Child protected FrameSlotReadNode readNode;

        private AbstractStoreIntoRemoteTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int indexInArray, final int indexOfArray) {
            super(code, index, numBytecodes);
            this.indexInArray = indexInArray;
            this.indexOfArray = indexOfArray;
            storeNode = SqueakObjectAtPutAndMarkContextsNode.create(indexInArray);
            readNode = FrameSlotReadNode.create(code.getStackSlot(indexOfArray));
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return getTypeName() + "IntoTemp: " + indexInArray + " inVectorAt: " + indexOfArray;
        }
    }

    private abstract static class AbstractStoreIntoTempNode extends AbstractBytecodeNode {
        protected final int tempIndex;

        @Child protected TemporaryWriteMarkContextsNode storeNode;

        private AbstractStoreIntoTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int tempIndex) {
            super(code, index, numBytecodes);
            this.tempIndex = tempIndex;
            storeNode = TemporaryWriteMarkContextsNode.create(code, tempIndex);
        }

        protected abstract String getTypeName();

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return getTypeName() + "IntoTemp: " + tempIndex;
        }
    }

    public static final class PopIntoAssociationNode extends AbstractStoreIntoAssociationNode {
        @Child private FrameStackReadAndClearNode popNode;

        public PopIntoAssociationNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long variableIndex) {
            super(code, index, numBytecodes, variableIndex);
            popNode = FrameStackReadAndClearNode.create(code);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(code.getLiteral(variableIndex), popNode.executePop(frame));
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }
    }

    public static final class PopIntoReceiverVariableNode extends AbstractStoreIntoReceiverVariableNode {
        @Child private FrameStackReadAndClearNode popNode;

        public PopIntoReceiverVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long receiverIndex) {
            super(code, index, numBytecodes, receiverIndex);
            popNode = FrameStackReadAndClearNode.create(code);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(FrameAccess.getReceiver(frame), popNode.executePop(frame));
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }
    }

    public static final class PopIntoRemoteTempNode extends AbstractStoreIntoRemoteTempNode {
        @Child private FrameStackReadAndClearNode popNode;

        public PopIntoRemoteTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int indexInArray, final int indexOfArray) {
            super(code, index, numBytecodes, indexInArray, indexOfArray);
            popNode = FrameStackReadAndClearNode.create(code);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(readNode.executeRead(frame), popNode.executePop(frame));
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }
    }

    public static final class PopIntoTemporaryLocationNode extends AbstractStoreIntoTempNode {
        @Child private FrameStackReadAndClearNode popNode;

        public PopIntoTemporaryLocationNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int tempIndex) {
            super(code, index, numBytecodes, tempIndex);
            popNode = FrameStackReadAndClearNode.create(code);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(frame, popNode.executePop(frame));
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }
    }

    public static final class StoreIntoAssociationNode extends AbstractStoreIntoAssociationNode {
        @Child private FrameStackReadNode topNode;

        public StoreIntoAssociationNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long variableIndex) {
            super(code, index, numBytecodes, variableIndex);
            topNode = FrameStackReadNode.create(code);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(code.getLiteral(variableIndex), topNode.executeTop(frame));
        }

        @Override
        protected String getTypeName() {
            return "store";
        }
    }

    public static final class StoreIntoReceiverVariableNode extends AbstractStoreIntoReceiverVariableNode {
        @Child private FrameStackReadNode topNode;

        public StoreIntoReceiverVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long receiverIndex) {
            super(code, index, numBytecodes, receiverIndex);
            topNode = FrameStackReadNode.create(code);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(FrameAccess.getReceiver(frame), topNode.executeTop(frame));
        }

        @Override
        protected String getTypeName() {
            return "store";
        }
    }

    public static final class StoreIntoRemoteTempNode extends AbstractStoreIntoRemoteTempNode {
        @Child private FrameStackReadNode topNode;

        public StoreIntoRemoteTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int indexInArray, final int indexOfArray) {
            super(code, index, numBytecodes, indexInArray, indexOfArray);
            topNode = FrameStackReadNode.create(code);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(readNode.executeRead(frame), topNode.executeTop(frame));
        }

        @Override
        protected String getTypeName() {
            return "store";
        }
    }

    public static final class StoreIntoTempNode extends AbstractStoreIntoTempNode {
        @Child private FrameStackReadNode topNode;

        public StoreIntoTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int tempIndex) {
            super(code, index, numBytecodes, tempIndex);
            topNode = FrameStackReadNode.create(code);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(frame, topNode.executeTop(frame));
        }

        @Override
        protected String getTypeName() {
            return "store";
        }
    }
}
