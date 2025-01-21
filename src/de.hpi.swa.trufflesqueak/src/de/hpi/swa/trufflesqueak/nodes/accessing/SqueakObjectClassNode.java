/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

@GenerateInline
@GenerateUncached
@GenerateCached(false)
public abstract class SqueakObjectClassNode extends AbstractNode {

    public static SqueakObjectClassNode getUncached() {
        return SqueakObjectClassNodeGen.getUncached();
    }

    public abstract ClassObject executeLookup(Node node, Object receiver);

    public abstract ClassObject executeLookup(Node node, AbstractSqueakObject receiver);

    public static final ClassObject executeUncached(final Object receiver) {
        return SqueakObjectClassNodeGen.getUncached().executeLookup(null, receiver);
    }

    @Specialization
    protected static final ClassObject doNil(@SuppressWarnings("unused") final NilObject value, @Bind final SqueakImageContext image) {
        return image.nilClass;
    }

    @Specialization(guards = "value == TRUE")
    protected static final ClassObject doTrue(@SuppressWarnings("unused") final boolean value, @Bind final SqueakImageContext image) {
        return image.trueClass;
    }

    @Specialization(guards = "value != TRUE")
    protected static final ClassObject doFalse(@SuppressWarnings("unused") final boolean value, @Bind final SqueakImageContext image) {
        return image.falseClass;
    }

    @Specialization
    protected static final ClassObject doSmallInteger(@SuppressWarnings("unused") final long value, @Bind final SqueakImageContext image) {
        return image.smallIntegerClass;
    }

    @Specialization
    protected static final ClassObject doChar(@SuppressWarnings("unused") final char value, @Bind final SqueakImageContext image) {
        return image.characterClass;
    }

    @Specialization
    protected static final ClassObject doDouble(@SuppressWarnings("unused") final double value, @Bind final SqueakImageContext image) {
        return image.smallFloatClass;
    }

    @Specialization
    protected static final ClassObject doCharacter(@SuppressWarnings("unused") final CharacterObject value, @Bind final SqueakImageContext image) {
        return image.characterClass;
    }

    @Specialization
    protected static final ClassObject doContext(@SuppressWarnings("unused") final ContextObject value, @Bind final SqueakImageContext image) {
        return image.methodContextClass;
    }

    @Specialization
    protected static final ClassObject doFloat(@SuppressWarnings("unused") final FloatObject value, @Bind final SqueakImageContext image) {
        return image.floatClass;
    }

    @Specialization
    protected static final ClassObject doAbstractSqueakObjectWithClassAndHash(final AbstractSqueakObjectWithClassAndHash value) {
        return value.getSqueakClass();
    }

    @Specialization(guards = {"!isAbstractSqueakObject(value)", "!isUsedJavaPrimitive(value)"})
    protected static final ClassObject doForeignObject(@SuppressWarnings("unused") final Object value, @Bind final SqueakImageContext image) {
        return image.getForeignObjectClass();
    }
}
