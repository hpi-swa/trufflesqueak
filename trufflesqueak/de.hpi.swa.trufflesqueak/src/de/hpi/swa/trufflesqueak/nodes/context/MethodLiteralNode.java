package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.instrumentation.PrettyPrintVisitor;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithMethod;

public class MethodLiteralNode extends SqueakNodeWithMethod {
    final Object literal;

    public MethodLiteralNode(CompiledCodeObject cm, int idx) {
        super(cm);
        literal = cm.getLiteral(idx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return literal;
    }

    @Override
    public void prettyPrintOn(PrettyPrintVisitor b) {
        if (literal instanceof PointersObject && ((PointersObject) literal).size() == 2) {
            b.append(((PointersObject) literal).at0(0));
        } else {
            b.append(literal);
        }
    }
}
