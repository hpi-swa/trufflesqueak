package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNodeGen;

public class StoreAndPopRcvr extends StackBytecodeNode {
    public StoreAndPopRcvr(CompiledMethodObject cm, int idx, int i) {
        super(cm, idx, createChild(cm, i & 7), -1);
    }

    public static ContextAccessNode createChild(CompiledMethodObject cm, int receiverIndex) {
        return ObjectAtPutNodeGen.create(cm, receiverIndex, FrameSlotReadNode.receiver(cm), FrameSlotReadNode.top(cm));
    }
}
