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

        StoreIntoNode(CompiledCodeObject code, int idx, WriteNode writeNode) {
            super(code, idx);
            node = writeNode;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return node.executeWrite(frame, top(frame));
        }
    }

    public static SqueakBytecodeNode create(CompiledCodeObject code, int idx, int i) {
        switch (extractType(i)) {
            case 0:
                return new StoreIntoNode(code, idx, ObjectAtPutNode.create(code, idx, new FrameReceiverNode(code)));
            case 1:
                return new StoreIntoNode(code, idx, FrameSlotWriteNode.create(code.getStackSlot(idx)));
            case 2:
                throw new RuntimeException("illegal ExtendedStore bytecode: variable type 2");
            case 3:
                return new StoreIntoNode(code, idx, ObjectAtPutNode.create(code, 1, new MethodLiteralNode(code, idx)));
            default:
                throw new RuntimeException("illegal ExtendedStore bytecode");
        }
    }
}
