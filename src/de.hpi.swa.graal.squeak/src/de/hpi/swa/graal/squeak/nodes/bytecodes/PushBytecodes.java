package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.EnterCodeNode;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodes.GetCompiledMethodNode;
import de.hpi.swa.graal.squeak.nodes.context.LiteralConstantNode;
import de.hpi.swa.graal.squeak.nodes.context.MethodLiteralNode;
import de.hpi.swa.graal.squeak.nodes.context.ObjectAtNode;
import de.hpi.swa.graal.squeak.nodes.context.ReceiverNode;
import de.hpi.swa.graal.squeak.nodes.context.TemporaryReadNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPopNReversedNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushNode;

public final class PushBytecodes {

    private abstract static class AbstractPushNode extends AbstractBytecodeNode {
        @Child protected StackPushNode pushNode;

        protected AbstractPushNode(final CompiledCodeObject code, final int index) {
            this(code, index, 1);
        }

        protected AbstractPushNode(final CompiledCodeObject code, final int index, final int numBytecodes) {
            super(code, index, numBytecodes);
            pushNode = StackPushNode.create(code);
        }
    }

    public static final class PushActiveContextNode extends AbstractPushNode {
        @Child private GetOrCreateContextNode getContextNode;

        public PushActiveContextNode(final CompiledCodeObject code, final int index) {
            super(code, index);
            getContextNode = GetOrCreateContextNode.create(code);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, getContextNode.executeGet(frame));
        }

