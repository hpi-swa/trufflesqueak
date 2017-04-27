package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;

public class Dup extends StackBytecodeNode {
    public Dup(CompiledMethodObject cm, int idx) {
        super(cm, idx, createChild(cm), +1);
    }

    public static FrameSlotNode createChild(CompiledMethodObject cm) {
        return FrameSlotWriteNode.push(cm, FrameSlotReadNode.top(cm));
    }
}
