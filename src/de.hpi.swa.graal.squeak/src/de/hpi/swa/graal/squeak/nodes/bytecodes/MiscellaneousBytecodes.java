/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.bytecodes.PushBytecodes.PushLiteralConstantNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.PushBytecodes.PushLiteralVariableNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.PushBytecodes.PushReceiverVariableNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.PushBytecodes.PushTemporaryLocationNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.SendBytecodes.SendSelfSelector;
import de.hpi.swa.graal.squeak.nodes.bytecodes.SendBytecodes.SingleExtendedSuperNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.StoreBytecodes.PopIntoAssociationNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.StoreBytecodes.PopIntoReceiverVariableNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.StoreBytecodes.PopIntoTemporaryLocationNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.StoreBytecodes.StoreIntoAssociationNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.StoreBytecodes.StoreIntoReceiverVariableNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.StoreBytecodes.StoreIntoTempNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackPopNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackPushNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackTopNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;

public final class MiscellaneousBytecodes {

    public static final class CallPrimitiveNode extends AbstractBytecodeNode {
        public static final int NUM_BYTECODES = 3;

        @Child public AbstractPrimitiveNode primitiveNode;
        private final int primitiveIndex;

        public CallPrimitiveNode(final CompiledMethodObject method, final int index, final int byte1, final int byte2) {
            super(method, index, NUM_BYTECODES);
            primitiveIndex = byte1 + (byte2 << 8);
            primitiveNode = method.image.primitiveNodeFactory.forIndex(method, primitiveIndex);
            assert method.hasPrimitive();
        }

        public static CallPrimitiveNode create(final CompiledMethodObject code, final int index, final int byte1, final int byte2) {
            return new CallPrimitiveNode(code, index, byte1, byte2);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            throw SqueakException.create("Should never be called directly.");
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "callPrimitive: " + primitiveIndex;
        }
    }

    public static final class DoubleExtendedDoAnythingNode {

        public static AbstractInstrumentableBytecodeNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int second, final int third) {
            final int opType = second >> 5;
            switch (opType) {
                case 0:
                    return new SendSelfSelector(code, index, numBytecodes, code.getLiteral(third), second & 31);
                case 1:
                    return new SingleExtendedSuperNode(code, index, numBytecodes, third, second & 31);
                case 2:
                    return PushReceiverVariableNode.create(code, index, numBytecodes, third);
                case 3:
                    return new PushLiteralConstantNode(code, index, numBytecodes, third);
                case 4:
                    return new PushLiteralVariableNode(code, index, numBytecodes, third);
                case 5:
                    return new StoreIntoReceiverVariableNode(code, index, numBytecodes, third);
                case 6:
                    return new PopIntoReceiverVariableNode(code, index, numBytecodes, third);
                case 7:
                    return new StoreIntoAssociationNode(code, index, numBytecodes, third);
                default:
                    return new UnknownBytecodeNode(code, index, numBytecodes, second);
            }
        }
    }

    public static final class DupNode extends AbstractInstrumentableBytecodeNode {
        @Child private FrameStackPushNode pushNode;
        @Child private FrameStackTopNode topNode;

        public DupNode(final CompiledCodeObject code, final int index, final int numBytecodes) {
            super(code, index, numBytecodes);
            pushNode = FrameStackPushNode.create(code);
            topNode = FrameStackTopNode.create(code);
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

        public static AbstractInstrumentableBytecodeNode createPopInto(final CompiledCodeObject code, final int index, final int numBytecodes, final int nextByte) {
            final int variableIndex = variableIndex(nextByte);
            switch (variableType(nextByte)) {
                case 0:
                    return new PopIntoReceiverVariableNode(code, index, numBytecodes, variableIndex);
                case 1:
                    return new PopIntoTemporaryLocationNode(code, index, numBytecodes, variableIndex);
                case 2:
                    return new UnknownBytecodeNode(code, index, numBytecodes, nextByte);
                case 3:
                    return new PopIntoAssociationNode(code, index, numBytecodes, variableIndex);
                default:
                    throw SqueakException.create("illegal ExtendedStore bytecode");
            }
        }

        public static AbstractInstrumentableBytecodeNode createPush(final CompiledCodeObject code, final int index, final int numBytecodes, final int nextByte) {
            final int variableIndex = variableIndex(nextByte);
            switch (variableType(nextByte)) {
                case 0:
                    return PushReceiverVariableNode.create(code, index, numBytecodes, variableIndex);
                case 1:
                    return new PushTemporaryLocationNode(code, index, numBytecodes, variableIndex);
                case 2:
                    return new PushLiteralConstantNode(code, index, numBytecodes, variableIndex);
                case 3:
                    return new PushLiteralVariableNode(code, index, numBytecodes, variableIndex);
                default:
                    throw SqueakException.create("unexpected type for ExtendedPush");
            }
        }

        public static AbstractInstrumentableBytecodeNode createStoreInto(final CompiledCodeObject code, final int index, final int numBytecodes, final int nextByte) {
            final int variableIndex = variableIndex(nextByte);
            switch (variableType(nextByte)) {
                case 0:
                    return new StoreIntoReceiverVariableNode(code, index, numBytecodes, variableIndex);
                case 1:
                    return new StoreIntoTempNode(code, index, numBytecodes, variableIndex);
                case 2:
                    return new UnknownBytecodeNode(code, index, numBytecodes, nextByte);
                case 3:
                    return new StoreIntoAssociationNode(code, index, numBytecodes, variableIndex);
                default:
                    throw SqueakException.create("illegal ExtendedStore bytecode");
            }
        }

        private static byte variableIndex(final int i) {
            return (byte) (i & 63);
        }

        private static byte variableType(final int i) {
            return (byte) (i >> 6 & 3);
        }
    }

    public static final class PopNode extends AbstractInstrumentableBytecodeNode {
        @Child private FrameStackPopNode popNode;

        public PopNode(final CompiledCodeObject code, final int index, final int numBytecodes) {
            super(code, index, numBytecodes);
            popNode = FrameStackPopNode.create(code);
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
