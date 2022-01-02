/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

@NodeInfo(cost = NodeCost.NONE)
public abstract class SqueakObjectIdentityNode extends AbstractNode {

    public abstract boolean execute(Object left, Object right);

    @Specialization
    protected static final boolean doBoolean(final boolean left, final boolean right) {
        return BooleanObject.wrap(left == right);
    }

    @Specialization
    protected static final boolean doChar(final char left, final char right) {
        return BooleanObject.wrap(left == right);
    }

    @Specialization
    protected static final boolean doLong(final long left, final long right) {
        return BooleanObject.wrap(left == right);
    }

    @Specialization
    protected static final boolean doDouble(final double left, final double right) {
        return BooleanObject.wrap(Double.doubleToRawLongBits(left) == Double.doubleToRawLongBits(right));
    }

    @Specialization
    protected static final boolean doCharacterObject(final CharacterObject left, final CharacterObject right) {
        return BooleanObject.wrap(left.getValue() == right.getValue());
    }

    @Specialization
    protected static final boolean doSqueakObjectLeft(final AbstractSqueakObject left, final Object right) {
        return BooleanObject.wrap(left == right);
    }

    @Specialization
    protected static final boolean doSqueakObjectRight(final Object left, final AbstractSqueakObject right) {
        return BooleanObject.wrap(left == right);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "left == right")
    protected static final boolean doJavaIdenticalObject(final Object left, final Object right) {
        return BooleanObject.TRUE;
    }

    /** (inspired by SimpleLanguage's {@code SLEqualNode}). */
    @Specialization(guards = "left != right", limit = "4")
    protected static final boolean doObject(final Object left, final Object right,
                    @CachedLibrary("left") final InteropLibrary leftInterop,
                    @CachedLibrary("right") final InteropLibrary rightInterop) {
        try {
            if (leftInterop.isBoolean(left) && rightInterop.isBoolean(right)) {
                return doBoolean(leftInterop.asBoolean(left), rightInterop.asBoolean(right));
            } else if (leftInterop.isNull(left) && rightInterop.isNull(right)) {
                return true;
            } else if (leftInterop.fitsInLong(left) && rightInterop.fitsInLong(right)) {
                return doLong(leftInterop.asLong(left), rightInterop.asLong(right));
            } else if (leftInterop.fitsInDouble(left) && rightInterop.fitsInDouble(right)) {
                return doDouble(leftInterop.asDouble(left), rightInterop.asDouble(right));
            } else if (leftInterop.hasIdentity(left) && rightInterop.hasIdentity(right)) {
                return BooleanObject.wrap(leftInterop.isIdentical(left, right, rightInterop));
            } else if (left instanceof Character && right instanceof Character) {
                return doChar((Character) left, (Character) right);
            } else if (left instanceof CharacterObject && right instanceof CharacterObject) {
                return doCharacterObject((CharacterObject) left, (CharacterObject) right);
            } else {
                return false;
            }
        } catch (final UnsupportedMessageException e) {
            throw CompilerDirectives.shouldNotReachHere(e);
        }
    }
}
