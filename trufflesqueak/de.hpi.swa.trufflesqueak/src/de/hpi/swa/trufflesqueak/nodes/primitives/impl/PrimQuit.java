package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExit;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;

public abstract class PrimQuit extends PrimitiveNode {
    public PrimQuit(CompiledMethodObject code) {
        super(code);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new SqueakExit(0);
    }
}
