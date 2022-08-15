/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

@GenerateUncached
@NodeInfo(cost = NodeCost.NONE)
public abstract class SqueakObjectClassIndexNode extends AbstractNode {
    public static SqueakObjectClassIndexNode create() {
        return SqueakObjectClassIndexNodeGen.create();
    }

    public static SqueakObjectClassIndexNode getUncached() {
        return SqueakObjectClassIndexNodeGen.getUncached();
    }

    public abstract int executeLookup(Object receiver);

    public abstract int executeLookup(AbstractSqueakObject receiver);

    @Specialization
    protected final int doNil(@SuppressWarnings("unused") final NilObject value) {
        return getContext().nilClass.asClassIndex();
    }

    @Specialization(guards = "value == TRUE")
    protected final int doTrue(@SuppressWarnings("unused") final boolean value) {
        return getContext().trueClassIndex;
    }

    @Specialization(guards = "value != TRUE")
    protected final int doFalse(@SuppressWarnings("unused") final boolean value) {
        return getContext().falseClassIndex;
    }

    @Specialization
    protected final int doSmallInteger(@SuppressWarnings("unused") final long value) {
        return getContext().smallIntegerClassIndex;
    }

    @Specialization
    protected final int doChar(@SuppressWarnings("unused") final char value) {
        return getContext().characterClassIndex;
    }

    @Specialization
    protected final int doDouble(@SuppressWarnings("unused") final double value) {
        return getContext().smallFloatClassIndex;
    }

    @Specialization
    protected final int doCharacter(@SuppressWarnings("unused") final CharacterObject value) {
        return getContext().characterClassIndex;
    }

    @Specialization
    protected final int doFloat(@SuppressWarnings("unused") final FloatObject value) {
        return getContext().floatClassIndex;
    }

    @Specialization
    protected static final int doAbstractSqueakObjectWithClassAndHash(final AbstractSqueakObjectWithClassAndHash value) {
        return value.getSqueakClassIndex();
    }

    @Specialization(guards = {"!isAbstractSqueakObject(value)", "!isUsedJavaPrimitive(value)"})
    protected final int doForeignObject(@SuppressWarnings("unused") final Object value) {
        return getContext().getForeignObjectClassIndex();
    }
}
