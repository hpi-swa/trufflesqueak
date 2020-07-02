/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

@GenerateUncached
@NodeInfo(cost = NodeCost.NONE)
public abstract class SqueakObjectClassNode extends AbstractNode {
    public static SqueakObjectClassNode create() {
        return SqueakObjectClassNodeGen.create();
    }

    public static SqueakObjectClassNode getUncached() {
        return SqueakObjectClassNodeGen.getUncached();
    }

    public abstract ClassObject executeLookup(Object receiver);

    @Specialization
    protected static final ClassObject doNil(@SuppressWarnings("unused") final NilObject value,
                    @Shared("image") @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.nilClass;
    }

    @Specialization(guards = "value == TRUE")
    protected static final ClassObject doTrue(@SuppressWarnings("unused") final boolean value,
                    @Shared("image") @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.trueClass;
    }

    @Specialization(guards = "value != TRUE")
    protected static final ClassObject doFalse(@SuppressWarnings("unused") final boolean value,
                    @Shared("image") @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.falseClass;
    }

    @Specialization
    protected static final ClassObject doSmallInteger(@SuppressWarnings("unused") final long value,
                    @Shared("image") @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.smallIntegerClass;
    }

    @Specialization
    protected static final ClassObject doChar(@SuppressWarnings("unused") final char value,
                    @Shared("image") @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.characterClass;
    }

    @Specialization
    protected static final ClassObject doDouble(@SuppressWarnings("unused") final double value,
                    @Shared("image") @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.getSmallFloatClass();
    }

    @Specialization
    protected static final ClassObject doClosure(@SuppressWarnings("unused") final BlockClosureObject value,
                    @Shared("image") @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.blockClosureClass;
    }

    @Specialization
    protected static final ClassObject doCharacter(@SuppressWarnings("unused") final CharacterObject value,
                    @Shared("image") @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.characterClass;
    }

    @Specialization
    protected static final ClassObject doContext(@SuppressWarnings("unused") final ContextObject value,
                    @Shared("image") @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.methodContextClass;
    }

    @Specialization
    protected static final ClassObject doFloat(@SuppressWarnings("unused") final FloatObject value,
                    @Shared("image") @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.floatClass;
    }

    @Specialization
    protected static final ClassObject doAbstractSqueakObjectWithClassAndHash(final AbstractSqueakObjectWithClassAndHash value,
                    @Cached("createIdentityProfile()") final ValueProfile classProfile) {
        return classProfile.profile(value.getSqueakClass());
    }

    @Specialization(guards = {"!isAbstractSqueakObject(value)", "!isUsedJavaPrimitive(value)"})
    protected static final ClassObject doForeignObject(@SuppressWarnings("unused") final Object value,
                    @Shared("image") @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.getForeignObjectClass();
    }
}
