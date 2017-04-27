package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNodeGen;

public class PushLiteralConst extends StackBytecodeNode {
    public PushLiteralConst(CompiledMethodObject compiledMethodObject, int idx, int i) {
        super(compiledMethodObject, idx, createChild(compiledMethodObject, i & 31), +1);
    }

    public static ContextAccessNode createChild(CompiledMethodObject cm, int literalIdx) {
        return FrameSlotWriteNode.push(cm, MethodLiteralNodeGen.create(cm, literalIdx));
    }
}
