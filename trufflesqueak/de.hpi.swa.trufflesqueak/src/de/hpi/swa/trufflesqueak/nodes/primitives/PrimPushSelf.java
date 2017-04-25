package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class PrimPushSelf extends PrimitiveQuickReturnNode {
    public PrimPushSelf(CompiledMethodObject cm) {
        super(cm);
    }

    @Override
    Object getConstant(VirtualFrame frame) {
        return getReceiver(frame);
    }
}