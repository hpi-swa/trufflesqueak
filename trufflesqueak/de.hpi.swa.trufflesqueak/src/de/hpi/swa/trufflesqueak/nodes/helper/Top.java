package de.hpi.swa.trufflesqueak.nodes.helper;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakExecutionNode;

public class Top extends SqueakExecutionNode {
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
