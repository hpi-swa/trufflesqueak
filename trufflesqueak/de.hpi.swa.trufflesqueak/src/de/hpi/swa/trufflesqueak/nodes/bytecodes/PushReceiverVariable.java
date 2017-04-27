package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNodeGen;

public class PushReceiverVariable extends StackBytecodeNode {
    public PushReceiverVariable(CompiledMethodObject cm, int idx, int i) {
        super(cm, idx, createChild(cm, i & 15), +1);
    }

    public PushReceiverVariable(CompiledMethodObject cm, int i) {
        super(cm, 0, createChild(cm, i), +1);
    }

    public static ContextAccessNode createChild(CompiledMethodObject cm, int variableIndex) {
        return FrameSlotWriteNode.push(cm, ObjectAtNodeGen.create(cm, variableIndex, FrameSlotReadNode.receiver(cm)));
    }
}
