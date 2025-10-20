/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakObjectAtPutAndMarkContextsNode;
import de.hpi.swa.trufflesqueak.nodes.context.TemporaryWriteMarkContextsNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPopNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class StoreBytecodes {

    private abstract static class AbstractStoreIntoAssociationNode extends AbstractStoreIntoNode {
        protected final Object literalVariable;

        private AbstractStoreIntoAssociationNode(final CompiledCodeObject code, final int successorIndex, final long variableIndex) {
            super(successorIndex);
            literalVariable = code.getLiteral(variableIndex);
            storeNode = SqueakObjectAtPutAndMarkContextsNode.create(ASSOCIATION.VALUE);
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return getTypeName() + "IntoLit: " + SqueakObjectAt0Node.executeUncached(literalVariable, ASSOCIATION.KEY);
        }
    }

    private abstract static class AbstractStoreIntoNode extends AbstractInstrumentableBytecodeNode {
        @Child protected SqueakObjectAtPutAndMarkContextsNode storeNode;

        private AbstractStoreIntoNode(final int successorIndex) {
            super(successorIndex);
        }

        protected abstract String getTypeName();
    }

    private abstract static class AbstractStoreIntoReceiverVariableNode extends AbstractStoreIntoNode {
        protected final int receiverIndex;

        private AbstractStoreIntoReceiverVariableNode(final int successorIndex, final int receiverIndex) {
            super(successorIndex);
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

        @Child private FrameStackReadNode readNode;

        private AbstractStoreIntoRemoteTempNode(final int successorIndex, final byte indexInArray, final byte indexOfArray) {
            super(successorIndex);
            this.indexInArray = Byte.toUnsignedInt(indexInArray);
            this.indexOfArray = Byte.toUnsignedInt(indexOfArray);
            storeNode = SqueakObjectAtPutAndMarkContextsNode.create(indexInArray);
        }

        protected final FrameStackReadNode getReadNode(final VirtualFrame frame) {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readNode = insert(FrameStackReadNode.create(frame, indexOfArray, false));
            }
            return readNode;
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return getTypeName() + "IntoTemp: " + indexInArray + " inVectorAt: " + indexOfArray;
        }
    }

    private abstract static class AbstractStoreIntoTempNode extends AbstractInstrumentableBytecodeNode {
        protected final int tempIndex;

        @Child private TemporaryWriteMarkContextsNode storeNode;

        private AbstractStoreIntoTempNode(final int successorIndex, final int tempIndex) {
            super(successorIndex);
            this.tempIndex = tempIndex;
        }

        protected final TemporaryWriteMarkContextsNode getStoreNode(final VirtualFrame frame) {
            if (storeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                storeNode = insert(TemporaryWriteMarkContextsNode.create(frame, tempIndex));
            }
            return storeNode;
        }

        protected abstract String getTypeName();

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return getTypeName() + "IntoTemp: " + tempIndex;
        }
    }

    public static final class PopIntoLiteralVariableNode extends AbstractStoreIntoAssociationNode {
        @Child private FrameStackPopNode popNode = FrameStackPopNode.create();

        public PopIntoLiteralVariableNode(final CompiledCodeObject code, final int successorIndex, final long variableIndex) {
            super(code, successorIndex, variableIndex);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(literalVariable, popNode.execute(frame));
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }
    }

    public static final class PopIntoReceiverVariableNode extends AbstractStoreIntoReceiverVariableNode {
        @Child private FrameStackPopNode popNode = FrameStackPopNode.create();

        public PopIntoReceiverVariableNode(final int successorIndex, final int receiverIndex) {
            super(successorIndex, receiverIndex);
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

        public PopIntoRemoteTempNode(final int successorIndex, final byte indexInArray, final byte indexOfArray) {
            super(successorIndex, indexInArray, indexOfArray);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(getReadNode(frame).executeRead(frame), popNode.execute(frame));
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }
    }

    public static final class PopIntoTemporaryLocationNode extends AbstractStoreIntoTempNode {
        @Child private FrameStackPopNode popNode = FrameStackPopNode.create();

        public PopIntoTemporaryLocationNode(final int successorIndex, final int tempIndex) {
            super(successorIndex, tempIndex);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            getStoreNode(frame).executeWrite(frame, popNode.execute(frame));
        }

        @Override
        protected String getTypeName() {
            return "pop";
        }
    }

    public static final class StoreIntoLiteralVariableNode extends AbstractStoreIntoAssociationNode {
        @Child private FrameStackReadNode topNode;

        public StoreIntoLiteralVariableNode(final VirtualFrame frame, final CompiledCodeObject code, final int successorIndex, final long variableIndex) {
            super(code, successorIndex, variableIndex);
            topNode = FrameStackReadNode.createStackTop(frame);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(literalVariable, topNode.executeRead(frame));
        }

        @Override
        protected String getTypeName() {
            return "store";
        }
    }

    public static final class StoreIntoReceiverVariableNode extends AbstractStoreIntoReceiverVariableNode {
        @Child private FrameStackReadNode topNode;

        public StoreIntoReceiverVariableNode(final VirtualFrame frame, final int successorIndex, final int receiverIndex) {
            super(successorIndex, receiverIndex);
            topNode = FrameStackReadNode.createStackTop(frame);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(FrameAccess.getReceiver(frame), topNode.executeRead(frame));
        }

        @Override
        protected String getTypeName() {
            return "store";
        }
    }

    public static final class StoreIntoRemoteTempNode extends AbstractStoreIntoRemoteTempNode {
        @Child private FrameStackReadNode topNode;

        public StoreIntoRemoteTempNode(final VirtualFrame frame, final int successorIndex, final byte indexInArray, final byte indexOfArray) {
            super(successorIndex, indexInArray, indexOfArray);
            topNode = FrameStackReadNode.createStackTop(frame);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            storeNode.executeWrite(getReadNode(frame).executeRead(frame), topNode.executeRead(frame));
        }

        @Override
        protected String getTypeName() {
            return "store";
        }
    }

    public static final class StoreIntoTemporaryLocationNode extends AbstractStoreIntoTempNode {
        @Child private FrameStackReadNode topNode;

        public StoreIntoTemporaryLocationNode(final VirtualFrame frame, final int successorIndex, final int tempIndex) {
            super(successorIndex, tempIndex);
            topNode = FrameStackReadNode.createStackTop(frame);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            getStoreNode(frame).executeWrite(frame, topNode.executeRead(frame));
        }

        @Override
        protected String getTypeName() {
            return "store";
        }
    }
}
