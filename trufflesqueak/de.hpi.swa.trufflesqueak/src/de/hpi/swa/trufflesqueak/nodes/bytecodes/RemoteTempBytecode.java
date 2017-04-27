package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public abstract class RemoteTempBytecode extends StackBytecodeNode {
    protected final int indexInArray;
    protected final int indexOfArray;

    public RemoteTempBytecode(CompiledMethodObject compiledMethodObject, int idx, int indexInArray, int indexOfArray) {
        super(compiledMethodObject, idx);
        this.indexInArray = indexInArray;
        this.indexOfArray = indexOfArray;
    }

    protected ContextAccessNode getTempArray(CompiledMethodObject cm) {
        return FrameSlotReadNode.temp(cm, indexOfArray);
    }
}
