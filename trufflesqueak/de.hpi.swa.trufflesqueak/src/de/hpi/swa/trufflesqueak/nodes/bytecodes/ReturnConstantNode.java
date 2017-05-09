package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class ReturnConstantNode extends ReturnNode {
    public ReturnConstantNode(CompiledCodeObject method, int idx, Object obj) {
        super(method, idx);
        valueNode = new ConstantNode(method, idx, obj);
    }
}
