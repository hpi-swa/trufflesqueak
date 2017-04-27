package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;

public class PushTemp extends StackBytecodeNode {
    public PushTemp(CompiledMethodObject cm, int idx, int i) {
        super(cm, idx, createChild(cm, i & 15), +1);
    }

    public static FrameSlotNode createChild(CompiledMethodObject cm, int tempIndex) {
        return FrameSlotWriteNode.push(cm, FrameSlotReadNode.create(cm, cm.stackSlots[tempIndex]));
    }
}
