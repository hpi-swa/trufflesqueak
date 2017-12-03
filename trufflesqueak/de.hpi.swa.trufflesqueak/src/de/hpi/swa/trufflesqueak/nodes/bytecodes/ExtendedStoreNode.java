package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.WriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNode;

public class ExtendedStoreNode extends ExtendedAccess {
    private ExtendedStoreNode() {
    }

    private static class StoreIntoNode extends SqueakBytecodeNode {
        @Child WriteNode node;

        StoreIntoNode(CompiledCodeObject code, int index, WriteNode writeNode) {
            super(code, index);
            node = writeNode;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return node.executeWrite(frame, top(frame));
        }
    }

    public static SqueakBytecodeNode create(CompiledCodeObject code, int index, int i) {
        switch (extractType(i)) {
            case 0:
                return new StoreIntoNode(code, index, ObjectAtPutNode.create(index, new FrameReceiverNode(code)));
            case 1:
                return new StoreIntoNode(code, index, FrameSlotWriteNode.create(code.getStackSlot(index)));
            case 2:
                return new UnknownBytecodeNode(code, index, -1);
            case 3:
                return new StoreIntoNode(code, index, ObjectAtPutNode.create(1, new MethodLiteralNode(code, index)));
            default:
                throw new RuntimeException("illegal ExtendedStore bytecode");
        }
    }
}
