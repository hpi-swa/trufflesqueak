package de.hpi.swa.trufflesqueak.nodes.bytecodes;

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
import de.hpi.swa.trufflesqueak.util.FrameAccess;
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
            // current index is pc
            pushNode.executeWrite(frame, getContextNode.executeGet(frame, index));
        }

        @Override
        public String toString() {
            return "pushThisContext:";
        }
    }

    public abstract static class PushClosureNode extends AbstractPushNode {
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

        @Override
        public int executeInt(VirtualFrame frame) {
            executeVoid(frame);
            return index + numBytecodes + blockSize;
        }

        @Specialization(guards = "isVirtualized(frame, code)")
        protected Object doPushVirtualized(VirtualFrame frame) {
            FrameMarker frameMarker = (FrameMarker) FrameAccess.getContextOrMarker(frame, code);
            pushNode.executeWrite(frame, createClosure(frame, null, frameMarker));
            return index + numBytecodes + blockSize;
        }

        @Specialization(guards = "!isVirtualized(frame, code)")
        protected Object doPush(VirtualFrame frame) {
            ContextObject context = (ContextObject) FrameAccess.getContextOrMarker(frame, code);
            pushNode.executeWrite(frame, createClosure(frame, context, null));
            return index + numBytecodes + blockSize;
        }

        private BlockClosureObject createClosure(VirtualFrame frame, ContextObject context, FrameMarker frameMarker) {
            int bytecodeOffset = index + numBytecodes;
            CompiledBlockObject block = new CompiledBlockObject(code, numArgs, numCopied, bytecodeOffset, blockSize);
            Object receiver = receiverNode.executeGeneric(frame);
            Object[] copiedValues = (Object[]) popNReversedNode.executeGeneric(frame);
            return new BlockClosureObject(block, receiver, copiedValues, context, frameMarker);
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
            popNReversedNode = param > 127 ? PopNReversedStackNode.create(code, arraySize) : null;
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            if (popNReversedNode != null) {
                pushNode.executeWrite(frame, code.image.newList((Object[]) popNReversedNode.executeGeneric(frame)));
            } else {
                pushNode.executeWrite(frame, code.image.wrap(new Object[arraySize]));
            }
        }

        @Override
        public String toString() {
            return String.format("push: (Array new: %d)", arraySize);
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
            pushNode = PushStackNode.create(code);
            if (code.getNumStackSlots() <= tempIndex) {
                // sometimes we'll decode more bytecodes than we have slots ... that's fine
            } else {
                tempNode = TemporaryReadNode.create(code, tempIndex);
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
