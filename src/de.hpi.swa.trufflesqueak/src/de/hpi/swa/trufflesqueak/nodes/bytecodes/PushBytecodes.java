/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodesFactory.PushLiteralVariableNodeFactory.PushLiteralVariableReadonlyNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodesFactory.PushLiteralVariableNodeFactory.PushLiteralVariableWritableNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodesFactory.PushReceiverVariableNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodesFactory.PushRemoteTempNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPopNNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPopNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPushNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextWithFrameNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
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

    private abstract static class AbstractPushClosureNode extends AbstractInstrumentableBytecodeNode {
        @Child private FrameStackPushNode pushNode = FrameStackPushNode.create();
        @Child protected FrameStackPopNNode popCopiedValuesNode;

        private AbstractPushClosureNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int numCopied) {
            super(code, index, numBytecodes);
            popCopiedValuesNode = FrameStackPopNNode.create(numCopied);
        }

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, createClosure(frame, popCopiedValuesNode.execute(frame)));
        }

        protected abstract BlockClosureObject createClosure(VirtualFrame frame, Object[] copiedValues);

        protected final int getNumCopied() {
            CompilerAsserts.neverPartOfCompilation();
            return popCopiedValuesNode.numPop();
        }
    }

    public static final class PushClosureNode extends AbstractPushClosureNode {
        public static final int NUM_BYTECODES = 4;

        private final CompiledCodeObject shadowBlock;
        private final int numArgs;

        private final int blockSize;

        @Child private GetOrCreateContextWithFrameNode getOrCreateContextNode = GetOrCreateContextWithFrameNode.create();

        private PushClosureNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int numArgs, final int numCopied, final int blockSize) {
            super(code, index, numBytecodes + blockSize, numCopied);
            this.numArgs = numArgs;
            this.blockSize = blockSize;
            shadowBlock = code.getOrCreateShadowBlock(getSuccessorIndex() - blockSize);
        }

        public static PushClosureNode create(final CompiledCodeObject code, final int index, final byte i, final byte j, final byte k) {
            final int numArgs = i & 0xF;
            final int numCopied = Byte.toUnsignedInt(i) >> 4 & 0xF;
            final int blockSize = Byte.toUnsignedInt(j) << 8 | Byte.toUnsignedInt(k);
            return new PushClosureNode(code, index, NUM_BYTECODES, numArgs, numCopied, blockSize);
        }

        public static PushClosureNode createExtended(final CompiledCodeObject code, final int index, final int numBytecodes, final int extA, final int extB, final byte byteA, final byte byteB) {
            final int numArgs = (byteA & 7) + Math.floorMod(extA, 16) * 8;
            final int numCopied = (Byte.toUnsignedInt(byteA) >> 3 & 0x7) + Math.floorDiv(extA, 16) * 8;
            final int blockSize = Byte.toUnsignedInt(byteB) + (extB << 8);
            return new PushClosureNode(code, index, numBytecodes, numArgs, numCopied, blockSize);
        }

        @Override
        protected BlockClosureObject createClosure(final VirtualFrame frame, final Object[] copiedValues) {
            final ContextObject outerContext = getOrCreateContextNode.executeGet(frame);
            final int startPC = getSuccessorIndex() - blockSize;
            final SqueakImageContext image = getContext();
            return new BlockClosureObject(image, image.blockClosureClass, shadowBlock, startPC, numArgs, copiedValues, FrameAccess.getReceiver(frame), outerContext);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            final int start = getSuccessorIndex();
            final int end = start + blockSize;
            return "closureNumCopied: " + getNumCopied() + " numArgs: " + numArgs + " bytes " + start + " to " + end;
        }
    }

    public abstract static class AbstractPushFullClosureNode extends AbstractPushClosureNode {
        private final int literalIndex;
        private final CompiledCodeObject block;
        private final int blockInitialPC;
        private final int blockNumArgs;

        protected AbstractPushFullClosureNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int numCopied) {
            super(code, index, numBytecodes, numCopied);
            this.literalIndex = literalIndex;
            block = (CompiledCodeObject) code.getLiteral(literalIndex);
            blockInitialPC = block.getInitialPC();
            blockNumArgs = block.getNumArgs();
        }

        public static AbstractPushFullClosureNode createExtended(final CompiledCodeObject code, final int index, final int numBytecodes, final int extA, final byte byteA, final byte byteB) {
            final int literalIndex = Byte.toUnsignedInt(byteA) + (extA << 8);
            final int numCopied = Byte.toUnsignedInt(byteB) & 63;
            final boolean ignoreOuterContext = (byteB >> 6 & 1) == 1;
            final boolean receiverOnStack = (byteB >> 7 & 1) == 1;
            if (receiverOnStack) {
                if (ignoreOuterContext) {
                    return new PushFullClosureOnStackReceiverIgnoreOuterContextNode(code, index, numBytecodes, literalIndex, numCopied);
                } else {
                    return new PushFullClosureOnStackReceiverWithOuterContextNode(code, index, numBytecodes, literalIndex, numCopied);
                }
            } else {
                if (ignoreOuterContext) {
                    return new PushFullClosureFrameReceiverIgnoreOuterContextNode(code, index, numBytecodes, literalIndex, numCopied);
                } else {
                    return new PushFullClosureFrameReceiverWithOuterContextNode(code, index, numBytecodes, literalIndex, numCopied);
                }
            }
        }

        protected final BlockClosureObject createClosure(final Object[] copiedValues, final Object receiver, final ContextObject context) {
            final SqueakImageContext image = getContext();
            return new BlockClosureObject(image, image.getFullBlockClosureClass(), block, blockInitialPC, blockNumArgs, copiedValues, receiver, context);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushFullClosure: (self literalAt: " + literalIndex + ") numCopied: " + getNumCopied() + " numArgs: " + block.getNumArgs();
        }

        private static final class PushFullClosureOnStackReceiverWithOuterContextNode extends AbstractPushFullClosureNode {
            @Child private GetOrCreateContextWithFrameNode getOrCreateContextNode = GetOrCreateContextWithFrameNode.create();
            @Child private FrameStackPopNode popReceiverNode = FrameStackPopNode.create();

            private PushFullClosureOnStackReceiverWithOuterContextNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int numCopied) {
                super(code, index, numBytecodes, literalIndex, numCopied);
            }

            @Override
            protected BlockClosureObject createClosure(final VirtualFrame frame, final Object[] copiedValues) {
                final Object receiver = popReceiverNode.execute(frame);
                final ContextObject context = getOrCreateContextNode.executeGet(frame);
                return createClosure(copiedValues, receiver, context);
            }
        }

        private static final class PushFullClosureFrameReceiverWithOuterContextNode extends AbstractPushFullClosureNode {
            @Child private GetOrCreateContextWithFrameNode getOrCreateContextNode = GetOrCreateContextWithFrameNode.create();

            private PushFullClosureFrameReceiverWithOuterContextNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int numCopied) {
                super(code, index, numBytecodes, literalIndex, numCopied);
            }

            @Override
            protected BlockClosureObject createClosure(final VirtualFrame frame, final Object[] copiedValues) {
                final Object receiver = FrameAccess.getReceiver(frame);
                final ContextObject context = getOrCreateContextNode.executeGet(frame);
                return createClosure(copiedValues, receiver, context);
            }
        }

        private static final class PushFullClosureOnStackReceiverIgnoreOuterContextNode extends AbstractPushFullClosureNode {
            @Child private FrameStackPopNode popReceiverNode = FrameStackPopNode.create();

            private PushFullClosureOnStackReceiverIgnoreOuterContextNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int numCopied) {
                super(code, index, numBytecodes, literalIndex, numCopied);
            }

            @Override
            protected BlockClosureObject createClosure(final VirtualFrame frame, final Object[] copiedValues) {
                final Object receiver = popReceiverNode.execute(frame);
                return createClosure(copiedValues, receiver, null);
            }
        }

        private static final class PushFullClosureFrameReceiverIgnoreOuterContextNode extends AbstractPushFullClosureNode {
            private PushFullClosureFrameReceiverIgnoreOuterContextNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int numCopied) {
                super(code, index, numBytecodes, literalIndex, numCopied);
            }

            @Override
            protected BlockClosureObject createClosure(final VirtualFrame frame, final Object[] copiedValues) {
                final Object receiver = FrameAccess.getReceiver(frame);
                return createClosure(copiedValues, receiver, null);
            }
        }
    }

    public static final class PushActiveContextNode extends AbstractPushNode {
        @Child private GetOrCreateContextWithFrameNode getContextNode = GetOrCreateContextWithFrameNode.create();

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

    private abstract static class PushConstantNode extends AbstractPushNode {
        private PushConstantNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        protected abstract Object getConstant();

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, getConstant());
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushConstant: " + getConstant().toString();
        }
    }

    public static final class PushConstantTrueNode extends PushConstantNode {
        public PushConstantTrueNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        protected Object getConstant() {
            return BooleanObject.TRUE;
        }
    }

    public static final class PushConstantFalseNode extends PushConstantNode {
        public PushConstantFalseNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        protected Object getConstant() {
            return BooleanObject.FALSE;
        }
    }

    public static final class PushConstantNilNode extends PushConstantNode {
        public PushConstantNilNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        protected Object getConstant() {
            return NilObject.SINGLETON;
        }
    }

    public static final class PushConstantMinusOneNode extends PushConstantNode {
        public PushConstantMinusOneNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        protected Object getConstant() {
            return -1L;
        }
    }

    public static final class PushConstantZeroNode extends PushConstantNode {
        public PushConstantZeroNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        protected Object getConstant() {
            return 0L;
        }
    }

    public static final class PushConstantOneNode extends PushConstantNode {
        public PushConstantOneNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        protected Object getConstant() {
            return 1L;
        }
    }

    public static final class PushConstantTwoNode extends PushConstantNode {
        public PushConstantTwoNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        protected Object getConstant() {
            return 2L;
        }
    }

    public static final class PushLiteralConstantNode extends AbstractPushNode {
        private final Object literal;

        public PushLiteralConstantNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex) {
            super(code, index, numBytecodes);
            literal = code.getLiteral(literalIndex);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, literal);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushConstant: " + literal;
        }
    }

    public abstract static class PushLiteralVariableNode extends AbstractInstrumentableBytecodeNode {
        private static final String[] READONLY_CLASSES = {"ClassBinding", "ReadOnlyVariableBinding"};
        protected final AbstractSqueakObjectWithClassAndHash literal;

        protected PushLiteralVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final AbstractSqueakObjectWithClassAndHash literal) {
            super(code, index, numBytecodes);
            this.literal = literal;
        }

        public static final AbstractInstrumentableBytecodeNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex) {
            final Object literal = code.getLiteral(literalIndex);
            if (literal instanceof final AbstractSqueakObjectWithClassAndHash l) {
                final String squeakClassName = l.getSqueakClassName();
                if (ArrayUtils.containsEqual(READONLY_CLASSES, squeakClassName)) {
                    return PushLiteralVariableReadonlyNodeGen.create(code, index, numBytecodes, l);
                } else {
                    return PushLiteralVariableWritableNodeGen.create(code, index, numBytecodes, l);
                }
            } else {
                throw SqueakException.create("Unexpected literal", literal);
            }
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushLitVar: " + literal;
        }

        public abstract static class PushLiteralVariableReadonlyNode extends PushLiteralVariableNode {
            private final Object pushValue;

            protected PushLiteralVariableReadonlyNode(final CompiledCodeObject code, final int index, final int numBytecodes, final AbstractSqueakObjectWithClassAndHash literal) {
                super(code, index, numBytecodes, literal);
                pushValue = getPushValue(literal);
            }

            @Specialization
            protected final void doPushLiteralVariable(final VirtualFrame frame,
                            @Cached final FrameStackPushNode pushNode) {
                assert pushValue == getPushValue(literal) : "value of binding changed unexpectedly";
                pushNode.execute(frame, pushValue);
            }

            private static Object getPushValue(final Object literal) {
                CompilerAsserts.neverPartOfCompilation();
                return SqueakObjectAt0Node.executeUncached(literal, ASSOCIATION.VALUE);
            }
        }

        @SuppressWarnings("truffle-inlining")
        public abstract static class PushLiteralVariableWritableNode extends PushLiteralVariableNode {
            protected PushLiteralVariableWritableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final AbstractSqueakObjectWithClassAndHash literal) {
                super(code, index, numBytecodes, literal);
            }

            @Specialization
            protected final void doPushLiteralVariable(final VirtualFrame frame,
                            @Bind final Node node,
                            @Cached final SqueakObjectAt0Node at0Node,
                            @Cached("createIdentityProfile()") final ValueProfile profile,
                            @Cached final FrameStackPushNode pushNode) {
                pushNode.execute(frame, profile.profile(at0Node.execute(node, literal, ASSOCIATION.VALUE)));
            }
        }
    }

    public static final class PushNewArrayNode extends AbstractPushNode {
        @Child private ArrayNode arrayNode;

        protected PushNewArrayNode(final CompiledCodeObject code, final int index, final int numBytecodes, final byte param) {
            super(code, index, numBytecodes);
            final int arraySize = param & 127;
            arrayNode = param < 0 ? new ArrayFromStackNode(arraySize) : new CreateNewArrayNode(arraySize);
        }

        public static PushNewArrayNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final byte param) {
            return new PushNewArrayNode(code, index, numBytecodes, param);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, arrayNode.execute(frame));
        }

        protected abstract static class ArrayNode extends AbstractNode {
            protected final int arraySize;

            public ArrayNode(final int arraySize) {
                this.arraySize = arraySize;
            }

            protected abstract ArrayObject execute(VirtualFrame frame);
        }

        protected static final class ArrayFromStackNode extends ArrayNode {
            @Child private FrameStackPopNNode popNNode;

            public ArrayFromStackNode(final int arraySize) {
                super(arraySize);
                popNNode = FrameStackPopNNode.create(arraySize);
            }

            @Override
            protected ArrayObject execute(final VirtualFrame frame) {
                /**
                 * Pushing an ArrayObject with object strategy. Contents likely to be mixed values
                 * and therefore unlikely to benefit from storage strategy.
                 */
                return getContext().asArrayOfObjects(popNNode.execute(frame));
            }
        }

        protected static final class CreateNewArrayNode extends ArrayNode {
            public CreateNewArrayNode(final int arraySize) {
                super(arraySize);
            }

            @Override
            protected ArrayObject execute(final VirtualFrame frame) {
                /**
                 * Pushing an ArrayObject with object strategy. Contents likely to be mixed values
                 * and therefore unlikely to benefit from storage strategy.
                 */
                final SqueakImageContext image = getContext();
                return ArrayObject.createObjectStrategy(image, image.arrayClass, arraySize);
            }
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "push: (Array new: " + arrayNode.arraySize + ")";
        }
    }

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

    public abstract static class PushReceiverVariableNode extends AbstractInstrumentableBytecodeNode {
        private final int variableIndex;

        protected PushReceiverVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int varIndex) {
            super(code, index, numBytecodes);
            variableIndex = varIndex;
        }

        public static PushReceiverVariableNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int varIndex) {
            return PushReceiverVariableNodeGen.create(code, index, numBytecodes, varIndex);
        }

        @Specialization
        protected final void doPushReceiver(final VirtualFrame frame,
                        @Bind final Node node,
                        @Cached final SqueakObjectAt0Node at0Node,
                        @Cached final FrameStackPushNode pushNode) {
            pushNode.execute(frame, at0Node.execute(node, FrameAccess.getReceiver(frame), variableIndex));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushRcvr: " + variableIndex;
        }
    }

    public abstract static class PushRemoteTempNode extends AbstractInstrumentableBytecodeNode {
        protected final int indexInArray;
        protected final int indexOfArray;

        public PushRemoteTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final byte indexInArray, final byte indexOfArray) {
            super(code, index, numBytecodes);
            this.indexInArray = Byte.toUnsignedInt(indexInArray);
            this.indexOfArray = Byte.toUnsignedInt(indexOfArray);
        }

        public static PushRemoteTempNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final byte indexInArray, final byte indexOfArray) {
            return PushRemoteTempNodeGen.create(code, index, numBytecodes, indexInArray, indexOfArray);
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization
        protected final void doPushRemoteTemp(final VirtualFrame frame,
                        @Bind final Node node,
                        @Cached("create(frame, indexOfArray, false)") final FrameStackReadNode readTempNode,
                        @Cached final SqueakObjectAt0Node at0Node,
                        @Cached final FrameStackPushNode pushNode) {
            pushNode.execute(frame, at0Node.execute(node, readTempNode.executeRead(frame), indexInArray));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushTemp: " + indexInArray + " inVectorAt: " + indexOfArray;
        }
    }

    public static final class PushTemporaryLocationNode extends AbstractInstrumentableBytecodeNode {
        @Child private FrameStackPushNode pushNode = FrameStackPushNode.create();
        @Child private FrameStackReadNode tempNode;
        private final int tempIndex;

        public PushTemporaryLocationNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int tempIndex) {
            super(code, index, numBytecodes);
            this.tempIndex = tempIndex;
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            if (tempNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                tempNode = insert(FrameStackReadNode.create(frame, tempIndex, false));
            }
            pushNode.execute(frame, tempNode.executeRead(frame));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushTemp: " + tempIndex;
        }
    }

    public static final class PushSmallIntegerNode extends AbstractPushNode {
        private final long value;

        public PushSmallIntegerNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int value) {
            super(code, index, numBytecodes);
            this.value = value;
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, value);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushConstant: " + value;
        }
    }

    public static final class PushCharacterNode extends AbstractPushNode {
        private final Object value;

        public PushCharacterNode(final CompiledCodeObject code, final int index, final int numBytecodes, final long value) {
            super(code, index, numBytecodes);
            this.value = CharacterObject.valueOf(value);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, value);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushConstant: $" + value;
        }
    }
}
