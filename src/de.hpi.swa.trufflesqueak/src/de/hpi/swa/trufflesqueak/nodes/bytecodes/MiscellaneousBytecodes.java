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
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;

public final class MiscellaneousBytecodes {

    public abstract static class CallPrimitiveNode extends AbstractBytecodeNode {
        public CallPrimitiveNode(final int successorIndex, final int sp) {
            super(successorIndex, sp);
        }

        public static CallPrimitiveNode create(final VirtualFrame frame, final CompiledCodeObject code, final int successorIndex, final int sp, final byte byte1, final byte byte2) {
            return create(frame, code, successorIndex, sp, Byte.toUnsignedInt(byte1) + (Byte.toUnsignedInt(byte2) << 8));
        }

        public static CallPrimitiveNode create(final VirtualFrame frame, final CompiledCodeObject code, final int successorIndex, final int sp, final int primitiveIndex) {
            assert code.hasPrimitive() && code.primitiveIndex() == primitiveIndex;
            if (code.hasStoreIntoTemp1AfterCallPrimitive()) {
                return CallPrimitiveWithErrorCodeNodeGen.create(frame, successorIndex, sp);
            } else {
                return new CallPrimitiveWithoutErrorCodeNode(successorIndex, sp);
            }
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "callPrimitive: " + getCode().primitiveIndex();
        }

        public static final class CallPrimitiveWithoutErrorCodeNode extends CallPrimitiveNode {
            public CallPrimitiveWithoutErrorCodeNode(final int successorIndex, final int sp) {
                super(successorIndex, sp);
            }

            @Override
            public void executeVoid(final VirtualFrame frame) {
                // primitives handled specially
            }
        }

        public abstract static class CallPrimitiveWithErrorCodeNode extends CallPrimitiveNode {
            @Child private FrameStackWriteNode pushNode;

            public CallPrimitiveWithErrorCodeNode(final VirtualFrame frame, final int successorIndex, final int sp) {
                super(successorIndex, sp);
                pushNode = FrameStackWriteNode.create(frame, sp - 1);
            }

            @Specialization
            protected final void doCallPrimitive(final VirtualFrame frame,
                            @Bind final Node node,
                            @Bind final SqueakImageContext image,
                            @Cached final InlinedConditionProfile inRangeProfile) {
                final int primFailCode = image.getPrimFailCode();
                final ArrayObject errorTable = image.primitiveErrorTable;
                final Object errorObject;
                if (inRangeProfile.profile(node, primFailCode < errorTable.getObjectLength())) {
                    errorObject = errorTable.getObject(primFailCode);
                } else {
                    errorObject = (long) primFailCode;
                }
                pushNode.executeWriteAndSetSP(frame, errorObject, getSuccessorStackPointer());
            }
        }
    }

    public static final class DoubleExtendedDoAnythingNode {

        public static AbstractInstrumentableBytecodeNode create(final VirtualFrame frame, final CompiledCodeObject code, final int successorIndex, final int sp, final byte param1, final byte param2) {
            final int second = Byte.toUnsignedInt(param1);
            final int third = Byte.toUnsignedInt(param2);
            return switch (second >> 5) {
                case 0 -> new SelfSendNode(frame, successorIndex, sp, (NativeObject) code.getLiteral(third), second & 31);
                case 1 -> new SuperSendNode(frame, code, successorIndex, sp, third, second & 31);
                case 2 -> PushReceiverVariableNode.create(frame, successorIndex, sp + 1, third);
                case 3 -> new PushLiteralConstantNode(frame, code, successorIndex, sp + 1, third);
                case 4 -> PushLiteralVariableNode.create(frame, code, successorIndex, sp + 1, third);
                case 5 -> new StoreIntoReceiverVariableNode(frame, successorIndex, sp, third);
                case 6 -> new PopIntoReceiverVariableNode(frame, successorIndex, sp - 1, third);
                case 7 -> new StoreIntoLiteralVariableNode(frame, code, successorIndex, sp, third);
                default -> new UnknownBytecodeNode(successorIndex, sp, second);
            };
        }
    }

    public static final class DupNode extends AbstractInstrumentableBytecodeNode {
        @Child private FrameStackReadNode topNode;
        @Child private FrameStackWriteNode pushNode;

        public DupNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(successorIndex, sp);
            topNode = FrameStackReadNode.create(frame, sp - 2, false);
            pushNode = FrameStackWriteNode.create(frame, sp - 1);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWriteAndSetSP(frame, topNode.executeRead(frame), getSuccessorStackPointer());
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "dup";
        }
    }

    public static final class ExtendedBytecodes {

        public static AbstractInstrumentableBytecodeNode createPopInto(final VirtualFrame frame, final CompiledCodeObject code, final int successorIndex, final int sp, final byte nextByte) {
            final int variableIndex = variableIndex(nextByte);
            return switch (variableType(nextByte)) {
                case 0 -> new PopIntoReceiverVariableNode(frame, successorIndex, sp - 1, variableIndex);
                case 1 -> new PopIntoTemporaryLocationNode(frame, successorIndex, sp - 1, variableIndex);
                case 2 -> new UnknownBytecodeNode(successorIndex, sp, nextByte);
                case 3 -> new PopIntoLiteralVariableNode(frame, code, successorIndex, sp - 1, variableIndex);
                default -> throw SqueakException.create("illegal ExtendedStore bytecode");
            };
        }

        public static AbstractInstrumentableBytecodeNode createPush(final VirtualFrame frame, final CompiledCodeObject code, final int successorIndex, final int sp, final byte nextByte) {
            final int variableIndex = variableIndex(nextByte);
            return switch (variableType(nextByte)) {
                case 0 -> PushReceiverVariableNode.create(frame, successorIndex, sp + 1, variableIndex);
                case 1 -> new PushTemporaryLocationNode(frame, successorIndex, sp + 1, variableIndex);
                case 2 -> new PushLiteralConstantNode(frame, code, successorIndex, sp + 1, variableIndex);
                case 3 -> PushLiteralVariableNode.create(frame, code, successorIndex, sp + 1, variableIndex);
                default -> throw SqueakException.create("unexpected type for ExtendedPush");
            };
        }

        public static AbstractInstrumentableBytecodeNode createStoreInto(final VirtualFrame frame, final CompiledCodeObject code, final int successorIndex, final int sp, final byte nextByte) {
            final int variableIndex = variableIndex(nextByte);
            return switch (variableType(nextByte)) {
                case 0 -> new StoreIntoReceiverVariableNode(frame, successorIndex, sp, variableIndex);
                case 1 -> new StoreIntoTemporaryLocationNode(frame, successorIndex, sp, variableIndex);
                case 2 -> new UnknownBytecodeNode(successorIndex, sp, nextByte);
                case 3 -> new StoreIntoLiteralVariableNode(frame, code, successorIndex, sp, variableIndex);
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
        @Child private FrameStackReadNode popNode;

        public PopNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(successorIndex, sp);
            popNode = FrameStackReadNode.create(frame, sp, true);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            popNode.executeReadAndSetPC(frame, getSuccessorStackPointer());
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pop";
        }
    }

    public static final class NopBytecodeNode extends AbstractInstrumentableBytecodeNode {
        public NopBytecodeNode(final int successorIndex, final int sp) {
            super(successorIndex, sp);
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

        public UnknownBytecodeNode(final int successorIndex, final int sp, final int bc) {
            super(successorIndex, sp);
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
