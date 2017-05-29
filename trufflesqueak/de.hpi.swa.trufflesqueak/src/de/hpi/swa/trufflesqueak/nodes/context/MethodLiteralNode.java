package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.instrumentation.PrettyPrintVisitor;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithMethod;

public class MethodLiteralNode extends SqueakNodeWithMethod {
    public final Object literal;

    public MethodLiteralNode(CompiledCodeObject cm, int idx) {
        super(cm);
        literal = cm.getLiteral(idx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return literal;
    }

    @Override
    public void accept(PrettyPrintVisitor b) {
        b.visit(this);
    }
}
