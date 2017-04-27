package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;

public abstract class StackBytecodeNode extends SqueakBytecodeNode {
    private final int stackEffect;
    @Child ContextAccessNode child;

    public StackBytecodeNode(CompiledMethodObject cm, int idx, ContextAccessNode node, int effect) {
        super(cm, idx);
        child = node;
        stackEffect = effect;
    }

    protected ContextAccessNode getChild() {
        return child;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        try {
            return child.executeGeneric(frame);
        } finally {
            changeSP(frame, stackEffect);
        }
    }
}
