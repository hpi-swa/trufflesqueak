package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;

public class PushReceiver extends StackBytecodeNode {
    public PushReceiver(CompiledMethodObject cm, int idx) {
        super(cm, idx, createChild(cm), +1);
    }

    public static FrameSlotWriteNode createChild(CompiledMethodObject cm) {
        return FrameSlotWriteNode.push(cm, FrameSlotReadNode.receiver(cm));
    }
}
