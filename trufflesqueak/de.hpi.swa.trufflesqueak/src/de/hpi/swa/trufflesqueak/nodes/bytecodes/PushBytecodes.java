package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.CompiledBlockObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.LiteralConstantNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameTemporaryReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopNReversedStackNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;

public final class PushBytecodes {

    private static abstract class AbstractPushNode extends AbstractBytecodeNode {
        @Child protected PushStackNode pushNode;

        private AbstractPushNode(CompiledCodeObject code, int index) {
            this(code, index, 1);
        }

        private AbstractPushNode(CompiledCodeObject code, int index, int numBytecodes) {
            super(code, index, numBytecodes);
            pushNode = new PushStackNode(code);
        }

        @Override
        public abstract void executeVoid(VirtualFrame frame);
    }

    public static class PushActiveContextNode extends AbstractPushNode {

        public PushActiveContextNode(CompiledCodeObject code, int idx) {
            super(code, idx);
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            pushNode.executeWrite(frame, ContextObject.createReadOnlyContextObject(code.image, frame));
        }

        @Override
        public String toString() {
            return "pushThisContext:";
        }
    }

    public static class PushClosureNode extends AbstractPushNode {
        @CompilationFinal private final int blockSize;
        @CompilationFinal private final int numArgs;
        @CompilationFinal private final int numCopied;
        @CompilationFinal private final CompiledBlockObject compiledBlock;
        @Child private PopNReversedStackNode popNReversedNode;
        @Child private FrameReceiverNode receiverNode = new FrameReceiverNode();

        public PushClosureNode(CompiledCodeObject code, int index, int numBytecodes, int i, int j, int k) {
            super(code, index, numBytecodes);
            this.numArgs = i & 0xF;
            this.numCopied = (i >> 4) & 0xF;
            this.blockSize = (j << 8) | k;
            this.compiledBlock = new CompiledBlockObject(code, numArgs, numCopied);
            popNReversedNode = new PopNReversedStackNode(code, numCopied);
        }

        @Override
        public int executeInt(VirtualFrame frame) {
            executeVoid(frame);
            return index + numBytecodes + blockSize;
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            Object frameMarker = FrameUtil.getObjectSafe(frame, code.markerSlot);
            Object[] copiedValues = popNReversedNode.execute(frame);
            int codeStart = index + numBytecodes;
            int codeEnd = codeStart + blockSize;
            byte[] bytes = Arrays.copyOfRange(code.getBytes(), codeStart, codeEnd);
            compiledBlock.setBytes(bytes);
            pushNode.executeWrite(frame, new BlockClosure(frameMarker, compiledBlock, receiverNode.executeGeneric(frame), copiedValues));
        }

        @Override
        public String toString() {
            return String.format("closureNumCopied: %d numArgs: %d bytes %d to %d", numCopied, numArgs, index + numBytecodes, index + numBytecodes + blockSize);
        }
    }

    public static class PushConstantNode extends AbstractPushNode {
        @CompilationFinal private final Object constant;

        public PushConstantNode(CompiledCodeObject code, int index, Object obj) {
            super(code, index);
            constant = obj;
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            pushNode.executeWrite(frame, constant);
        }

        @Override
        public String toString() {
            return "pushConstant: " + constant.toString();
        }
    }

    public static class PushLiteralConstantNode extends AbstractPushNode {
        @Child private SqueakNode literalNode;
        @CompilationFinal private final int literalIndex;

        public PushLiteralConstantNode(CompiledCodeObject code, int index, int numBytecodes, int literalIndex) {
            super(code, index, numBytecodes);
            this.literalIndex = literalIndex;
            literalNode = new MethodLiteralNode(code, literalIndex);
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            pushNode.executeWrite(frame, literalNode.executeGeneric(frame));
        }

        @Override
        public String toString() {
            return "pushConstant: " + code.getLiteral(literalIndex).toString();
        }
    }

    public static class PushLiteralVariableNode extends AbstractPushNode {
        @Child private ObjectAtNode valueNode;
        @CompilationFinal private final int literalIndex;

