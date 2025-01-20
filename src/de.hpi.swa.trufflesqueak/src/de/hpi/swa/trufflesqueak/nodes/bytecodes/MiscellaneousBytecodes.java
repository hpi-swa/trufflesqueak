/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.MiscellaneousBytecodesFactory.CallPrimitiveNodeFactory.GetErrorObjectFromPrimFailCodeNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodes.PushLiteralConstantNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodes.PushLiteralVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodes.PushReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodes.PushTemporaryLocationNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.SelfSendNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.SuperSendNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreBytecodes.PopIntoLiteralVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreBytecodes.PopIntoReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreBytecodes.PopIntoTemporaryLocationNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreBytecodes.StoreIntoLiteralVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreBytecodes.StoreIntoReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreBytecodes.StoreIntoTemporaryLocationNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPopNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPushNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackTopNode;

public final class MiscellaneousBytecodes {

    public static final class CallPrimitiveNode extends AbstractBytecodeNode {
        public static final int NUM_BYTECODES = 3;
        @Child private FrameStackPushNode pushNode;
        @Child private GetErrorObjectFromPrimFailCodeNode getErrorObjectNode;

        public CallPrimitiveNode(final CompiledCodeObject method, final int index, final int primitiveIndex) {
            super(method, index, NUM_BYTECODES);
            assert method.hasPrimitive() && method.primitiveIndex() == primitiveIndex;
            if (method.hasStoreIntoTemp1AfterCallPrimitive()) {
                pushNode = FrameStackPushNode.create();
                getErrorObjectNode = GetErrorObjectFromPrimFailCodeNodeGen.create();
            }
        }

        public static CallPrimitiveNode create(final CompiledCodeObject code, final int index, final byte byte1, final byte byte2) {
            return new CallPrimitiveNode(code, index, Byte.toUnsignedInt(byte1) + (Byte.toUnsignedInt(byte2) << 8));
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            // primitives handled specially
            if (pushNode != null) {
                pushNode.execute(frame, getErrorObjectNode.execute(getContext().getPrimFailCode()));
            }
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "callPrimitive: " + getCode().primitiveIndex();
        }

        @GenerateInline(false)
        protected abstract static class GetErrorObjectFromPrimFailCodeNode extends AbstractNode {
            protected abstract Object execute(int primFailCode);

            @Specialization
            protected static final Object doGet(final int primFailCode,
                            @Bind final Node node,
                            @Cached final InlinedConditionProfile inRangeProfile) {
                final ArrayObject errorTable = getContext(node).primitiveErrorTable;
                if (inRangeProfile.profile(node, primFailCode < errorTable.getObjectLength())) {
                    return errorTable.getObject(primFailCode);
                } else {
                    return (long) primFailCode;
                }
            }
        }
    }

    public static final class DoubleExtendedDoAnythingNode {

        public static AbstractInstrumentableBytecodeNode create(final VirtualFrame frame, final CompiledCodeObject code, final int index, final int numBytecodes, final byte param1,
                        final byte param2) {
            final int second = Byte.toUnsignedInt(param1);
            final int third = Byte.toUnsignedInt(param2);
            return switch (second >> 5) {
                case 0 -> new SelfSendNode(frame, code, index, numBytecodes, (NativeObject) code.getLiteral(third), second & 31);
                case 1 -> new SuperSendNode(frame, code, index, numBytecodes, third, second & 31);
                case 2 -> PushReceiverVariableNode.create(code, index, numBytecodes, third);
                case 3 -> new PushLiteralConstantNode(code, index, numBytecodes, third);
                case 4 -> PushLiteralVariableNode.create(code, index, numBytecodes, third);
                case 5 -> new StoreIntoReceiverVariableNode(code, index, numBytecodes, third);
                case 6 -> new PopIntoReceiverVariableNode(code, index, numBytecodes, third);
                case 7 -> new StoreIntoLiteralVariableNode(code, index, numBytecodes, third);
                default -> new UnknownBytecodeNode(code, index, numBytecodes, second);
            };
        }
    }

