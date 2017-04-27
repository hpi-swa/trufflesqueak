package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;

public class PushTemp extends StackBytecodeNode {
    private final int tempIndex;

    public PushTemp(CompiledMethodObject cm, int idx, int i) {
        super(cm, idx);
        tempIndex = i & 15;
    }

    @Override
    public FrameSlotNode createChild(CompiledMethodObject cm) {
        return FrameSlotWriteNode.push(cm, FrameSlotReadNode.create(cm, cm.stackSlots[tempIndex]));
    }
}
