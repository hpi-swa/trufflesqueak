package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.EnterCodeNode;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.bytecodes.PushBytecodesFactory.PushNewArrayNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.PushBytecodesFactory.PushReceiverNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.PushBytecodesFactory.PushReceiverVariableNodeGen;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPopNReversedNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

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

    @NodeInfo(cost = NodeCost.NONE)
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
            CompilerAsserts.neverPartOfCompilation();
            return "pushThisContext:";
        }
    }

    public static final class PushClosureNode extends AbstractPushNode {
        private final int blockSize;
        private final int numArgs;
        private final int numCopied;

        @Child private StackPopNReversedNode popNReversedNode;
        @Child private GetOrCreateContextNode getOrCreateContextNode;

        @CompilationFinal private CompiledBlockObject block;
        @CompilationFinal private RootCallTarget blockCallTarget;

        private PushClosureNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int i, final int j, final int k) {
            super(code, index, numBytecodes);
            numArgs = i & 0xF;
            numCopied = i >> 4 & 0xF;
            blockSize = j << 8 | k;
            popNReversedNode = StackPopNReversedNode.create(code, numCopied);
            getOrCreateContextNode = GetOrCreateContextNode.create(code);
        }

        public static PushClosureNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int i, final int j, final int k) {
            return new PushClosureNode(code, index, numBytecodes, i, j, k);
        }

        private CompiledBlockObject getBlock(final VirtualFrame frame) {
            if (block == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                block = CompiledBlockObject.create(code, FrameAccess.getMethod(frame), numArgs, numCopied, index + numBytecodes, blockSize);
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
            final Object receiver = FrameAccess.getReceiver(frame);
            final Object[] copiedValues = popNReversedNode.executePopN(frame);
            final ContextObject outerContext = getOrCreateContextNode.executeGet(frame);
            return new BlockClosureObject(getBlock(frame), blockCallTarget, receiver, copiedValues, outerContext);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
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
            CompilerAsserts.neverPartOfCompilation();
            return "pushConstant: " + constant.toString();
        }
    }

    public static final class PushLiteralConstantNode extends AbstractPushNode {
        private final int literalIndex;

        public PushLiteralConstantNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex) {
            super(code, index, numBytecodes);
            this.literalIndex = literalIndex;
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, code.getLiteral(literalIndex));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushConstant: " + code.getLiteral(literalIndex).toString();
        }
    }

    public static final class PushLiteralVariableNode extends AbstractPushNode {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        private final int literalIndex;

        public PushLiteralVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex) {
            super(code, index, numBytecodes);
            this.literalIndex = literalIndex;
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, at0Node.execute(code.getLiteral(literalIndex), 1));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushLit: " + literalIndex;
        }
    }

    public abstract static class PushNewArrayNode extends AbstractPushNode {
        @Child protected StackPopNReversedNode popNReversedNode;
        private final int arraySize;

        protected PushNewArrayNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int param) {
            super(code, index, numBytecodes);
            arraySize = param & 127;
            popNReversedNode = param > 127 ? StackPopNReversedNode.create(code, arraySize) : null;
        }

        public static PushNewArrayNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int param) {
            return PushNewArrayNodeGen.create(code, index, numBytecodes, param);
        }

        @Specialization(guards = {"popNReversedNode != null"})
        protected final void doPushArray(final VirtualFrame frame) {
            pushNode.executeWrite(frame, code.image.asArrayOfObjects(popNReversedNode.executePopN(frame)));
        }

        @Specialization(guards = {"popNReversedNode == null"})
        protected final void doPushNewArray(final VirtualFrame frame) {
            // TODO: createEmptyStrategy?
            pushNode.executeWrite(frame, ArrayObject.createObjectStrategy(code.image, code.image.arrayClass, arraySize));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "push: (Array new: " + arraySize + ")";
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public abstract static class PushReceiverNode extends AbstractPushNode {

        protected PushReceiverNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        public static PushReceiverNode create(final CompiledCodeObject code, final int index) {
            return PushReceiverNodeGen.create(code, index);
        }

        @Specialization
        protected final void doReceiverVirtualized(final VirtualFrame frame) {
            pushNode.executeWrite(frame, FrameAccess.getReceiver(frame));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();

            return "self";
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public abstract static class PushReceiverVariableNode extends AbstractPushNode {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        private final int variableIndex;

        protected PushReceiverVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int varIndex) {
            super(code, index, numBytecodes);
            variableIndex = varIndex;
        }

        public static PushReceiverVariableNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int varIndex) {
            return PushReceiverVariableNodeGen.create(code, index, numBytecodes, varIndex);
        }

        @Specialization
        protected final void doReceiverVirtualized(final VirtualFrame frame) {
            pushNode.executeWrite(frame, at0Node.execute(FrameAccess.getReceiver(frame), variableIndex));
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushRcvr: " + variableIndex;
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class PushRemoteTempNode extends AbstractPushNode {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child private FrameSlotReadNode readTempNode;
        private final int indexInArray;
        private final int indexOfArray;

        public PushRemoteTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int indexInArray, final int indexOfArray) {
            super(code, index, numBytecodes);
            this.indexInArray = indexInArray;
            this.indexOfArray = indexOfArray;
            readTempNode = FrameSlotReadNode.create(code.getStackSlot(indexOfArray));
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, at0Node.execute(readTempNode.executeRead(frame), indexInArray));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushTemp: " + indexInArray + " inVectorAt: " + indexOfArray;
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class PushTemporaryLocationNode extends AbstractBytecodeNode {
        @Child private StackPushNode pushNode;
        @Child private FrameSlotReadNode tempNode;
        private final int tempIndex;

        public PushTemporaryLocationNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int tempIndex) {
            super(code, index, numBytecodes);
            this.tempIndex = tempIndex;
            pushNode = StackPushNode.create(code);
            tempNode = FrameSlotReadNode.create(code.getStackSlot(tempIndex));
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, tempNode.executeRead(frame));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushTemp: " + tempIndex;
        }
    }
}
