/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.interop;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;

import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.FloatObjectNodes.AsFloatObjectIfNessaryNode;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

@GenerateInline
@GenerateUncached
@GenerateCached(false)
public abstract class WrapToSqueakNode extends AbstractNode {

    public abstract Object executeWrap(Node node, Object value);

    public static final Object executeUncached(final Object value) {
        return WrapToSqueakNodeGen.getUncached().executeWrap(null, value);
    }

    public final Object[] executeObjects(final Node node, final Object... values) {
        final Object[] wrappedElements = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            wrappedElements[i] = executeWrap(node, values[i]);
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
    protected static final Object doFloat(final Node node, final float value,
                    @Shared("boxNode") @Cached final AsFloatObjectIfNessaryNode boxNode) {
        return boxNode.execute(node, value);
    }

    @Specialization
    protected static final Object doDouble(final Node node, final double value,
                    @Shared("boxNode") @Cached final AsFloatObjectIfNessaryNode boxNode) {
        return boxNode.execute(node, value);
    }

    @Specialization
    protected static final NativeObject doString(final Node node, final String value,
                    @Shared("wideStringProfile") @Cached final InlinedConditionProfile wideStringProfile) {
        return getContext(node).asString(value, wideStringProfile, node);
    }

    @Specialization
    protected static final NativeObject doTruffleString(final Node node, final TruffleString value,
                    @Shared("wideStringProfile") @Cached final InlinedConditionProfile wideStringProfile) {
        return getContext(node).asString(value, wideStringProfile, node);
    }

    @Specialization
    protected static final NativeObject doChar(final Node node, final char value) {
        return getContext(node).asByteString(MiscUtils.stringValueOf(value));
    }

    @Specialization
    protected final ArrayObject doObjects(final Node node, final Object[] values) {
        // Avoid recursive explosions in inlining by putting the recursive call behind a boundary.
        return getContext(node).asArrayOfObjects(executeObjectsWithBoundary(node, values));
    }

    @TruffleBoundary
    private Object[] executeObjectsWithBoundary(final Node node, final Object[] values) {
        return executeObjects(node, values);
    }

    @Fallback
    protected static final Object doObject(final Object value) {
        return value;
    }
}