    public static final class DupNode extends AbstractInstrumentableBytecodeNode {
        @Child private FrameStackPushNode pushNode = FrameStackPushNode.create();
        @Child private FrameStackTopNode topNode = FrameStackTopNode.create();

        public DupNode(final CompiledCodeObject code, final int index) {
            super(code, index, 1);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.execute(frame, topNode.execute(frame));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "dup";
        }
    }

    public static final class ExtendedBytecodes {

        public static AbstractInstrumentableBytecodeNode createPopInto(final CompiledCodeObject code, final int index, final int numBytecodes, final byte nextByte) {
            final int variableIndex = variableIndex(nextByte);
            return switch (variableType(nextByte)) {
                case 0 -> new PopIntoReceiverVariableNode(code, index, numBytecodes, variableIndex);
                case 1 -> new PopIntoTemporaryLocationNode(code, index, numBytecodes, variableIndex);
                case 2 -> new UnknownBytecodeNode(code, index, numBytecodes, nextByte);
                case 3 -> new PopIntoLiteralVariableNode(code, index, numBytecodes, variableIndex);
                default -> throw SqueakException.create("illegal ExtendedStore bytecode");
            };
        }

        public static AbstractInstrumentableBytecodeNode createPush(final CompiledCodeObject code, final int index, final int numBytecodes, final byte nextByte) {
            final int variableIndex = variableIndex(nextByte);
            return switch (variableType(nextByte)) {
                case 0 -> PushReceiverVariableNode.create(code, index, numBytecodes, variableIndex);
                case 1 -> new PushTemporaryLocationNode(code, index, numBytecodes, variableIndex);
                case 2 -> new PushLiteralConstantNode(code, index, numBytecodes, variableIndex);
                case 3 -> PushLiteralVariableNode.create(code, index, numBytecodes, variableIndex);
                default -> throw SqueakException.create("unexpected type for ExtendedPush");
            };
        }

        public static AbstractInstrumentableBytecodeNode createStoreInto(final CompiledCodeObject code, final int index, final int numBytecodes, final byte nextByte) {
            final int variableIndex = variableIndex(nextByte);
            return switch (variableType(nextByte)) {
                case 0 -> new StoreIntoReceiverVariableNode(code, index, numBytecodes, variableIndex);
                case 1 -> new StoreIntoTemporaryLocationNode(code, index, numBytecodes, variableIndex);
                case 2 -> new UnknownBytecodeNode(code, index, numBytecodes, nextByte);
                case 3 -> new StoreIntoLiteralVariableNode(code, index, numBytecodes, variableIndex);
                default -> throw SqueakException.create("illegal ExtendedStore bytecode");
            };
        }

        public static int variableIndex(final byte i) {
            return i & 63;
        }

        public static byte variableType(final byte i) {
            return (byte) (i >> 6 & 3);
        }
    }

    public static final class PopNode extends AbstractInstrumentableBytecodeNode {
        @Child private FrameStackPopNode popNode = FrameStackPopNode.create();

        public PopNode(final CompiledCodeObject code, final int index) {
            super(code, index, 1);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            popNode.execute(frame);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pop";
        }
    }

    public static final class NopBytecodeNode extends AbstractInstrumentableBytecodeNode {
        public NopBytecodeNode(final CompiledCodeObject code, final int index) {
            super(code, index, 1);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            // nothing to do
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "nop";
        }
    }

    public static final class UnknownBytecodeNode extends AbstractInstrumentableBytecodeNode {
        private final long bytecode;

        public UnknownBytecodeNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bc) {
            super(code, index, numBytecodes);
            bytecode = bc;
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            throw SqueakException.create("Unknown/uninterpreted bytecode:", bytecode);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "unknown: " + bytecode;
        }
    }
}
