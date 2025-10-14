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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.MiscellaneousBytecodesFactory.CallPrimitiveNodeFactory.CallPrimitiveWithErrorCodeNodeGen;
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

    public abstract static class CallPrimitiveNode extends AbstractBytecodeNode {
        public CallPrimitiveNode(final int successorIndex) {
            super(successorIndex);
        }

        public static CallPrimitiveNode create(final CompiledCodeObject code, final int successorIndex, final byte byte1, final byte byte2) {
            return create(code, successorIndex, Byte.toUnsignedInt(byte1) + (Byte.toUnsignedInt(byte2) << 8));
        }

        public static CallPrimitiveNode create(final CompiledCodeObject code, final int successorIndex, final int primitiveIndex) {
            assert code.hasPrimitive() && code.primitiveIndex() == primitiveIndex;
            if (code.hasStoreIntoTemp1AfterCallPrimitive()) {
                return CallPrimitiveWithErrorCodeNodeGen.create(successorIndex);
            } else {
                return new CallPrimitiveWithoutErrorCodeNode(successorIndex);
            }
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "callPrimitive: " + getCode().primitiveIndex();
        }

        public static final class CallPrimitiveWithoutErrorCodeNode extends CallPrimitiveNode {
            public CallPrimitiveWithoutErrorCodeNode(final int successorIndex) {
                super(successorIndex);
            }

            @Override
            public void executeVoid(final VirtualFrame frame) {
                // primitives handled specially
            }
        }

        public abstract static class CallPrimitiveWithErrorCodeNode extends CallPrimitiveNode {
            public CallPrimitiveWithErrorCodeNode(final int successorIndex) {
                super(successorIndex);
            }

            @Specialization
            protected static final void doCallPrimitive(final VirtualFrame frame,
                            @Bind final Node node,
                            @Bind final SqueakImageContext image,
                            @Cached final FrameStackPushNode pushNode,
                            @Cached final InlinedConditionProfile inRangeProfile) {
                final int primFailCode = image.getPrimFailCode();
                final ArrayObject errorTable = image.primitiveErrorTable;
                final Object errorObject;
                if (inRangeProfile.profile(node, primFailCode < errorTable.getObjectLength())) {
                    errorObject = errorTable.getObject(primFailCode);
                } else {
                    errorObject = (long) primFailCode;
                }
                pushNode.execute(frame, errorObject);
            }
        }
    }

    public static final class DoubleExtendedDoAnythingNode {

        public static AbstractInstrumentableBytecodeNode create(final VirtualFrame frame, final CompiledCodeObject code, final int successorIndex, final byte param1, final byte param2) {
            final int second = Byte.toUnsignedInt(param1);
            final int third = Byte.toUnsignedInt(param2);
            return switch (second >> 5) {
                case 0 -> new SelfSendNode(frame, successorIndex, (NativeObject) code.getLiteral(third), second & 31);
                case 1 -> new SuperSendNode(frame, code, successorIndex, third, second & 31);
                case 2 -> PushReceiverVariableNode.create(successorIndex, third);
                case 3 -> new PushLiteralConstantNode(code, successorIndex, third);
                case 4 -> PushLiteralVariableNode.create(code, successorIndex, third);
                case 5 -> new StoreIntoReceiverVariableNode(successorIndex, third);
                case 6 -> new PopIntoReceiverVariableNode(successorIndex, third);
                case 7 -> new StoreIntoLiteralVariableNode(code, successorIndex, third);
                default -> new UnknownBytecodeNode(successorIndex, second);
            };
        }
    }

    public static final class DupNode extends AbstractInstrumentableBytecodeNode {
        @Child private FrameStackPushNode pushNode = FrameStackPushNode.create();
        @Child private FrameStackTopNode topNode = FrameStackTopNode.create();

        public DupNode(final int successorIndex) {
            super(successorIndex);
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

        public static AbstractInstrumentableBytecodeNode createPopInto(final CompiledCodeObject code, final int successorIndex, final byte nextByte) {
            final int variableIndex = variableIndex(nextByte);
            return switch (variableType(nextByte)) {
                case 0 -> new PopIntoReceiverVariableNode(successorIndex, variableIndex);
                case 1 -> new PopIntoTemporaryLocationNode(successorIndex, variableIndex);
                case 2 -> new UnknownBytecodeNode(successorIndex, nextByte);
                case 3 -> new PopIntoLiteralVariableNode(code, successorIndex, variableIndex);
                default -> throw SqueakException.create("illegal ExtendedStore bytecode");
            };
        }

        public static AbstractInstrumentableBytecodeNode createPush(final CompiledCodeObject code, final int successorIndex, final byte nextByte) {
            final int variableIndex = variableIndex(nextByte);
            return switch (variableType(nextByte)) {
                case 0 -> PushReceiverVariableNode.create(successorIndex, variableIndex);
                case 1 -> new PushTemporaryLocationNode(successorIndex, variableIndex);
                case 2 -> new PushLiteralConstantNode(code, successorIndex, variableIndex);
                case 3 -> PushLiteralVariableNode.create(code, successorIndex, variableIndex);
                default -> throw SqueakException.create("unexpected type for ExtendedPush");
            };
        }

        public static AbstractInstrumentableBytecodeNode createStoreInto(final CompiledCodeObject code, final int successorIndex, final byte nextByte) {
            final int variableIndex = variableIndex(nextByte);
            return switch (variableType(nextByte)) {
                case 0 -> new StoreIntoReceiverVariableNode(successorIndex, variableIndex);
                case 1 -> new StoreIntoTemporaryLocationNode(successorIndex, variableIndex);
                case 2 -> new UnknownBytecodeNode(successorIndex, nextByte);
                case 3 -> new StoreIntoLiteralVariableNode(code, successorIndex, variableIndex);
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

        public PopNode(final int successorIndex) {
            super(successorIndex);
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
        public NopBytecodeNode(final int successorIndex) {
            super(successorIndex);
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

        public UnknownBytecodeNode(final int successorIndex, final int bc) {
            super(successorIndex);
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
