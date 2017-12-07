package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.WriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNode;

public class ExtendedStoreAndPopNode extends ExtendedAccess {
    private ExtendedStoreAndPopNode() {
    }

    private static class StoreAndPopIntoNode extends SqueakBytecodeNode {
        @Child WriteNode node;

        StoreAndPopIntoNode(CompiledCodeObject code, int index, WriteNode writeNode) {
            super(code, index);
            node = writeNode;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return node.executeWrite(frame, pop(frame));
        }
    }

    public static SqueakBytecodeNode create(CompiledCodeObject code, int index, int nextByte) {
        int variableIndex = variableIndex(nextByte);
        switch (variableType(nextByte)) {
            case 0:
                return new StoreAndPopReceiverVariableNode(code, index, variableIndex);
            case 1:
                return new StoreAndPopTemporaryVariableNode(code, index, variableIndex);
            case 2:
                return new UnknownBytecodeNode(code, index, -1);
            case 3:
                return new StoreAndPopIntoNode(code, index, ObjectAtPutNode.create(1, new MethodLiteralNode(code, variableIndex)));
            default:
                throw new RuntimeException("illegal ExtendedStore bytecode");
        }
    }
}
