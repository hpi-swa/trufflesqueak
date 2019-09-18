/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CharacterObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.VariablePointersObject;
import de.hpi.swa.graal.squeak.model.WeakVariablePointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;

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
        return image.smallFloatClass;
    }

    @Specialization
    protected static final ClassObject doPointers(final PointersObject value) {
        return value.getSqueakClass();
    }

    @Specialization
    protected static final ClassObject doVariablePointers(final VariablePointersObject value) {
        return value.getSqueakClass();
    }

    @Specialization
    protected static final ClassObject doWeakPointers(final WeakVariablePointersObject value) {
        return value.getSqueakClass();
    }

    @Specialization
    protected static final ClassObject doArray(final ArrayObject value) {
        return value.getSqueakClass();
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
    protected static final ClassObject doClass(final ClassObject value) {
        return value.getSqueakClass();
    }

    @Specialization
    protected static final ClassObject doMethod(final CompiledMethodObject value) {
        return value.image.compiledMethodClass;
    }

    @Specialization
    protected static final ClassObject doContext(@SuppressWarnings("unused") final ContextObject value,
                    @Shared("image") @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.methodContextClass;
    }

    @Specialization
    protected static final ClassObject doEmpty(final EmptyObject value) {
        return value.getSqueakClass();
    }

    @Specialization
    protected static final ClassObject doFloat(@SuppressWarnings("unused") final FloatObject value,
                    @Shared("image") @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.floatClass;
    }

    @Specialization
    protected static final ClassObject doLargeInteger(final LargeIntegerObject value) {
        return value.getSqueakClass();
    }

    @Specialization
    protected static final ClassObject doNative(final NativeObject value) {
        return value.getSqueakClass();
    }

    @Specialization(guards = {"!isAbstractSqueakObject(value)", "!isUsedJavaPrimitive(value)"})
    protected static final ClassObject doTruffleObject(@SuppressWarnings("unused") final Object value,
                    @Shared("image") @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        assert image.supportsTruffleObject();
        return image.truffleObjectClass;
    }
}
