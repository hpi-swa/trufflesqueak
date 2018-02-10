package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags.StatementTag;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.Returns.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodes.PushLiteralConstantNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodes.PushLiteralVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodes.PushReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodes.PushTemporaryLocationNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.SendSelfSelector;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.SingleExtendedSuperNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreBytecodes.PopIntoAssociationNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreBytecodes.PopIntoReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreBytecodes.PopIntoTemporaryLocationNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreBytecodes.StoreIntoAssociationNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreBytecodes.StoreIntoReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreBytecodes.StoreIntoTempNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopStackNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.TopStackNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ControlPrimitives.PrimitiveFailedNode;

public final class MiscellaneousBytecodes {

    public static class CallPrimitiveNode extends AbstractBytecodeNode {
        @Child private AbstractPrimitiveNode primitiveNode;
        @CompilationFinal private final int primitiveIndex;

        public CallPrimitiveNode(CompiledCodeObject code, int index, int numBytecodes, int byte1, int byte2) {
            super(code, index, numBytecodes);
            assert code instanceof CompiledMethodObject;
            if (!code.hasPrimitive()) {
                primitiveIndex = 0;
                primitiveNode = PrimitiveFailedNode.create((CompiledMethodObject) code);
            } else {
                primitiveIndex = byte1 + (byte2 << 8);
                primitiveNode = PrimitiveNodeFactory.forIndex((CompiledMethodObject) code, primitiveIndex);
                primitiveNode = primitiveNode != null ? primitiveNode : PrimitiveFailedNode.create((CompiledMethodObject) code);
            }
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            CompilerAsserts.compilationConstant(index);
            try {
                throw new LocalReturn(primitiveNode.executePrimitive(frame));
            } catch (UnsupportedSpecializationException | PrimitiveFailed e) {
            }
        }

        @Override
        protected boolean isTaggedWith(Class<?> tag) {
            return tag == StatementTag.class;
        }

        @Override
        public String toString() {
            return "callPrimitive: " + primitiveIndex;
        }
    }

    public static abstract class DoubleExtendedDoAnythingNode {

        public static AbstractBytecodeNode create(CompiledCodeObject code, int index, int numBytecodes, int second, int third) {
            int opType = second >> 5;
            switch (opType) {
                case 0:
                    return new SendSelfSelector(code, index, numBytecodes, code.getLiteral(third), second & 31);
                case 1:
                    return new SingleExtendedSuperNode(code, index, numBytecodes, third, second & 31);
                case 2:
                    return new PushReceiverVariableNode(code, index, numBytecodes, third);
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

    public static class DupNode extends UnknownBytecodeNode {
        @Child private PushStackNode pushNode;
        @Child private TopStackNode topNode;

        public DupNode(CompiledCodeObject code, int index, int numBytecodes) {
            super(code, index, numBytecodes, -1);
            topNode = TopStackNode.create(code);
            pushNode = PushStackNode.create(code);
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            pushNode.executeWrite(frame, topNode.executeRead(frame));
        }

        @Override
        public String toString() {
            return "dup";
        }
    }

    public static class ExtendedBytecodes {

        public static AbstractBytecodeNode createPopInto(CompiledCodeObject code, int index, int numBytecodes, int nextByte) {
            long variableIndex = variableIndex(nextByte);
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
                    throw new SqueakException("illegal ExtendedStore bytecode");
            }
        }

        public static AbstractBytecodeNode createPush(CompiledCodeObject code, int index, int numBytecodes, int nextByte) {
            int variableIndex = variableIndex(nextByte);
            switch (variableType(nextByte)) {
                case 0:
                    return new PushReceiverVariableNode(code, index, numBytecodes, variableIndex);
                case 1:
                    return new PushTemporaryLocationNode(code, index, numBytecodes, variableIndex);
                case 2:
                    return new PushLiteralConstantNode(code, index, numBytecodes, variableIndex);
                case 3:
                    return new PushLiteralVariableNode(code, index, numBytecodes, variableIndex);
                default:
                    throw new SqueakException("unexpected type for ExtendedPush");
            }
        }

        public static AbstractBytecodeNode createStoreInto(CompiledCodeObject code, int index, int numBytecodes, int nextByte) {
            long variableIndex = variableIndex(nextByte);
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
                    throw new SqueakException("illegal ExtendedStore bytecode");
            }
        }

        protected static byte variableIndex(int i) {
            return (byte) (i & 63);
        }

        protected static byte variableType(int i) {
            return (byte) ((i >> 6) & 3);
        }
    }

    public static class PopNode extends UnknownBytecodeNode {
        @Child private PopStackNode popNode;

        public PopNode(CompiledCodeObject code, int index, int numBytecodes) {
            super(code, index, numBytecodes, -1);
            popNode = PopStackNode.create(code);
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            popNode.executeRead(frame);
        }

        @Override
        public String toString() {
            return "pop";
        }
    }

    public static class UnknownBytecodeNode extends AbstractBytecodeNode {
        private final long bytecode;

        public UnknownBytecodeNode(CompiledCodeObject code, int index, int numBytecodes, int bc) {
            super(code, index, numBytecodes);
            bytecode = bc;
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            throw new SqueakException("Unknown/uninterpreted bytecode " + bytecode);
        }

        @Override
        public String toString() {
            return "unknown: " + bytecode;
        }
    }
}
