package de.hpi.swa.trufflesqueak.nodes.context;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.instrumentation.PrettyPrintVisitor;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.FalseObject;
import de.hpi.swa.trufflesqueak.model.LargeInteger;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.SmallInteger;
import de.hpi.swa.trufflesqueak.model.TrueObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithMethod;

public abstract class MethodLiteralNode extends SqueakNodeWithMethod {
    final Object literal;

    public MethodLiteralNode(CompiledCodeObject cm, int idx) {
        super(cm);
        literal = cm.getLiteral(idx);
    }

    @Specialization(guards = {"isSmallInteger()"}, rewriteOn = ArithmeticException.class)
    public int literalInt() {
        return Math.toIntExact(((SmallInteger) literal).getValue());
    }

    @Specialization(guards = {"isSmallInteger()"})
    public long literalLong() {
        return ((SmallInteger) literal).getValue();
    }

    @Specialization(guards = {"isLargeInteger()"}, rewriteOn = ArithmeticException.class)
    public long literalLargeLong() {
        return ((LargeInteger) literal).getValue().longValueExact();
    }

    @Specialization(guards = {"isLargeInteger()"})
    public BigInteger literalLarge() {
        return ((LargeInteger) literal).getValue();
    }

    @Specialization(guards = {"isBoolean()"})
    public boolean literalBoolean() {
        return literal instanceof TrueObject;
    }

    @Specialization
    public Object literalObject() {
        return literal;
    }

    protected boolean isSmallInteger() {
        return literal instanceof SmallInteger;
    }

    protected boolean isLargeInteger() {
        return literal instanceof LargeInteger;
    }

    protected boolean isBoolean() {
        return literal instanceof TrueObject || literal instanceof FalseObject;
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
