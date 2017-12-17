package de.hpi.swa.trufflesqueak.nodes.primitives.impl.quickreturn;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;

public class PrimQuickReturnMinusOne extends PrimitiveNode {
    public PrimQuickReturnMinusOne(CompiledMethodObject code) {
        super(code);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return -1;
    }
}