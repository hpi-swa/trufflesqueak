package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithMethod;

public class PrimitiveNode extends SqueakNodeWithMethod {
    public PrimitiveNode(CompiledMethodObject cm) {
        super(cm);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) throws LocalReturn {
        return null;
    }

    public PrimitiveNode mostSpecializedVersion() {
        return this;
    }
}
