/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

@GenerateInline(true)
@NodeInfo(cost = NodeCost.NONE)
public abstract class SqueakObjectIdentityNode extends AbstractNode {

    public abstract boolean execute(Node node, Object left, Object right);

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

    @SuppressWarnings("unused")
    @Specialization(guards = "isUsedJavaPrimitive(left)")
    protected static final boolean doPrimitiveAbstractSqueakObject(final Object left, final AbstractSqueakObject right) {
        return BooleanObject.FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isUsedJavaPrimitive(right)")
    protected static final boolean doAbstractSqueakObjectPrimitive(final AbstractSqueakObject left, final Object right) {
        return BooleanObject.FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isUsedJavaPrimitive(left)", "isUsedJavaPrimitive(right)", "left.getClass() != right.getClass()"})
    protected static final boolean doNonComparablePrimitives(final Object left, final Object right) {
        return BooleanObject.FALSE;
    }

    @Specialization(guards = "!isCharacterObject(left)")
    protected static final boolean doAbstractSqueakObject(final AbstractSqueakObject left, final Object right) {
        return BooleanObject.wrap(left == right);
    }

    @Specialization
    protected static final boolean doCharacterObject(final CharacterObject left, final CharacterObject right) {
        return BooleanObject.wrap(left.getValue() == right.getValue());
    }

    @Specialization(replaces = "doCharacterObject")
    protected static final boolean doCharacterObjectGeneric(final CharacterObject left, final Object right) {
        if (right instanceof CharacterObject) {
            return doCharacterObject(left, (CharacterObject) right);
        } else {
            return BooleanObject.FALSE;
        }
    }

    @Specialization(guards = "isForeignObject(left)")
    protected static final boolean doForeignAbstractSqueakObject(final Object left, final AbstractSqueakObject right) {
        return BooleanObject.wrap(left == right);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "left == right")
    protected static final boolean doJavaIdenticalObject(final Object left, final Object right) {
        return BooleanObject.TRUE;
    }

    /** (inspired by SimpleLanguage's {@code SLEqualNode}). */
    @Specialization(guards = "isForeignObject(left) || isForeignObject(right)", limit = "4")
    protected static final boolean doForeignObject(final Object left, final Object right,
                    @CachedLibrary("left") final InteropLibrary leftInterop,
                    @CachedLibrary("right") final InteropLibrary rightInterop) {
        try {
            if (leftInterop.isBoolean(left) && rightInterop.isBoolean(right)) {
                return doBoolean(leftInterop.asBoolean(left), rightInterop.asBoolean(right));
            } else if (leftInterop.isNull(left) && rightInterop.isNull(right)) {
                return BooleanObject.TRUE;
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
                return BooleanObject.wrap(left == right);
            }
        } catch (final UnsupportedMessageException e) {
            throw CompilerDirectives.shouldNotReachHere(e);
        }
    }
}
