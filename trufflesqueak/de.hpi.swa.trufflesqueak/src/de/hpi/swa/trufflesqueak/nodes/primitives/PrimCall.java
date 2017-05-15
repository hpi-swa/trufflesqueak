package de.hpi.swa.trufflesqueak.nodes.primitives;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public abstract class PrimCall extends PrimitiveNode {
    private PrimCall() {
        super(null);
    }

    public static PrimitiveNode create(CompiledMethodObject cm) {
        BaseSqueakObject descriptor = cm.getLiteral(0);
        if (descriptor.size() < 2) {
            return new PrimitiveNode(cm);
        }
        String modulename = descriptor.at0(0).toString();
        String functionname = descriptor.at0(1).toString();
        return PrimitiveNodeFactory.forName(cm, modulename, functionname);
    }
}
