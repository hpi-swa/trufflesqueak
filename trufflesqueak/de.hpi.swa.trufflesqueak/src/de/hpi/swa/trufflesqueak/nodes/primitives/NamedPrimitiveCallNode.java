package de.hpi.swa.trufflesqueak.nodes.primitives;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class NamedPrimitiveCallNode extends PrimitiveNode {
    private NamedPrimitiveCallNode(CompiledMethodObject code) {
        super(code);
    }

    public static PrimitiveNode create(CompiledMethodObject code) {
        BaseSqueakObject descriptor = code.getLiteral(0) instanceof BaseSqueakObject ? (BaseSqueakObject) code.getLiteral(0) : null;
        if (descriptor != null && descriptor.getSqClass() != null && descriptor.size() >= 2) {
            Object descriptorAt0 = descriptor.at0(0);
            Object descriptorAt1 = descriptor.at0(1);
            if (descriptorAt0 != null && descriptorAt1 != null) {
                String modulename = descriptorAt0.toString();
                String functionname = descriptorAt1.toString();
                return PrimitiveNodeFactory.forName(code, modulename, functionname);
            }
        }
        return new PrimitiveNode(code);
    }
}
