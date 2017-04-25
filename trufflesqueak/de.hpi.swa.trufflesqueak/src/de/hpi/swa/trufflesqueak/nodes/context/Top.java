package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class Top extends ContextAccessNode {
    private final int offset;

    public Top(CompiledMethodObject cm, int off) {
        super(cm);
        offset = off;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return top(frame, offset);
    }
}
