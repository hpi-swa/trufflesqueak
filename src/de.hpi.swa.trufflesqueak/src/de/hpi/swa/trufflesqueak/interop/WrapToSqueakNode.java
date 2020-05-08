/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.interop;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.util.MiscUtils;

@GenerateUncached
public abstract class WrapToSqueakNode extends AbstractNode {

    public static WrapToSqueakNode getUncached() {
        return WrapToSqueakNodeGen.getUncached();
    }

    public abstract Object executeWrap(Object value);

    public final Object[] executeObjects(final Object... values) {
        final Object[] wrappedElements = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            wrappedElements[i] = executeWrap(values[i]);
        }
        return wrappedElements;
    }

    public final ArrayObject executeList(final Object... values) {
        return (ArrayObject) executeWrap(values);
    }

    @Specialization
    protected static final boolean doBoolean(final boolean value) {
        return value;
    }

    @Specialization
    protected static final long doByte(final byte value) {
        return value;
    }

    @Specialization
    protected static final long doShort(final short value) {
        return value;
    }

    @Specialization
    protected static final long doInteger(final int value) {
        return value;
    }

    @Specialization
    protected static final long doLong(final long value) {
        return value;
    }

    @Specialization
    protected static final double doFloat(final float value) {
        return value;
    }

    @Specialization
    protected static final double doDouble(final double value) {
        return value;
    }

    @Specialization
    protected static final NativeObject doString(final String value,
                    @Cached("createBinaryProfile()") final ConditionProfile wideStringProfile,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.asString(value, wideStringProfile);
    }

    @Specialization
    protected static final NativeObject doChar(final char value,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.asByteString(MiscUtils.stringValueOf(value));
    }

    @Specialization
    protected final ArrayObject doObjects(final Object[] values,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.asArrayOfObjects(executeObjects(values));
    }

    @Fallback
    protected static final Object doObject(final Object value) {
        return value;
    }
}
