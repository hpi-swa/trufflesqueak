package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;

public abstract class ExtendedAccess extends StackBytecodeNode {
    public ExtendedAccess(CompiledMethodObject cm, int index, ContextAccessNode node, int effect) {
        super(cm, index, node, effect);
    }

    protected static byte extractIndex(int i) {
        return (byte) (i & 63);
    }

    protected static byte extractType(int i) {
        return (byte) ((i >> 6) & 3);
    }
}