package de.hpi.swa.trufflesqueak.nodes.helpers;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;

public abstract class IsNumericNode extends Node {

    public static IsNumericNode create() {
        return IsNumericNodeGen.create();
    }

    public abstract boolean execute(Object value);

    @Specialization
    protected static final boolean doLong(@SuppressWarnings("unused") final long value) {
        return true;
    }

    @Specialization
    protected static final boolean doLargeInteger(@SuppressWarnings("unused") final LargeIntegerObject value) {
        return true;
    }

    @Specialization
    protected static final boolean doDouble(@SuppressWarnings("unused") final double value) {
        return true;
    }

    @Specialization
    protected static final boolean doFloat(@SuppressWarnings("unused") final FloatObject value) {
        return true;
    }

    @Fallback
    protected static final boolean doObject(@SuppressWarnings("unused") final Object value) {
        return false;
    }
}
