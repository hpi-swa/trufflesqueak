/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.interop;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;

import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.FloatObjectNodes.AsFloatObjectIfNessaryNode;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

@GenerateUncached
public abstract class WrapToSqueakNode extends AbstractNode {

    @NeverDefault
    public static WrapToSqueakNode create() {
        return WrapToSqueakNodeGen.create();
    }

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
    protected static final Object doFloat(final float value,
                    @Cached final AsFloatObjectIfNessaryNode boxNode) {
        return boxNode.execute(value);
    }

    @Specialization
    protected static final Object doDouble(final double value,
                    @Cached final AsFloatObjectIfNessaryNode boxNode) {
        return boxNode.execute(value);
    }

    @Specialization
    protected final NativeObject doString(final String value,
                    @Cached final ConditionProfile wideStringProfile) {
        return getContext().asString(value, wideStringProfile);
    }

    @Specialization
    protected final NativeObject doTruffleString(final TruffleString value,
                    @Cached final TruffleString.ToJavaStringNode toJavaString,
                    @Cached final ConditionProfile wideStringProfile) {
        return doString(toJavaString.execute(value), wideStringProfile);
    }

    @Specialization
    protected final NativeObject doChar(final char value) {
        return getContext().asByteString(MiscUtils.stringValueOf(value));
    }

    @Specialization
    protected final ArrayObject doObjects(final Object[] values) {
        // Avoid recursive explosions in inlining by putting the recursive call behind a boundary.
        return getContext().asArrayOfObjects(executeObjectsWithBoundary(values));
    }

    @TruffleBoundary
    private Object[] executeObjectsWithBoundary(final Object[] values) {
        return executeObjects(values);
    }

    @Fallback
    protected static final Object doObject(final Object value) {
        return value;
    }
}
