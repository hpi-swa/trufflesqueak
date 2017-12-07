package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;

public class MethodLiteralNode extends SqueakNodeWithCode {
    public final Object literal;

    public MethodLiteralNode(CompiledCodeObject code, int index) {
        super(code);
        literal = code.getLiteral(index);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return literal;
    }
}
