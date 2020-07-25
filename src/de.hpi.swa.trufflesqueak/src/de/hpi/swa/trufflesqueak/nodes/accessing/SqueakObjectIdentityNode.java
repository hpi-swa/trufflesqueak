/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

@NodeInfo(cost = NodeCost.NONE)
public abstract class SqueakObjectIdentityNode extends AbstractNode {

    public abstract boolean execute(Object a, Object b);

    @Specialization
    protected static final boolean doBoolean(final boolean a, final boolean b) {
        return BooleanObject.wrap(a == b);
    }

    @Specialization
    protected static final boolean doChar(final char a, final char b) {
        return BooleanObject.wrap(a == b);
    }

    @Specialization
    protected static final boolean doLong(final long a, final long b) {
        return BooleanObject.wrap(a == b);
    }

    @Specialization
    protected static final boolean doDouble(final double a, final double b) {
        return BooleanObject.wrap(Double.doubleToRawLongBits(a) == Double.doubleToRawLongBits(b));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isDouble(b)")
    protected static final boolean doDoubleFalse(final double a, final Object b) {
        return BooleanObject.FALSE;
    }

    @Specialization
    protected static final boolean doCharacterObject(final CharacterObject a, final CharacterObject b) {
        return BooleanObject.wrap(a.getValue() == b.getValue());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isCharacterObject(b)")
    protected static final boolean doCharacterObjectFalse(final CharacterObject a, final Object b) {
        return BooleanObject.FALSE;
    }

    @Specialization(guards = {"!isDouble(a)", "!isCharacterObject(a)"}, replaces = {"doBoolean", "doChar", "doLong"})
    protected static final boolean doGeneric(final Object a, final Object b) {
        return BooleanObject.wrap(a == b);
    }
}
