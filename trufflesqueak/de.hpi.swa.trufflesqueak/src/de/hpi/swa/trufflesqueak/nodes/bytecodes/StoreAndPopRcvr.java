package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNodeGen;

public class StoreAndPopRcvr extends StackBytecodeNode {
    private final int receiverIndex;

    public StoreAndPopRcvr(CompiledMethodObject compiledMethodObject, int idx, int i) {
        super(compiledMethodObject, idx);
        receiverIndex = i & 7;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        try {
            return super.executeGeneric(frame);
        } finally {
            decSP(frame);
        }
    }

    @Override
    public ContextAccessNode createChild(CompiledMethodObject cm) {
        return ObjectAtPutNodeGen.create(cm, receiverIndex, FrameSlotReadNode.receiver(cm), FrameSlotReadNode.top(cm));
    }
}
