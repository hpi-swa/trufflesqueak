package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public class Pop extends StackBytecodeNode {
    public Pop(CompiledMethodObject cm, int idx) {
        super(cm, idx, createChild(cm), -1);
    }

    public static FrameSlotNode createChild(CompiledMethodObject cm) {
        return FrameSlotReadNode.top(cm);
    }
}
