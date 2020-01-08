/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.BooleanObject;
import de.hpi.swa.graal.squeak.model.CharacterObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;

@GenerateUncached
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

    @Specialization
    protected static final boolean doCharacterObject(final CharacterObject a, final CharacterObject b) {
        return BooleanObject.wrap(a.getValueRaw() == b.getValueRaw());
    }

    @Fallback
    protected static final boolean doObject(final Object a, final Object b) {
        return BooleanObject.wrap(a == b);
    }
}
