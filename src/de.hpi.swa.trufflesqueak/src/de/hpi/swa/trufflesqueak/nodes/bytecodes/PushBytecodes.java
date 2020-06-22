/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledBlockObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.nodes.SqueakProfiles.SqueakProfile;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodesFactory.PushNewArrayNodeFactory.ArrayFromStackNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodesFactory.PushNewArrayNodeFactory.CreateNewArrayNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPopNNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPushNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class PushBytecodes {

    private abstract static class AbstractPushNode extends AbstractInstrumentableBytecodeNode {
        @Child protected FrameStackPushNode pushNode = FrameStackPushNode.create();

        protected AbstractPushNode(final CompiledCodeObject code, final int index) {
            this(code, index, 1);
        }

        protected AbstractPushNode(final CompiledCodeObject code, final int index, final int numBytecodes) {
            super(code, index, numBytecodes);
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class PushActiveContextNode extends AbstractPushNode {
        @Child private GetOrCreateContextNode getContextNode = GetOrCreateContextNode.create(true);

        public PushActiveContextNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, getContextNode.executeGet(frame));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushThisContext:";
        }
    }

    @GenerateWrapper
    public static class PushClosureNode extends AbstractBytecodeNode implements InstrumentableNode {
        public static final int NUM_BYTECODES = 4;

        private final int blockSize;
        private final int numArgs;
        private final int numCopied;
        @CompilationFinal private CompiledBlockObject cachedBlock;
        @CompilationFinal private int cachedStartPC;

        @Child private FrameStackPopNNode popNNode;
        @Child protected FrameStackPushNode pushNode = FrameStackPushNode.create();
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create(true);

        private PushClosureNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int i, final int j, final int k) {
            super(code, index, numBytecodes);
            numArgs = i & 0xF;
            numCopied = i >> 4 & 0xF;
            blockSize = j << 8 | k;
            popNNode = FrameStackPopNNode.create(numCopied);
        }

        public PushClosureNode(final PushClosureNode node) {
            super(node.code, node.index, node.numBytecodes);
            numArgs = node.numArgs;
            numCopied = node.numCopied;
            blockSize = node.blockSize;
            popNNode = FrameStackPopNNode.create(numCopied);
        }

        public static PushClosureNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int i, final int j, final int k) {
            return new PushClosureNode(code, index, numBytecodes, i, j, k);
        }

        private CompiledBlockObject getBlock(final VirtualFrame frame) {
            if (cachedBlock == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cachedBlock = code.findBlock(FrameAccess.getMethod(frame), numArgs, numCopied, getSuccessorIndex(), blockSize);
                cachedStartPC = cachedBlock.getInitialPC();
            }
            return cachedBlock;
        }

        public int getBockSize() {
            return blockSize;
        }

        public int getClosureSuccessorIndex() {
            return getSuccessorIndex() + getBockSize();
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            throw SqueakException.create("Should never be called directly.");
        }

        public void executePush(final VirtualFrame frame) {
            pushNode.execute(frame, createClosure(frame));
        }

        private BlockClosureObject createClosure(final VirtualFrame frame) {
            final Object[] copiedValues = popNNode.execute(frame);
            final ContextObject outerContext = getOrCreateContextNode.executeGet(frame);
            return new BlockClosureObject(code.image, getBlock(frame), cachedStartPC, numArgs, copiedValues, outerContext);
        }

        @Override
        public final boolean isInstrumentable() {
            return true;
        }

        @Override
        public WrapperNode createWrapper(final ProbeNode probe) {
            return new PushClosureNodeWrapper(this, this, probe);
        }

        @Override
        public boolean hasTag(final Class<? extends Tag> tag) {
            return tag == StandardTags.StatementTag.class;
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            final int start = index + numBytecodes;
            final int end = start + blockSize;
            return "closureNumCopied: " + numCopied + " numArgs: " + numArgs + " bytes " + start + " to " + end;
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class PushConstantNode extends AbstractPushNode {
        private final Object constant;

        public PushConstantNode(final CompiledCodeObject code, final int index, final Object obj) {
            super(code, index);
            constant = obj;
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, constant);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushConstant: " + constant.toString();
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class PushLiteralConstantNode extends AbstractPushNode {
        private final int literalIndex;

        public PushLiteralConstantNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex) {
            super(code, index, numBytecodes);
            this.literalIndex = literalIndex;
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, code.getLiteral(literalIndex));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushConstant: " + code.getLiteral(literalIndex).toString();
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class PushLiteralVariableNode extends AbstractPushNode {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        private final SqueakProfile valueProfile;
        private final int literalIndex;

        public PushLiteralVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex) {
            super(code, index, numBytecodes);
            this.literalIndex = literalIndex;
            valueProfile = SqueakProfile.createLiteralProfile(code.getLiteral(literalIndex));
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, valueProfile.profile(at0Node.execute(code.getLiteral(literalIndex), ASSOCIATION.VALUE)));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushLitVar: " + code.getLiteral(literalIndex);
        }
    }

    public static final class PushNewArrayNode extends AbstractPushNode {
        @Child protected ArrayNode arrayNode;

        protected PushNewArrayNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int param) {
            super(code, index, numBytecodes);
            final int arraySize = param & 127;
            arrayNode = param > 127 ? ArrayFromStackNodeGen.create(arraySize) : CreateNewArrayNodeGen.create(arraySize);
        }

        public static PushNewArrayNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int param) {
            return new PushNewArrayNode(code, index, numBytecodes, param);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, arrayNode.execute(frame));
        }

        protected abstract static class ArrayNode extends Node {
            protected final int arraySize;

            public ArrayNode(final int arraySize) {
                this.arraySize = arraySize;
            }

            protected abstract ArrayObject execute(VirtualFrame frame);
        }

        protected abstract static class ArrayFromStackNode extends ArrayNode {
            @Child protected FrameStackPopNNode popNNode;

            public ArrayFromStackNode(final int arraySize) {
                super(arraySize);
                popNNode = FrameStackPopNNode.create(arraySize);
            }

            @Specialization
            protected final ArrayObject doPopN(final VirtualFrame frame, @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
                /**
                 * Pushing an ArrayObject with object strategy. Contents likely to be mixed values
                 * and therefore unlikely to benefit from storage strategy.
                 */
                return image.asArrayOfObjects(popNNode.execute(frame));
            }
        }

        protected abstract static class CreateNewArrayNode extends ArrayNode {
            public CreateNewArrayNode(final int arraySize) {
                super(arraySize);
            }

            @Specialization
            protected final ArrayObject doFresh(@CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
                /**
                 * Pushing an ArrayObject with object strategy. Contents likely to be mixed values
                 * and therefore unlikely to benefit from storage strategy.
                 */
                return ArrayObject.createObjectStrategy(image, image.arrayClass, arraySize);
            }
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "push: (Array new: " + arrayNode.arraySize + ")";
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class PushReceiverNode extends AbstractPushNode {

        protected PushReceiverNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        public static PushReceiverNode create(final CompiledCodeObject code, final int index) {
            return new PushReceiverNode(code, index);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, FrameAccess.getReceiver(frame));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "self";
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class PushReceiverVariableNode extends AbstractPushNode {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        private final int variableIndex;

        protected PushReceiverVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int varIndex) {
            super(code, index, numBytecodes);
            variableIndex = varIndex;
        }

        public static PushReceiverVariableNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int varIndex) {
            return new PushReceiverVariableNode(code, index, numBytecodes, varIndex);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, at0Node.execute(FrameAccess.getReceiver(frame), variableIndex));
        }

        @Override
        public String toString() {
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
            pushNode.execute(frame, at0Node.execute(readTempNode.executeRead(frame), indexInArray));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushTemp: " + indexInArray + " inVectorAt: " + indexOfArray;
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class PushTemporaryLocationNode extends AbstractInstrumentableBytecodeNode {
        @Child private FrameStackPushNode pushNode = FrameStackPushNode.create();
        @Child private FrameSlotReadNode tempNode;
        private final int tempIndex;

        public PushTemporaryLocationNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int tempIndex) {
            super(code, index, numBytecodes);
            this.tempIndex = tempIndex;
            tempNode = FrameSlotReadNode.create(code.getStackSlot(tempIndex));
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, tempNode.executeRead(frame));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushTemp: " + tempIndex;
        }
    }
}
