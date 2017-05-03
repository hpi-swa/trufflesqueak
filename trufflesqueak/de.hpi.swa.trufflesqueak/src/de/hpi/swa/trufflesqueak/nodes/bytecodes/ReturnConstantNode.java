package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class ReturnConstantNode extends ReturnNode {
    public ReturnConstantNode(CompiledMethodObject cm, int idx, Object obj) {
        super(cm, idx);
        valueNode = new ConstantNode(cm, idx, obj);
    }
}
