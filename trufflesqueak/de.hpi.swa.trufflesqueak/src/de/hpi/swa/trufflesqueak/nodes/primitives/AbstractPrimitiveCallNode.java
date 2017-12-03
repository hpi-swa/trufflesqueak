package de.hpi.swa.trufflesqueak.nodes.primitives;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public abstract class AbstractPrimitiveCallNode extends PrimitiveNode {
    private AbstractPrimitiveCallNode() {
        super(null);
    }

    public static PrimitiveNode create(CompiledMethodObject code) {
        Object descriptor = code.getLiteral(0);
        if (descriptor instanceof BaseSqueakObject && ((BaseSqueakObject) descriptor).size() >= 2) {
            String modulename = ((BaseSqueakObject) descriptor).at0(0).toString();
            String functionname = ((BaseSqueakObject) descriptor).at0(1).toString();
            return PrimitiveNodeFactory.forName(code, modulename, functionname);
        }
        return new PrimitiveNode(code);
    }
}
