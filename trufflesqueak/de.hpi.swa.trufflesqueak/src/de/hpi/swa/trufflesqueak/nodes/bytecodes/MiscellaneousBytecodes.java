package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.instrumentation.StandardTags.StatementTag;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.Returns.FreshReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.GetOrCreateContextNode;
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
import de.hpi.swa.trufflesqueak.nodes.context.stack.StackPopNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.StackPushNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.StackTopNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ControlPrimitives.PrimitiveFailedNode;

public final class MiscellaneousBytecodes {

    public static class CallPrimitiveNode extends AbstractBytecodeNode {
        @Child private GetOrCreateContextNode getOrCreateContextNode;
        @Child private AbstractPrimitiveNode primitiveNode;
        @CompilationFinal private final int primitiveIndex;

        public CallPrimitiveNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int byte1, final int byte2) {
            super(code, index, numBytecodes);
            assert code instanceof CompiledMethodObject;
            getOrCreateContextNode = GetOrCreateContextNode.create(code);
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
        public void executeVoid(final VirtualFrame frame) {
            CompilerAsserts.compilationConstant(index);
            try {
                throw new FreshReturn(new LocalReturn(primitiveNode.executePrimitive(frame)));
            } catch (PrimitiveFailed e) { // TODO: support `error: ec`
// if (e.getReasonCode() != ERROR_TABLE.GENERIC_ERROR) { // handle `error: ec`
// ContextObject thisContext = getOrCreateContextNode.executeGet(frame, true);
// Object error = code.image.lookupError(e.getReasonCode());
// thisContext.atTempPut(0, error);
// }
            } catch (UnsupportedSpecializationException e) {
            }
        }

        @Override
        public boolean hasTag(final Class<? extends Tag> tag) {
            return tag == StatementTag.class;
        }

        @Override
        public String toString() {
            return "callPrimitive: " + primitiveIndex;
        }
    }

    public abstract static class DoubleExtendedDoAnythingNode {

        public static AbstractBytecodeNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int second, final int third) {
            final int opType = second >> 5;
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
        @Child private StackPushNode pushNode;
        @Child private StackTopNode topNode;

        public DupNode(final CompiledCodeObject code, final int index, final int numBytecodes) {
            super(code, index, numBytecodes, -1);
            topNode = StackTopNode.create(code);
            pushNode = StackPushNode.create(code);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, topNode.executeRead(frame));
        }

        @Override
        public String toString() {
            return "dup";
        }
    }

    public static class ExtendedBytecodes {

        public static AbstractBytecodeNode createPopInto(final CompiledCodeObject code, final int index, final int numBytecodes, final int nextByte) {
            final long variableIndex = variableIndex(nextByte);
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

        public static AbstractBytecodeNode createPush(final CompiledCodeObject code, final int index, final int numBytecodes, final int nextByte) {
            final int variableIndex = variableIndex(nextByte);
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

        public static AbstractBytecodeNode createStoreInto(final CompiledCodeObject code, final int index, final int numBytecodes, final int nextByte) {
            final long variableIndex = variableIndex(nextByte);
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

        protected static byte variableIndex(final int i) {
            return (byte) (i & 63);
        }

        protected static byte variableType(final int i) {
            return (byte) ((i >> 6) & 3);
        }
    }

    public static class PopNode extends UnknownBytecodeNode {
        @Child private StackPopNode popNode;

        public PopNode(final CompiledCodeObject code, final int index, final int numBytecodes) {
            super(code, index, numBytecodes, -1);
            popNode = StackPopNode.create(code);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            popNode.executeRead(frame);
        }

        @Override
        public String toString() {
            return "pop";
        }
    }

    public static class UnknownBytecodeNode extends AbstractBytecodeNode {
        private final long bytecode;

        public UnknownBytecodeNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bc) {
            super(code, index, numBytecodes);
            bytecode = bc;
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            throw new SqueakException("Unknown/uninterpreted bytecode " + bytecode);
        }

        @Override
        public String toString() {
            return "unknown: " + bytecode;
        }
    }
}