        @Override
        public String toString() {
            return "pushThisContext:";
        }
    }

    public static final class PushClosureNode extends AbstractPushNode {
        protected final int blockSize;
        protected final int numArgs;
        protected final int numCopied;

        @Child private GetOrCreateContextNode getOrCreateContextNode;
        @Child private StackPopNReversedNode popNReversedNode;
        @Child private ReceiverNode receiverNode;
        @Child private GetCompiledMethodNode getMethodNode = GetCompiledMethodNode.create();

        @CompilationFinal private CompiledBlockObject block;
        @CompilationFinal private RootCallTarget blockCallTarget;

        public static PushClosureNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int i, final int j, final int k) {
            return new PushClosureNode(code, index, numBytecodes, i, j, k);
        }

        protected PushClosureNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int i, final int j, final int k) {
            super(code, index, numBytecodes);
            numArgs = i & 0xF;
            numCopied = (i >> 4) & 0xF;
            blockSize = (j << 8) | k;
            getOrCreateContextNode = GetOrCreateContextNode.create(code);
            popNReversedNode = StackPopNReversedNode.create(code, numCopied);
            receiverNode = ReceiverNode.create(code);
        }

        private CompiledBlockObject getBlock() {
            if (block == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                block = CompiledBlockObject.create(code, getMethodNode.execute(code), numArgs, numCopied, index + numBytecodes, blockSize);
                blockCallTarget = Truffle.getRuntime().createCallTarget(EnterCodeNode.create(block.image.getLanguage(), block));
            }
            return block;
        }

        public int getBockSize() {
            return blockSize;
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, createClosure(frame));
        }

        private BlockClosureObject createClosure(final VirtualFrame frame) {
            final Object receiver = receiverNode.executeRead(frame);
            final Object[] copiedValues = (Object[]) popNReversedNode.executeRead(frame);
            final ContextObject thisContext = getOrCreateContextNode.executeGet(frame);
            return new BlockClosureObject(getBlock(), blockCallTarget, receiver, copiedValues, thisContext);
        }

        @Override
        public String toString() {
            final int start = index + numBytecodes;
            final int end = start + blockSize;
            return "closureNumCopied: " + numCopied + " numArgs: " + numArgs + " bytes " + start + " to " + end;
        }
    }

    public static final class PushConstantNode extends AbstractPushNode {
        private final Object constant;

        public PushConstantNode(final CompiledCodeObject code, final int index, final Object obj) {
            super(code, index);
            constant = obj;
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, constant);
        }

        @Override
        public String toString() {
            return "pushConstant: " + constant.toString();
        }
    }

    public static final class PushLiteralConstantNode extends AbstractPushNode {
        @Child private SqueakNode literalNode;
        private final int literalIndex;

        public PushLiteralConstantNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex) {
            super(code, index, numBytecodes);
            this.literalIndex = literalIndex;
            literalNode = new MethodLiteralNode(code, literalIndex);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, literalNode.executeRead(frame));
        }

        @Override
        public String toString() {
            return "pushConstant: " + code.getLiteral(literalIndex).toString();
        }
    }

    public static final class PushLiteralVariableNode extends AbstractPushNode {
        @Child private ObjectAtNode valueNode;
        private final int literalIndex;

        public PushLiteralVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex) {
            super(code, index, numBytecodes);
            this.literalIndex = literalIndex;
            valueNode = ObjectAtNode.create(1, new LiteralConstantNode(code, literalIndex));
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, valueNode.executeGeneric(frame));
        }

        @Override
        public String toString() {
            return "pushLit: " + literalIndex;
        }
    }

    public static final class PushNewArrayNode extends AbstractPushNode {
        @Child private StackPopNReversedNode popNReversedNode;
        private final int arraySize;

        public PushNewArrayNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int param) {
            super(code, index, numBytecodes);
            arraySize = param & 127;
            popNReversedNode = param > 127 ? StackPopNReversedNode.create(code, arraySize) : null;
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            if (popNReversedNode != null) {
                pushNode.executeWrite(frame, code.image.newList(popNReversedNode.executeRead(frame)));
            } else {
                pushNode.executeWrite(frame, ArrayObject.createObjectStrategy(code.image, code.image.arrayClass, arraySize));
            }
        }

        @Override
        public String toString() {
            return "push: (Array new: " + arraySize + ")";
        }
    }

    public static final class PushReceiverNode extends AbstractPushNode {
        @Child private ReceiverNode receiverNode;

        public PushReceiverNode(final CompiledCodeObject code, final int index) {
            super(code, index);
            receiverNode = ReceiverNode.create(code);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, receiverNode.executeRead(frame));
        }

        @Override
        public String toString() {
            return "self";
        }
    }

    public static final class PushReceiverVariableNode extends AbstractPushNode {
        @Child private ObjectAtNode fetchNode;
        private final int variableIndex;

        public PushReceiverVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int varIndex) {
            super(code, index, numBytecodes);
            variableIndex = varIndex;
            fetchNode = ObjectAtNode.create(varIndex, ReceiverNode.create(code));
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, fetchNode.executeGeneric(frame));
        }

        @Override
        public String toString() {
            return "pushRcvr: " + variableIndex;
        }
    }

    public static final class PushRemoteTempNode extends AbstractPushNode {
        @Child private ObjectAtNode remoteTempNode;
        private final int indexInArray;
        private final int indexOfArray;

        public PushRemoteTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int indexInArray, final int indexOfArray) {
            super(code, index, numBytecodes);
            this.indexInArray = indexInArray;
            this.indexOfArray = indexOfArray;
            remoteTempNode = ObjectAtNode.create(indexInArray, TemporaryReadNode.create(code, indexOfArray));
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, remoteTempNode.executeGeneric(frame));
        }

        @Override
        public String toString() {
            return "pushTemp: " + indexInArray + " inVectorAt: " + indexOfArray;
        }
    }

    public static final class PushTemporaryLocationNode extends AbstractBytecodeNode {
        @Child private StackPushNode pushNode;
        @Child private SqueakNode tempNode;
        private final int tempIndex;

        public PushTemporaryLocationNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int tempIndex) {
            super(code, index, numBytecodes);
            this.tempIndex = tempIndex;
            pushNode = StackPushNode.create(code);
            tempNode = TemporaryReadNode.create(code, tempIndex);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, tempNode.executeRead(frame));
        }

        @Override
        public String toString() {
            return "pushTemp: " + this.tempIndex;
        }
    }
}