        public PushLiteralVariableNode(CompiledCodeObject code, int index, int numBytecodes, int literalIndex) {
            super(code, index, numBytecodes);
            this.literalIndex = literalIndex;
            valueNode = ObjectAtNode.create(1, new LiteralConstantNode(code, literalIndex));
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            pushNode.executeWrite(frame, valueNode.executeGeneric(frame));
        }

        @Override
        public String toString() {
            return "pushLit: " + literalIndex;
        }
    }

    public static class PushNewArrayNode extends AbstractPushNode {
        @Child private PopNReversedStackNode popNReversedNode;
        @CompilationFinal private final int arraySize;

        public PushNewArrayNode(CompiledCodeObject code, int index, int numBytecodes, int param) {
            super(code, index, numBytecodes);
            arraySize = param & 127;
            popNReversedNode = param > 127 ? new PopNReversedStackNode(code, arraySize) : null;
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            Object[] array;
            if (popNReversedNode != null) {
                array = popNReversedNode.execute(frame);
            } else {
                array = new Object[arraySize];
                Arrays.fill(array, code.image.nil); // Initialize with nil
            }
            pushNode.executeWrite(frame, code.image.wrap(array));
        }

        @Override
        public String toString() {
            return String.format("push: (Array new: %d)", arraySize);
        }
    }

    public static class PushReceiverNode extends AbstractPushNode {
        @Child private FrameReceiverNode receiverNode = new FrameReceiverNode();

        public PushReceiverNode(CompiledCodeObject code, int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            pushNode.executeWrite(frame, receiverNode.executeGeneric(frame));
        }

        @Override
        public String toString() {
            return "self";
        }
    }

    public static class PushReceiverVariableNode extends AbstractPushNode {
        @Child private ObjectAtNode fetchNode;
        @CompilationFinal private final int variableIndex;

        public PushReceiverVariableNode(CompiledCodeObject code, int index, int numBytecodes, int varIndex) {
            super(code, index, numBytecodes);
            variableIndex = varIndex;
            fetchNode = ObjectAtNode.create(varIndex, new FrameReceiverNode());
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            pushNode.executeWrite(frame, fetchNode.executeGeneric(frame));
        }

        @Override
        public String toString() {
            return "pushRcvr: " + variableIndex;
        }
    }

    public static class PushRemoteTempNode extends AbstractPushNode {
        @Child private ObjectAtNode remoteTempNode;
        @CompilationFinal private final int indexInArray;
        @CompilationFinal private final int indexOfArray;

        public PushRemoteTempNode(CompiledCodeObject code, int index, int numBytecodes, int indexInArray, int indexOfArray) {
            super(code, index, numBytecodes);
            this.indexInArray = indexInArray;
            this.indexOfArray = indexOfArray;
            remoteTempNode = ObjectAtNode.create(indexInArray, FrameTemporaryReadNode.create(code, indexOfArray));
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            pushNode.executeWrite(frame, remoteTempNode.executeGeneric(frame));
        }

        @Override
        public String toString() {
            return String.format("pushTemp: %d inVectorAt: %d", this.indexInArray, this.indexOfArray);
        }
    }

    public static class PushTemporaryLocationNode extends AbstractBytecodeNode {
        @Child private PushStackNode pushNode;
        @Child private SqueakNode tempNode;
        @CompilationFinal private final int tempIndex;

        public PushTemporaryLocationNode(CompiledCodeObject code, int index, int numBytecodes, int tempIndex) {
            super(code, index, numBytecodes);
            this.tempIndex = tempIndex;
            pushNode = new PushStackNode(code);
            if (code.getNumStackSlots() <= tempIndex) {
                // sometimes we'll decode more bytecodes than we have slots ... that's fine
            } else {
                tempNode = FrameTemporaryReadNode.create(code, tempIndex);
            }
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            pushNode.executeWrite(frame, tempNode.executeGeneric(frame));
        }

        @Override
        public String toString() {
            return "pushTemp: " + this.tempIndex;
        }
    }
}
