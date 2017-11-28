package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;

public class MethodLiteralNode extends SqueakNodeWithCode {
    public final Object literal;

    public MethodLiteralNode(CompiledCodeObject cm, int idx) {
        super(cm);
        literal = cm.getLiteral(idx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return literal;
    }
}
