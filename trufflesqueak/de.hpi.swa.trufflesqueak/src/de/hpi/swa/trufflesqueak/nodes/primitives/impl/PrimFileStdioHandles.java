package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;

public class PrimFileStdioHandles extends PrimitiveNode {
    public PrimFileStdioHandles(CompiledMethodObject cm) {
        super(cm);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return method.image.wrap(0, 1, 2);
    }
}
