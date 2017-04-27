package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class PrimPushOne extends PrimitiveQuickReturnNode {
    public PrimPushOne(CompiledMethodObject cm) {
        super(cm);
    }

    @Override
    Object getConstant(VirtualFrame frame) {
        return 1;
    }
}