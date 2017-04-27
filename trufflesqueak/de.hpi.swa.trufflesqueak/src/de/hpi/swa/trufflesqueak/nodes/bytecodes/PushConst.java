package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ConstantNode;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;

public class PushConst extends StackBytecodeNode {
    private final Object constant;

    public PushConst(CompiledMethodObject compiledMethodObject, int idx, Object obj) {
        super(compiledMethodObject, idx);
        constant = obj;
    }

    @Override
    public ContextAccessNode createChild(CompiledMethodObject cm) {
        return FrameSlotWriteNode.push(cm, new ConstantNode(constant));
    }
}
