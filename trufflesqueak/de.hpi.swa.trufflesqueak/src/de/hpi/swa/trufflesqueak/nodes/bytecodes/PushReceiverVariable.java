package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNodeGen;

public class PushReceiverVariable extends StackBytecodeNode {
    private int variableIndex;

    public PushReceiverVariable(CompiledMethodObject cm, int idx, int i) {
        super(cm, idx);
        variableIndex = i & 15;
    }

    public PushReceiverVariable(CompiledMethodObject cm, int i) {
        super(cm, 0);
        variableIndex = i;
    }

    @Override
    public ContextAccessNode createChild(CompiledMethodObject cm) {
        return FrameSlotWriteNode.push(cm, ObjectAtNodeGen.create(cm, variableIndex, FrameSlotReadNode.receiver(cm)));
    }
}
