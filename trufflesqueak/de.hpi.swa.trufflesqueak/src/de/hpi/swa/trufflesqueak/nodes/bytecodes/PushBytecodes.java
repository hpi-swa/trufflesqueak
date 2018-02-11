package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledBlockObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodesFactory.PushClosureNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.LiteralConstantNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNode;
import de.hpi.swa.trufflesqueak.nodes.context.ReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.TemporaryReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopNReversedStackNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

public final class PushBytecodes {

    private static abstract class AbstractPushNode extends AbstractBytecodeNode {
        @Child protected PushStackNode pushNode;

        protected AbstractPushNode(CompiledCodeObject code, int index) {
            this(code, index, 1);
        }

        protected AbstractPushNode(CompiledCodeObject code, int index, int numBytecodes) {
            super(code, index, numBytecodes);
            pushNode = PushStackNode.create(code);
        }
    }

    public static class PushActiveContextNode extends AbstractPushNode {
        @Child private GetOrCreateContextNode getContextNode;

        public PushActiveContextNode(CompiledCodeObject code, int index) {
            super(code, index);
            getContextNode = GetOrCreateContextNode.create(code);
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            pushNode.executeWrite(frame, getContextNode.executeGet(frame, true));
        }

        @Override
        public String toString() {
            return "pushThisContext:";
        }
    }

    public static abstract class PushClosureNode extends AbstractPushNode {
        @CompilationFinal protected CompiledBlockObject block;
        @CompilationFinal protected final int blockSize;
        @CompilationFinal protected final int numArgs;
        @CompilationFinal protected final int numCopied;
        @Child protected PopNReversedStackNode popNReversedNode;
        @Child protected ReceiverNode receiverNode;

        public static PushClosureNode create(CompiledCodeObject code, int index, int numBytecodes, int i, int j, int k) {
            return PushClosureNodeGen.create(code, index, numBytecodes, i, j, k);
        }

        protected PushClosureNode(CompiledCodeObject code, int index, int numBytecodes, int i, int j, int k) {
            super(code, index, numBytecodes);
            numArgs = i & 0xF;
            numCopied = (i >> 4) & 0xF;
            blockSize = (j << 8) | k;
            popNReversedNode = PopNReversedStackNode.create(code, numCopied);
            receiverNode = ReceiverNode.create(code);

        }

        private CompiledBlockObject getBlock() {
            if (block == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                block = CompiledBlockObject.create(code, numArgs, numCopied, index + numBytecodes, blockSize);
            }
            return block;
        }

        @Override
        public int getSuccessorIndex() {
            return index + numBytecodes + blockSize;
        }

        @Specialization(guards = "isVirtualized(frame)")
        protected int doPushVirtualized(VirtualFrame frame) {
            CompilerDirectives.ensureVirtualizedHere(frame);
            pushNode.executeWrite(frame, createClosure(frame, null, getFrameMarker(frame)));
            return getSuccessorIndex();
        }

        @Specialization(guards = "!isVirtualized(frame)")
        protected int doPush(VirtualFrame frame) {
            pushNode.executeWrite(frame, createClosure(frame, getContext(frame), null));
            return getSuccessorIndex();
        }

        private BlockClosureObject createClosure(VirtualFrame frame, ContextObject context, FrameMarker frameMarker) {
            Object receiver = receiverNode.executeRead(frame);
            Object[] copiedValues = (Object[]) popNReversedNode.executeRead(frame);
            return new BlockClosureObject(getBlock(), receiver, copiedValues, context, frameMarker);
        }

        @Override
        public String toString() {
            int start = index + numBytecodes;
            int end = start + blockSize;
            return "closureNumCopied: " + numCopied + " numArgs: " + numArgs + " bytes " + start + " to " + end;
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
            pushNode.executeWrite(frame, literalNode.executeRead(frame));
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
            popNReversedNode = param > 127 ? PopNReversedStackNode.create(code, arraySize) : null;
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            if (popNReversedNode != null) {
                pushNode.executeWrite(frame, code.image.newList((Object[]) popNReversedNode.executeRead(frame)));
            } else {
                Object[] result = new Object[arraySize];
                Arrays.fill(result, code.image.nil);
                pushNode.executeWrite(frame, code.image.newList(result));
            }
        }

        @Override
        public String toString() {
            return "push: (Array new: " + arraySize + ")";
        }
    }

    public static class PushReceiverNode extends AbstractPushNode {
        @Child private ReceiverNode receiverNode;

        public PushReceiverNode(CompiledCodeObject code, int index) {
            super(code, index);
            receiverNode = ReceiverNode.create(code);
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            pushNode.executeWrite(frame, receiverNode.executeRead(frame));
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
            fetchNode = ObjectAtNode.create(varIndex, ReceiverNode.create(code));
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
            remoteTempNode = ObjectAtNode.create(indexInArray, TemporaryReadNode.create(code, indexOfArray));
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            pushNode.executeWrite(frame, remoteTempNode.executeGeneric(frame));
        }

        @Override
        public String toString() {
            return "pushTemp: " + indexInArray + " inVectorAt: " + indexOfArray;
        }
    }

    public static class PushTemporaryLocationNode extends AbstractBytecodeNode {
        @Child private PushStackNode pushNode;
        @Child private SqueakNode tempNode;
        @CompilationFinal private final int tempIndex;

        public PushTemporaryLocationNode(CompiledCodeObject code, int index, int numBytecodes, int tempIndex) {
            super(code, index, numBytecodes);
            this.tempIndex = tempIndex;
            pushNode = PushStackNode.create(code);
            if (tempIndex <= code.getNumStackSlots()) { // for decoder
                tempNode = TemporaryReadNode.create(code, tempIndex);
            }
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            pushNode.executeWrite(frame, tempNode.executeRead(frame));
        }

        @Override
        public String toString() {
            return "pushTemp: " + this.tempIndex;
        }
    }
}
