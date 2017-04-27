package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public abstract class RemoteTempBytecode extends StackBytecodeNode {
    public RemoteTempBytecode(CompiledMethodObject cm, int idx, ContextAccessNode node, int stackEffect) {
        super(cm, idx, node, stackEffect);
    }

    protected static ContextAccessNode getTempArray(CompiledMethodObject cm, int indexOfArray) {
        return FrameSlotReadNode.temp(cm, indexOfArray);
    }
}
