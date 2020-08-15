/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakObjectAtPutAndMarkContextsNode;
import de.hpi.swa.trufflesqueak.nodes.context.TemporaryWriteMarkContextsNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPopNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackTopNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

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
            return getTypeName() + "IntoLit: " + SqueakObjectAt0Node.getUncached().execute(code.getLiteral(variableIndex), ASSOCIATION.KEY);
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    private abstract static class AbstractStoreIntoNode extends AbstractInstrumentableBytecodeNode {
        @Child protected SqueakObjectAtPutAndMarkContextsNode storeNode;

        private AbstractStoreIntoNode(final CompiledCodeObject code, final int index, final int numBytecodes) {
            super(code, index, numBytecodes);
        }

        protected abstract String getTypeName();
    }

    private abstract static class AbstractStoreIntoReceiverVariableNode extends AbstractStoreIntoNode {
        protected final int receiverIndex;

        private AbstractStoreIntoReceiverVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int receiverIndex) {
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

        private AbstractStoreIntoRemoteTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final byte indexInArray, final byte indexOfArray) {
            super(code, index, numBytecodes);
            this.indexInArray = Byte.toUnsignedInt(indexInArray);
            this.indexOfArray = Byte.toUnsignedInt(indexOfArray);
            storeNode = SqueakObjectAtPutAndMarkContextsNode.create(indexInArray);
        }

        protected final ArrayObject getRemoteTemp(final VirtualFrame frame) {
            return (ArrayObject) FrameAccess.getStackAt(frame, code.getStackSlot(), indexOfArray);
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return getTypeName() + "IntoTemp: " + indexInArray + " inVectorAt: " + indexOfArray;
        }
    }

    private abstract static class AbstractStoreIntoTempNode extends AbstractInstrumentableBytecodeNode {
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
        @Child private FrameStackPopNode popNode = FrameStackPopNode.create();

        public PopIntoAssociationNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long variableIndex) {
            super(code, index, numBytecodes, variableIndex);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(code.getLiteral(variableIndex), popNode.execute(frame));
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }
    }

    public static final class PopIntoReceiverVariableNode extends AbstractStoreIntoReceiverVariableNode {
        @Child private FrameStackPopNode popNode = FrameStackPopNode.create();

        public PopIntoReceiverVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int receiverIndex) {
            super(code, index, numBytecodes, receiverIndex);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(FrameAccess.getReceiver(frame), popNode.execute(frame));
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }
    }

    public static final class PopIntoRemoteTempNode extends AbstractStoreIntoRemoteTempNode {
        @Child private FrameStackPopNode popNode = FrameStackPopNode.create();

        public PopIntoRemoteTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final byte indexInArray, final byte indexOfArray) {
            super(code, index, numBytecodes, indexInArray, indexOfArray);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(getRemoteTemp(frame), popNode.execute(frame));
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }
    }

    public static final class PopIntoTemporaryLocationNode extends AbstractStoreIntoTempNode {
        @Child private FrameStackPopNode popNode = FrameStackPopNode.create();

        public PopIntoTemporaryLocationNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int tempIndex) {
            super(code, index, numBytecodes, tempIndex);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(frame, popNode.execute(frame));
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }
    }

    public static final class StoreIntoAssociationNode extends AbstractStoreIntoAssociationNode {
        @Child private FrameStackTopNode topNode = FrameStackTopNode.create();

        public StoreIntoAssociationNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long variableIndex) {
            super(code, index, numBytecodes, variableIndex);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(code.getLiteral(variableIndex), topNode.execute(frame));
        }

        @Override
        protected String getTypeName() {
            return "store";
        }
    }

    public static final class StoreIntoReceiverVariableNode extends AbstractStoreIntoReceiverVariableNode {
        @Child private FrameStackTopNode topNode = FrameStackTopNode.create();

        public StoreIntoReceiverVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int receiverIndex) {
            super(code, index, numBytecodes, receiverIndex);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(FrameAccess.getReceiver(frame), topNode.execute(frame));
        }

        @Override
        protected String getTypeName() {
            return "store";
        }
    }

    public static final class StoreIntoRemoteTempNode extends AbstractStoreIntoRemoteTempNode {
        @Child private FrameStackTopNode topNode = FrameStackTopNode.create();

        public StoreIntoRemoteTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final byte indexInArray, final byte indexOfArray) {
            super(code, index, numBytecodes, indexInArray, indexOfArray);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(getRemoteTemp(frame), topNode.execute(frame));
        }

        @Override
        protected String getTypeName() {
            return "store";
        }
    }

    public static final class StoreIntoTempNode extends AbstractStoreIntoTempNode {
        @Child private FrameStackTopNode topNode = FrameStackTopNode.create();

        public StoreIntoTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int tempIndex) {
            super(code, index, numBytecodes, tempIndex);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(frame, topNode.execute(frame));
        }

        @Override
        protected String getTypeName() {
            return "store";
        }
    }
}
