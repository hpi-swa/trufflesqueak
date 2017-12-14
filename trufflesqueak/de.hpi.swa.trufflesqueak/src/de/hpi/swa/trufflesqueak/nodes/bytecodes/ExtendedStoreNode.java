package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.WriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNode;

public class ExtendedStoreNode extends ExtendedAccess {
    public static final int ASSOCIATION_VALUE = 1;

    private ExtendedStoreNode() {
    }

    private static class StoreTopIntoNode extends SqueakBytecodeNode {
        @Child WriteNode node;

        StoreTopIntoNode(CompiledCodeObject code, int index, int numBytecodes, WriteNode writeNode) {
            super(code, index, numBytecodes);
            node = writeNode;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return node.executeWrite(frame, top(frame));
        }
    }

    public static SqueakBytecodeNode create(CompiledCodeObject code, int index, int numBytecodes, int nextByte) {
        int variableIndex = variableIndex(nextByte);
        switch (variableType(nextByte)) {
            case 0:
                return new StoreTopIntoNode(code, index, numBytecodes, ObjectAtPutNode.create(variableIndex, new FrameReceiverNode(code)));
            case 1:
                return new StoreTopIntoNode(code, index, numBytecodes, FrameSlotWriteNode.create(code.getTempSlot(variableIndex)));
            case 2:
                return new UnknownBytecodeNode(code, index, numBytecodes, nextByte);
            case 3:
                return new StoreTopIntoNode(code, index, numBytecodes, ObjectAtPutNode.create(ASSOCIATION_VALUE, new MethodLiteralNode(code, variableIndex)));
            default:
                throw new RuntimeException("illegal ExtendedStore bytecode");
        }
    }
}
