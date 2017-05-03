package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.FalseObject;
import de.hpi.swa.trufflesqueak.model.SmallInteger;
import de.hpi.swa.trufflesqueak.model.TrueObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public abstract class MethodLiteralNode extends SqueakNode {
    final Object literal;

    public MethodLiteralNode(CompiledMethodObject cm, int idx) {
        literal = cm.getLiteral(idx);
    }

    @Specialization(guards = {"isSmallInteger()"})
    public int literalInt() {
        return ((SmallInteger) literal).getValue();
    }

    @Specialization(guards = {"isBoolean()"})
    public boolean literalBoolean() {
        return literal instanceof TrueObject;
    }

    @Specialization(replaces = {"literalInt"})
    public Object literalObject() {
        return literal;
    }

    protected boolean isSmallInteger() {
        return literal instanceof SmallInteger;
    }

    protected boolean isBoolean() {
        return literal instanceof TrueObject || literal instanceof FalseObject;
    }
}
