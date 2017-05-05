package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveQuickReturnNode;

public class PrimPushNil extends PrimitiveQuickReturnNode {
    public PrimPushNil(CompiledMethodObject cm) {
        super(cm);
    }

    @Override
    protected Object getConstant(VirtualFrame frame) {
        return method.image.nil;
    }
}