package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class MethodLiteralNode extends ContextAccessNode {
    Object literal;

    public MethodLiteralNode(CompiledMethodObject cm, int idx) {
        super(cm);
        literal = cm.getLiteral(idx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return literal;
    }
}
