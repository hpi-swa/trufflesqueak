package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;

public class StoreAndPopTemp extends StackBytecodeNode {
    public StoreAndPopTemp(CompiledMethodObject cm, int idx, int i) {
        super(cm, idx, createChild(cm, i & 7), -1);
    }

    public static ContextAccessNode createChild(CompiledMethodObject cm, int tempIndex) {
        return FrameSlotWriteNode.temp(cm, tempIndex, FrameSlotReadNode.top(cm));
    }
}
