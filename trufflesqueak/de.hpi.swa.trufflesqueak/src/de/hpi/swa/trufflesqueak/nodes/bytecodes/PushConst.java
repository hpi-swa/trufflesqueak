package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ConstantNode;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;

public class PushConst extends StackBytecodeNode {
    public PushConst(CompiledMethodObject compiledMethodObject, int idx, Object obj) {
        super(compiledMethodObject, idx, createChild(compiledMethodObject, obj), +1);
    }

    public static ContextAccessNode createChild(CompiledMethodObject cm, Object constant) {
        return FrameSlotWriteNode.push(cm, new ConstantNode(constant));
    }
}
