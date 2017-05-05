package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveQuickReturnNode;

public class PrimPushMinusOne extends PrimitiveQuickReturnNode {
    public PrimPushMinusOne(CompiledMethodObject cm) {
        super(cm);
    }

    @Override
    protected Object getConstant(VirtualFrame frame) {
        return -1;
    }
}