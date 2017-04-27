package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;

public class PushLiteralConst extends StackBytecodeNode {
    private final int literalIdx;

    public PushLiteralConst(CompiledMethodObject compiledMethodObject, int idx, int i) {
        super(compiledMethodObject, idx);
        literalIdx = i & 31;
    }

    @Override
    public ContextAccessNode createChild(CompiledMethodObject cm) {
        return FrameSlotWriteNode.push(cm, new MethodLiteralNode(cm, literalIdx));
    }
}
