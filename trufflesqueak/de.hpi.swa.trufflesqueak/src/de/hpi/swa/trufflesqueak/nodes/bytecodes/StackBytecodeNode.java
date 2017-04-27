package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;

public abstract class StackBytecodeNode extends SqueakBytecodeNode {
    @Child ContextAccessNode child;

    public StackBytecodeNode(CompiledMethodObject cm, int idx) {
        super(cm, idx);
        child = createChild(cm);
    }

    public abstract ContextAccessNode createChild(CompiledMethodObject cm);

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return child.executeGeneric(frame);
    }
}
