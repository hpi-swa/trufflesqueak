package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public abstract class PrimitiveQuickReturnNode extends PrimitiveNode {
    public PrimitiveQuickReturnNode(CompiledMethodObject code) {
        super(code);
    }

    abstract protected Object getConstant(VirtualFrame frame);

    @Override
    public Object executeGeneric(VirtualFrame frame) throws LocalReturn {
        throw new LocalReturn(getConstant(frame));
    }
}
