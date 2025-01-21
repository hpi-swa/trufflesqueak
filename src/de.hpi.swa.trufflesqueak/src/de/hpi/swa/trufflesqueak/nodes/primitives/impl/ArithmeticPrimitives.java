/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigDecimal;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.ExactMath;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.RespecializeException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakGuards;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.FloatObjectNodes.AsFloatObjectIfNessaryNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

public final class ArithmeticPrimitives extends AbstractPrimitiveFactoryHolder {
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 1)
    protected abstract static class PrimAddNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs) {
            return Math.addExact(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        protected static final Object doLongWithOverflow(final long lhs, final long rhs, @Bind final SqueakImageContext image) {
            return LargeIntegerObject.add(image, lhs, rhs);
        }

        @Specialization
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return rhs.add(lhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final double doLongDouble(final long lhs, final double rhs) {
            return lhs + rhs;
        }

        @Specialization
        protected static final Object doLongFloat(final long lhs, final FloatObject rhs,
                        @Bind final Node node,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(node, lhs + rhs.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 2)
    protected abstract static class PrimSubtractNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs) {
            return Math.subtractExact(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        protected static final Object doLongWithOverflow(final long lhs, final long rhs, @Bind final SqueakImageContext image) {
            return LargeIntegerObject.subtract(image, lhs, rhs);
        }

        @Specialization
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return LargeIntegerObject.subtract(lhs, rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final double doLongDouble(final long lhs, final double rhs) {
            return lhs - rhs;
        }

        @Specialization
        protected static final Object doLongFloat(final long lhs, final FloatObject rhs,
                        @Bind final Node node,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(node, lhs - rhs.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 3)
    protected abstract static class PrimLessThanNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs < rhs);
        }

        @Specialization
        protected static final boolean doLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) >= 0);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final boolean doDouble(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.wrap(lhs < rhs);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 4)
    protected abstract static class PrimGreaterThanNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs > rhs);
        }

        @Specialization
        protected static final boolean doLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) <= 0);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final boolean doDouble(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.wrap(lhs > rhs);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 5)
    protected abstract static class PrimLessOrEqualNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs <= rhs);
        }

        @Specialization
        protected static final boolean doLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) > 0);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final boolean doDouble(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.wrap(lhs <= rhs);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 6)
    protected abstract static class PrimGreaterOrEqualNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs >= rhs);
        }

        @Specialization
        protected static final boolean doLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) < 0);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final boolean doDouble(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.wrap(lhs >= rhs);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 7)
    protected abstract static class PrimEqualNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs == rhs);
        }

        @Specialization
        protected static final boolean doLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) == 0);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final boolean doDouble(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.FALSE;
            }
        }

        /** Quick return `false` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(rhs)", "!isPointersObject(rhs)"})
        protected static final boolean doQuickFalse(final long lhs, final AbstractSqueakObject rhs) {
            return BooleanObject.FALSE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 8)
    protected abstract static class PrimNotEqualNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs != rhs);
        }

        @Specialization
        protected static final boolean doLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) != 0);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final boolean doDouble(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.TRUE;
            }
        }

        /** Quick return `true` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(rhs)", "!isPointersObject(rhs)"})
        protected static final boolean doQuickTrue(final long lhs, final AbstractSqueakObject rhs) {
            return BooleanObject.TRUE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 9)
    protected abstract static class PrimMultiplyNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs) {
            return Math.multiplyExact(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        protected static final Object doLongWithOverflow(final long lhs, final long rhs, @Bind final SqueakImageContext image) {
            return LargeIntegerObject.multiply(image, lhs, rhs);
        }

        @Specialization
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return rhs.multiply(lhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()", rewriteOn = RespecializeException.class)
        protected static final double doLongDoubleFinite(final long lhs, final double rhs) throws RespecializeException {
            return ensureFinite(lhs * rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()", replaces = "doLongDoubleFinite")
        protected static final Object doLongDouble(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(node, lhs * rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 10)
    protected abstract static class PrimDivideNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)", "isIntegralWhenDividedBy(lhs, rhs)"})
        protected static final long doLong(final long lhs, final long rhs) {
            return lhs / rhs;
        }

        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)"}, replaces = "doLong")
        protected final Object doLongFraction(final long lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile fractionProfile,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            if (fractionProfile.profile(node, SqueakGuards.isIntegralWhenDividedBy(lhs, rhs))) {
                return lhs / rhs;
            } else {
                return getContext().asFraction(lhs, rhs, writeNode, node);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isOverflowDivision(lhs, rhs)"})
        protected static final LargeIntegerObject doLongOverflow(final long lhs, final long rhs, @Bind final SqueakImageContext image) {
            return LargeIntegerObject.createLongMinOverflowResult(image);
        }

        @Specialization(guards = {"isPrimitiveDoMixedArithmetic()", "!isZero(rhs)"}, rewriteOn = RespecializeException.class)
        protected static final double doLongDoubleFinite(final long lhs, final double rhs) throws RespecializeException {
            return ensureFinite(lhs / rhs);
        }

        @Specialization(guards = {"isPrimitiveDoMixedArithmetic()", "!isZero(rhs)"}, replaces = "doLongDoubleFinite")
        protected static final Object doLongDouble(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(node, lhs / rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 11)
    protected abstract static class PrimFloorModNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        /** Profiled version of {@link Math#floorMod(long, long)}. */
        @Specialization(guards = "rhs != 0")
        protected static final long doLong(final long lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile profile) {
            final long r = lhs % rhs;
            // if the signs are different and modulo not zero, adjust result
            if (profile.profile(node, (lhs ^ rhs) < 0 && r != 0)) {
                return r + rhs;
            } else {
                return r;
            }
        }

        @Specialization(guards = "!rhs.isZero()")
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return rhs.floorModReverseOrder(lhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 12)
    protected abstract static class PrimFloorDivideNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        /** Profiled version of {@link Math#floorDiv(long, long)}. */
        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)"})
        protected static final long doLong(final long lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile profile) {
            final long q = lhs / rhs;
            // if the signs are different and modulo not zero, round down
            if (profile.profile(node, (lhs ^ rhs) < 0 && (q * rhs != lhs))) {
                return q - 1;
            } else {
                return q;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isOverflowDivision(lhs, rhs)"})
        protected static final LargeIntegerObject doLongOverflowDivision(final long lhs, final long rhs, @Bind final SqueakImageContext image) {
            return LargeIntegerObject.createLongMinOverflowResult(image);
        }

        @Specialization(guards = {"!rhs.isZero()"})
        protected static final long doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return LargeIntegerObject.floorDivide(lhs, rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 13)
    protected abstract static class PrimQuoNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)"})
        protected static final long doLong(final long lhs, final long rhs) {
            return lhs / rhs;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isOverflowDivision(lhs, rhs)"})
        protected static final LargeIntegerObject doLongOverflow(final long lhs, final long rhs, @Bind final SqueakImageContext image) {
            return LargeIntegerObject.createLongMinOverflowResult(image);
        }

        @Specialization(guards = {"!rhs.isZero()"})
        protected static final long doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return LargeIntegerObject.divide(lhs, rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 14)
    protected abstract static class PrimBitAndNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final long doLong(final long receiver, final long arg) {
            return receiver & arg;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLongLargeQuick(final long receiver, final LargeIntegerObject arg,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile positiveProfile) {
            return receiver & (positiveProfile.profile(node, receiver >= 0) ? arg.longValue() : arg.longValueExact());
        }

        @Specialization(replaces = "doLongLargeQuick")
        protected static final Object doLongLarge(final long receiver, final LargeIntegerObject arg) {
            return arg.and(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 15)
    protected abstract static class PrimBitOrNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final long doLong(final long receiver, final long arg) {
            return receiver | arg;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLongLargeQuick(final long receiver, final LargeIntegerObject arg) {
            return receiver | arg.longValueExact();
        }

        @Specialization(replaces = "doLongLargeQuick")
        protected static final Object doLongLarge(final long receiver, final LargeIntegerObject arg) {
            return arg.or(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 16)
    protected abstract static class PrimBitXorNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final long doLong(final long receiver, final long arg) {
            return receiver ^ arg;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLongLargeQuick(final long receiver, final LargeIntegerObject arg) {
            return receiver ^ arg.longValueExact();
        }

        @Specialization(replaces = "doLongLargeQuick")
        protected static final Object doLongLarge(final long receiver, final LargeIntegerObject arg) {
            return arg.xor(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 17)
    protected abstract static class PrimBitShiftNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"arg >= 0", "!isLShiftLongOverflow(receiver, arg)"})
        protected static final long doLongPositive(final long receiver, final long arg) {
            return receiver << arg;
        }

        @Specialization(guards = {"arg >= 0", "isLShiftLongOverflow(receiver, arg)"})
        protected static final Object doLongPositiveOverflow(final long receiver, final long arg, @Bind final SqueakImageContext image) {
            /*
             * -1 in check needed, because we do not want to shift a positive long into negative
             * long (most significant bit indicates positive/negative).
             */
            return LargeIntegerObject.shiftLeftPositive(image, receiver, (int) arg);
        }

        @Specialization(guards = {"arg < 0", "inLongSizeRange(arg)"})
        protected static final long doLongNegativeInLongSizeRange(final long receiver, final long arg) {
            /*
             * The result of a right shift can only become smaller than the receiver and 0 or -1 at
             * minimum, so no BigInteger needed here.
             */
            return receiver >> -arg;
        }

        @Specialization(guards = {"arg < 0", "!inLongSizeRange(arg)"})
        protected static final long doLongNegative(final long receiver, @SuppressWarnings("unused") final long arg) {
            return receiver >= 0 ? 0L : -1L;
        }

        protected static final boolean inLongSizeRange(final long arg) {
            return -Long.SIZE < arg;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 18)
    protected abstract static class PrimMakePointNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final PointersObject doPoint(final Object xPos, final Object yPos,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            return image.asPoint(writeNode, node, xPos, yPos);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 20)
    protected abstract static class PrimRemLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"rhs != 0"})
        protected static final long doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.remainder(rhs);
        }

        @Specialization(guards = {"!rhs.isZero()"})
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.remainder(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 21)
    protected abstract static class PrimAddLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.add(rhs);
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.add(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 22)
    protected abstract static class PrimSubtractLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.subtract(rhs);
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.subtract(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 23)
    protected abstract static class PrimLessThanLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) < 0);
        }

        @Specialization
        protected static final boolean doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) < 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 24)
    protected abstract static class PrimGreaterThanLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) > 0);
        }

        @Specialization
        protected static final boolean doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) > 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 25)
    protected abstract static class PrimLessOrEqualLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) <= 0);
        }

        @Specialization
        protected static final boolean doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) <= 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 26)
    protected abstract static class PrimGreaterOrEqualLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) >= 0);
        }

        @Specialization
        protected static final boolean doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) >= 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 27)
    protected abstract static class PrimEqualLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, @SuppressWarnings("unused") final long rhs) {
            assert !lhs.fitsIntoLong() : "non-reduced large integer!";
            return BooleanObject.FALSE;
        }

        @Specialization
        protected static final boolean doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) == 0);
        }

        /** Quick return `false` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(rhs)", "!isPointersObject(rhs)"})
        protected static final boolean doQuickFalse(final LargeIntegerObject lhs, final AbstractSqueakObject rhs) {
            return BooleanObject.FALSE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 28)
    protected abstract static class PrimNotEqualLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, @SuppressWarnings("unused") final long rhs) {
            assert !lhs.fitsIntoLong() : "non-reduced large integer!";
            return BooleanObject.TRUE;
        }

        @Specialization
        protected static final boolean doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) != 0);
        }

        /** Quick return `true` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(rhs)", "!isPointersObject(rhs)"})
        protected static final boolean doQuickTrue(final Object lhs, final AbstractSqueakObject rhs) {
            return BooleanObject.TRUE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 29)
    protected abstract static class PrimMultiplyLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.multiply(rhs);
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.multiply(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 30)
    protected abstract static class PrimDivideLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"rhs != 0", "lhs.isIntegralWhenDividedBy(rhs)"})
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.divide(rhs);
        }

        @Specialization(guards = {"!rhs.isZero()", "lhs.isIntegralWhenDividedBy(rhs)"})
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.divide(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 31)
    protected abstract static class PrimFloorModLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.floorMod(rhs);
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.floorMod(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 32)
    protected abstract static class PrimFloorDivideLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "rhs != 0")
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.floorDivide(rhs);
        }

        @Specialization(guards = "!rhs.isZero()")
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.floorDivide(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 33)
    protected abstract static class PrimQuoLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "rhs != 0")
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final long rhs) {
            return lhs.divide(rhs);
        }

        @Specialization(guards = "!rhs.isZero()")
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.divide(rhs);
        }
    }

    // Squeak/Smalltalk uses LargeIntegers plugin for bit operations instead of primitives 34 to 37.

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 38)
    protected abstract static class PrimFloatAtNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "index == 1")
        protected static final long doHigh(final Object receiver, @SuppressWarnings("unused") final long index,
                        @Bind final Node node,
                        @Shared("toDoubleNode") @Cached final PrimFloatAtReceiverToDoubleNode toDoubleNode) {
            return Integer.toUnsignedLong((int) (Double.doubleToRawLongBits(toDoubleNode.execute(node, receiver)) >> 32));
        }

        @Specialization(guards = "index == 2")
        protected static final long doLow(final Object receiver, @SuppressWarnings("unused") final long index,
                        @Bind final Node node,
                        @Shared("toDoubleNode") @Cached final PrimFloatAtReceiverToDoubleNode toDoubleNode) {
            return Integer.toUnsignedLong((int) Double.doubleToRawLongBits(toDoubleNode.execute(node, receiver)));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"index != 1", "index != 2"})
        protected static final long doDoubleFail(final Object receiver, final long index) {
            throw PrimitiveFailed.BAD_INDEX;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 39)
    protected abstract static class PrimFloatAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "index == 1")
        protected static final long doFloatHigh(final FloatObject receiver, @SuppressWarnings("unused") final long index, final long value) {
            receiver.setHigh(value);
            return value;
        }

        @Specialization(guards = "index == 2")
        protected static final long doFloatLow(final FloatObject receiver, @SuppressWarnings("unused") final long index, final long value) {
            receiver.setLow(value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"index != 1", "index != 2"})
        protected static final long doFloatFail(final FloatObject receiver, final long index, final long value) {
            throw PrimitiveFailed.BAD_INDEX;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 40)
    protected abstract static class PrimAsFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final double doLong(final long receiver) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 41)
    protected abstract static class PrimAddFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doAdd(final FloatObject lhs, final Object rhs,
                        @Bind final Node node,
                        @Cached final PrimFloatArgumentToDoubleNode toDoubleNode,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(node, lhs.getValue() + toDoubleNode.execute(node, rhs));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 42)
    protected abstract static class PrimSubtractFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doSubtract(final FloatObject lhs, final Object rhs,
                        @Bind final Node node,
                        @Cached final PrimFloatArgumentToDoubleNode toDoubleNode,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(node, lhs.getValue() - toDoubleNode.execute(node, rhs));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 43)
    protected abstract static class PrimLessThanFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doDouble(final FloatObject lhs, final double rhs) {
            return BooleanObject.wrap(lhs.getValue() < rhs);
        }

        @Specialization
        protected static final boolean doFloat(final FloatObject lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final boolean doLong(final FloatObject lhsObject, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            final double lhs = lhsObject.getValue();
            if (isExactProfile.profile(node, lhs == rhs)) {
                return BooleanObject.wrap((long) lhs < rhs);
            } else {
                return BooleanObject.wrap(lhs < rhs);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 44)
    protected abstract static class PrimGreaterThanFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doDouble(final FloatObject lhs, final double rhs) {
            return BooleanObject.wrap(lhs.getValue() > rhs);
        }

        @Specialization
        protected static final boolean doFloat(final FloatObject lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final boolean doLong(final FloatObject lhsObject, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            final double lhs = lhsObject.getValue();
            if (isExactProfile.profile(node, lhs == rhs)) {
                return BooleanObject.wrap((long) lhs > rhs);
            } else {
                return BooleanObject.wrap(lhs > rhs);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 45)
    protected abstract static class PrimLessOrEqualFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doDouble(final FloatObject lhs, final double rhs) {
            return BooleanObject.wrap(lhs.getValue() <= rhs);
        }

        @Specialization
        protected static final boolean doFloat(final FloatObject lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final boolean doLong(final FloatObject lhsObject, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            final double lhs = lhsObject.getValue();
            if (isExactProfile.profile(node, lhs == rhs)) {
                return BooleanObject.wrap((long) lhs <= rhs);
            } else {
                return BooleanObject.wrap(lhs <= rhs);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 46)
    protected abstract static class PrimGreaterOrEqualFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doDouble(final FloatObject lhs, final double rhs) {
            return BooleanObject.wrap(lhs.getValue() >= rhs);
        }

        @Specialization
        protected static final boolean doFloat(final FloatObject lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final boolean doLong(final FloatObject lhsObject, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            final double lhs = lhsObject.getValue();
            if (isExactProfile.profile(node, lhs == rhs)) {
                return BooleanObject.wrap((long) lhs >= rhs);
            } else {
                return BooleanObject.wrap(lhs >= rhs);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 47)
    protected abstract static class PrimEqualFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doDouble(final FloatObject lhs, final double rhs) {
            return BooleanObject.wrap(lhs.getValue() == rhs);
        }

        @Specialization
        protected static final boolean doFloat(final FloatObject lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final boolean doLong(final FloatObject lhsObject, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            final double lhs = lhsObject.getValue();
            if (isExactProfile.profile(node, lhs == rhs)) {
                return BooleanObject.wrap((long) lhs == rhs);
            } else {
                return BooleanObject.FALSE;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 48)
    protected abstract static class PrimNotEqualFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doDouble(final FloatObject lhs, final double rhs) {
            return BooleanObject.wrap(lhs.getValue() != rhs);
        }

        @Specialization
        protected static final boolean doFloat(final FloatObject lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final boolean doLong(final FloatObject lhsObject, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            final double lhs = lhsObject.getValue();
            if (isExactProfile.profile(node, lhs == rhs)) {
                return BooleanObject.wrap((long) lhs != rhs);
            } else {
                return BooleanObject.TRUE;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 49)
    protected abstract static class PrimMultiplyFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doMultiply(final FloatObject lhs, final Object rhs,
                        @Bind final Node node,
                        @Cached final PrimFloatArgumentToDoubleNode toDoubleNode,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(node, lhs.getValue() * toDoubleNode.execute(node, rhs));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 50)
    protected abstract static class PrimDivideFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doDivide(final FloatObject lhs, final Object rhs,
                        @Bind final Node node,
                        @Cached final PrimFloatArgumentToDoubleNode toDoubleNode,
                        @Cached final InlinedConditionProfile isZeroProfile,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            final double rhsValue = toDoubleNode.execute(node, rhs);
            if (isZeroProfile.profile(node, rhsValue == 0.0)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            } else {
                return boxNode.execute(node, lhs.getValue() / rhsValue);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 51)
    protected abstract static class PrimFloatTruncatedNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = "inSafeIntegerRange(receiver.getValue())")
        protected static final long doFloat(final FloatObject receiver,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isNegativeProfile) {
            assert receiver.isFinite();
            return truncate(node, isNegativeProfile, receiver.getValue());
        }

        @Specialization(guards = {"!inSafeIntegerRange(receiver.getValue())", "receiver.isFinite()"})
        protected static final Object doFloatExact(final FloatObject receiver, @Bind final SqueakImageContext image) {
            return LargeIntegerObject.truncateExact(image, receiver.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 52)
    protected abstract static class PrimFloatFractionPartNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = "inSafeIntegerRange(receiver.getValue())")
        protected static final double doFloat(final FloatObject receiver) {
            return receiver.getValue() - (long) receiver.getValue();
        }

        @TruffleBoundary
        @Specialization(guards = {"!inSafeIntegerRange(receiver.getValue())", "receiver.isFinite()"})
        protected static final double doFloatExact(final FloatObject receiver) {
            return receiver.getValue() - new BigDecimal(receiver.getValue()).toBigInteger().doubleValue();
        }

        @Specialization(guards = "receiver.isNaN()")
        protected static final FloatObject doFloatNaN(final FloatObject receiver) {
            return receiver.shallowCopy();
        }

        @Specialization(guards = "receiver.isInfinite()")
        protected static final double doFloatInfinite(@SuppressWarnings("unused") final FloatObject receiver,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isNegativeInfinityProfile) {
            return isNegativeInfinityProfile.profile(node, receiver.getValue() == Double.NEGATIVE_INFINITY) ? -0.0D : 0.0D;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 53)
    protected abstract static class PrimFloatExponentNode extends AbstractFloatArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = "receiver.isZero()")
        protected static final long doFloatZero(@SuppressWarnings("unused") final FloatObject receiver) {
            return 0L;
        }

        @Specialization(guards = "!receiver.isZero()")
        protected static final long doFloat(final FloatObject receiver,
                        @Bind final Node node,
                        @Cached final InlinedBranchProfile subnormalFloatProfile) {
            return exponentNonZero(receiver.getValue(), subnormalFloatProfile, node);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 54)
    protected abstract static class PrimFloatTimesTwoPowerNode extends AbstractFloatArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "matissa.isZero() || isZero(exponent)")
        protected static final FloatObject doDoubleZero(final FloatObject matissa, @SuppressWarnings("unused") final long exponent) {
            return matissa; /* Can be either 0.0 or -0.0. */
        }

        @Specialization(guards = {"!matissa.isZero()", "!isZero(exponent)"}, rewriteOn = RespecializeException.class)
        protected static final double doDoubleFinite(final FloatObject matissa, final long exponent) throws RespecializeException {
            return ensureFinite(timesToPower(matissa.getValue(), exponent));
        }

        @Specialization(guards = {"!matissa.isZero()", "!isZero(exponent)"}, replaces = "doDoubleFinite")
        protected static final Object doDouble(final FloatObject matissa, final long exponent,
                        @Bind final Node node,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(node, timesToPower(matissa.getValue(), exponent));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 55)
    protected abstract static class PrimSquareRootNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"receiver.isPositive()", "receiver.isFinite()"})
        protected static final double doFloat(final FloatObject receiver) {
            return Math.sqrt(receiver.getValue());
        }

        @Specialization(guards = {"receiver.isPositiveInfinity()"})
        protected static final FloatObject doFloatPositiveInfinity(final FloatObject receiver) {
            return receiver.shallowCopy();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 56)
    protected abstract static class PrimSinNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"receiver.isFinite()"})
        protected static final double doFloat(final FloatObject receiver) {
            return Math.sin(receiver.getValue());
        }

        @Specialization(guards = {"!receiver.isFinite()"})
        protected static final FloatObject doFloatNotFinite(@SuppressWarnings("unused") final FloatObject receiver, @Bind final SqueakImageContext image) {
            return FloatObject.valueOf(image, Double.NaN);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 57)
    protected abstract static class PrimArcTanNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"!receiver.isNaN()"})
        protected static final double doFloat(final FloatObject receiver) {
            return Math.atan(receiver.getValue());
        }

        @Specialization(guards = {"receiver.isNaN()"})
        protected static final FloatObject doFloatNaN(final FloatObject receiver) {
            return receiver.shallowCopy();
        }
    }

    @ImportStatic(Double.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 58)
    protected abstract static class PrimLogNNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"receiver.isFinite()", "!receiver.isZero()"})
        protected static final double doFloat(final FloatObject receiver) {
            return Math.log(receiver.getValue());
        }

        @Specialization(guards = "receiver.isZero()")
        protected static final FloatObject doFloatZero(@SuppressWarnings("unused") final FloatObject receiver, @Bind final SqueakImageContext image) {
            return FloatObject.valueOf(image, Double.NEGATIVE_INFINITY);
        }

        @Specialization(guards = "receiver.isPositiveInfinity()")
        protected static final FloatObject doFloatPositiveInfinity(final FloatObject receiver) {
            return receiver.shallowCopy();
        }

        @Specialization(guards = "receiver.isNegativeInfinity() || receiver.isNaN()")
        protected static final FloatObject doFloatOthers(@SuppressWarnings("unused") final FloatObject receiver, @Bind final SqueakImageContext image) {
            return FloatObject.valueOf(image, Double.NaN);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 59)
    protected abstract static class PrimExpNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = "receiver.isFinite()")
        protected static final Object doFloat(final FloatObject receiver,
                        @Bind final Node node,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(node, Math.exp(receiver.getValue()));
        }

        @Specialization(guards = "receiver.isNegativeInfinity()")
        protected static final double doFloatNegativeInfinity(@SuppressWarnings("unused") final FloatObject receiver) {
            return 0.0;
        }

        @Specialization(guards = "receiver.isPositiveInfinity() || receiver.isNaN()")
        protected static final FloatObject doFloatOthers(final FloatObject receiver) {
            return receiver.shallowCopy();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 159)
    public abstract static class PrimHashMultiplyNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        public static final int HASH_MULTIPLY_CONSTANT = 1664525;
        public static final int HASH_MULTIPLY_MASK = 0xFFFFFFF;

        @Specialization
        protected static final long doLargeInteger(final LargeIntegerObject receiver) {
            return doLong(receiver.longValue());
        }

        @Specialization
        protected static final long doLong(final long receiver) {
            return receiver * HASH_MULTIPLY_CONSTANT & HASH_MULTIPLY_MASK;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 541)
    protected abstract static class PrimSmallFloatAddFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final double doDouble(final double lhs, final double rhs) {
            return lhs + rhs;
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final double doLong(final double lhs, final long rhs) {
            return doDouble(lhs, rhs);
        }

        @Specialization
        protected static final Object doFloat(final double lhs, final FloatObject rhs,
                        @Bind final Node node,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(node, lhs + rhs.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 542)
    protected abstract static class PrimSmallFloatSubtractFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final double doDouble(final double lhs, final double rhs) {
            return lhs - rhs;
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final double doLong(final double lhs, final long rhs) {
            return doDouble(lhs, rhs);
        }

        @Specialization
        protected static final Object doFloat(final double lhs, final FloatObject rhs,
                        @Bind final Node node,
                        @Shared("boxNode") @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(node, lhs - rhs.getValue());
        }

        @Specialization(guards = "isFraction(rhs, node)")
        protected static final Object doFraction(final double lhs, final PointersObject rhs,
                        @Bind final Node node,
                        @Shared("boxNode") @Cached final AsFloatObjectIfNessaryNode boxNode,
                        @Cached final AbstractPointersObjectNodes.AbstractPointersObjectReadNode readNode) {
            return boxNode.execute(node, lhs - SqueakImageContext.fromFraction(rhs, readNode, node));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 543)
    protected abstract static class PrimSmallFloatLessThanFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs < rhs);
        }

        @Specialization
        protected static final boolean doFloat(final double lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final boolean doLong(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return BooleanObject.wrap((long) lhs < rhs);
            } else {
                return doDouble(lhs, rhs);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 544)
    protected abstract static class PrimSmallFloatGreaterThanFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs > rhs);
        }

        @Specialization
        protected static final boolean doFloat(final double lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final boolean doLong(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return BooleanObject.wrap((long) lhs > rhs);
            } else {
                return doDouble(lhs, rhs);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 545)
    protected abstract static class PrimSmallFloatLessOrEqualFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs <= rhs);
        }

        @Specialization
        protected static final boolean doFloat(final double lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final boolean doLong(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return BooleanObject.wrap((long) lhs <= rhs);
            } else {
                return doDouble(lhs, rhs);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 546)
    protected abstract static class PrimSmallFloatGreaterOrEqualFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs >= rhs);
        }

        @Specialization
        protected static final boolean doFloat(final double lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final boolean doLong(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return BooleanObject.wrap((long) lhs >= rhs);
            } else {
                return doDouble(lhs, rhs);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 547)
    protected abstract static class PrimSmallFloatEqualFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs == rhs);
        }

        @Specialization
        protected static final boolean doFloat(final double lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final boolean doLong(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return BooleanObject.wrap((long) lhs == rhs);
            } else {
                return BooleanObject.FALSE;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 548)
    protected abstract static class PrimSmallFloatNotEqualFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs != rhs);
        }

        @Specialization
        protected static final boolean doFloat(final double lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final boolean doLong(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return BooleanObject.wrap((long) lhs != rhs);
            } else {
                return BooleanObject.TRUE;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 549)
    protected abstract static class PrimSmallFloatMultiplyFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {

        @Specialization(rewriteOn = RespecializeException.class)
        protected static final double doDoubleFinite(final double lhs, final double rhs) throws RespecializeException {
            return ensureFinite(lhs * rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()", rewriteOn = RespecializeException.class)
        protected static final double doLongFinite(final double lhs, final long rhs) throws RespecializeException {
            return doDoubleFinite(lhs, rhs);
        }

        @Specialization(replaces = "doDoubleFinite")
        protected static final Object doDouble(final double lhs, final double rhs,
                        @Bind final Node node,
                        @Shared("boxNode") @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(node, lhs * rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()", replaces = "doLongFinite")
        protected static final Object doLong(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("boxNode") @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return doDouble(lhs, rhs, node, boxNode);
        }

        @Specialization
        protected static final Object doFloat(final double lhs, final FloatObject rhs,
                        @Bind final Node node,
                        @Shared("boxNode") @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return doDouble(lhs, rhs.getValue(), node, boxNode);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 550)
    protected abstract static class PrimSmallFloatDivideFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "!isZero(rhs)", rewriteOn = RespecializeException.class)
        protected static final double doDoubleFinite(final double lhs, final double rhs) throws RespecializeException {
            return ensureFinite(lhs / rhs);
        }

        @Specialization(guards = {"isPrimitiveDoMixedArithmetic()", "!isZero(rhs)"}, rewriteOn = RespecializeException.class)
        protected static final double doLongFinite(final double lhs, final long rhs) throws RespecializeException {
            return doDoubleFinite(lhs, rhs);
        }

        @Specialization(guards = "!isZero(rhs)", replaces = "doDoubleFinite")
        protected static final Object doDouble(final double lhs, final double rhs,
                        @Bind final Node node,
                        @Shared("boxNode") @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(node, lhs / rhs);
        }

        @Specialization(guards = {"isPrimitiveDoMixedArithmetic()", "!isZero(rhs)"}, replaces = "doLongFinite")
        protected static final Object doLong(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("boxNode") @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return doDouble(lhs, rhs, node, boxNode);
        }

        @Specialization(guards = "!rhs.isZero()")
        protected static final Object doFloat(final double lhs, final FloatObject rhs,
                        @Bind final Node node,
                        @Shared("boxNode") @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return doDouble(lhs, rhs.getValue(), node, boxNode);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 551)
    protected abstract static class PrimSmallFloatTruncatedNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = "inSafeIntegerRange(receiver)")
        protected static final long doDouble(final double receiver,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isNegativeProfile) {
            return truncate(node, isNegativeProfile, receiver);
        }

        @Specialization(guards = "!inSafeIntegerRange(receiver)")
        protected static final Object doDoubleExact(final double receiver, @Bind final SqueakImageContext image) {
            return LargeIntegerObject.truncateExact(image, receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 552)
    protected abstract static class PrimSmallFloatFractionPartNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = "inSafeIntegerRange(receiver)")
        protected static final double doDouble(final double receiver) {
            return receiver - (long) receiver;
        }

        @TruffleBoundary
        @Specialization(guards = "!inSafeIntegerRange(receiver)")
        protected static final double doDoubleExact(final double receiver) {
            return receiver - new BigDecimal(receiver).toBigInteger().doubleValue();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 553)
    protected abstract static class PrimSmallFloatExponentNode extends AbstractFloatArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = "isZero(receiver)")
        protected static final long doDoubleZero(@SuppressWarnings("unused") final double receiver) {
            return 0L;
        }

        @Specialization(guards = "!isZero(receiver)")
        protected static final long doDouble(final double receiver,
                        @Bind final Node node,
                        @Cached final InlinedBranchProfile subnormalFloatProfile) {
            return exponentNonZero(receiver, subnormalFloatProfile, node);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 554)
    protected abstract static class PrimSmallFloatTimesTwoPowerNode extends AbstractFloatArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "isZero(matissa) || isZero(exponent)")
        protected static final double doDoubleZero(final double matissa, @SuppressWarnings("unused") final long exponent) {
            return matissa; /* Can be either 0.0 or -0.0. */
        }

        @Specialization(guards = {"!isZero(matissa)", "!isZero(exponent)"}, rewriteOn = RespecializeException.class)
        protected static final double doDoubleFinite(final double matissa, final long exponent) throws RespecializeException {
            return ensureFinite(timesToPower(matissa, exponent));
        }

        @Specialization(guards = {"!isZero(matissa)", "!isZero(exponent)"}, replaces = "doDoubleFinite")
        protected static final Object doDouble(final double matissa, final long exponent,
                        @Bind final Node node,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(node, timesToPower(matissa, exponent));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 555)
    protected abstract static class PrimSquareRootSmallFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"isZeroOrGreater(receiver)"})
        protected static final double doDouble(final double receiver) {
            assert Double.isFinite(receiver);
            return Math.sqrt(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 556)
    protected abstract static class PrimSinSmallFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final double doDouble(final double receiver) {
            assert Double.isFinite(receiver);
            return Math.sin(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 557)
    protected abstract static class PrimArcTanSmallFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final double doDouble(final double receiver) {
            assert Double.isFinite(receiver);
            return Math.atan(receiver);
        }
    }

    @ImportStatic(Double.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 558)
    protected abstract static class PrimLogNSmallFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = "isGreaterThanZero(receiver)")
        protected static final double doDouble(final double receiver) {
            assert Double.isFinite(receiver);
            return Math.log(receiver);
        }

        @Specialization(guards = "isZero(receiver)")
        protected static final FloatObject doFloatZero(@SuppressWarnings("unused") final double receiver, @Bind final SqueakImageContext image) {
            return FloatObject.valueOf(image, Double.NEGATIVE_INFINITY);
        }

        @Specialization(guards = "isLessThanZero(receiver)")
        protected static final FloatObject doDoubleNegative(@SuppressWarnings("unused") final double receiver, @Bind final SqueakImageContext image) {
            return FloatObject.valueOf(image, Double.NaN);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 559)
    protected abstract static class PrimExpSmallFloatNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization(rewriteOn = RespecializeException.class)
        protected static final double doDoubleFinite(final double receiver) throws RespecializeException {
            assert Double.isFinite(receiver);
            return ensureFinite(Math.exp(receiver));
        }

        @Specialization(replaces = "doDoubleFinite")
        protected static final Object doDouble(final double receiver,
                        @Bind final Node node,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            assert Double.isFinite(receiver);
            return boxNode.execute(node, Math.exp(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 575)
    protected abstract static class PrimHighBitNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final long doLong(final long receiver,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile negativeProfile) {
            return Long.SIZE - Long.numberOfLeadingZeros(negativeProfile.profile(node, receiver < 0) ? -receiver : receiver);
        }
    }

    @ImportStatic(Double.class)
    public abstract static class AbstractArithmeticPrimitiveNode extends AbstractPrimitiveNode {
        private static final long MAX_SAFE_INTEGER_LONG = (1L << FloatObject.PRECISION) - 1;
        private static final long MIN_SAFE_INTEGER_LONG = -MAX_SAFE_INTEGER_LONG;

        public static final double ensureFinite(final double value) throws RespecializeException {
            if (Double.isFinite(value)) {
                return value;
            } else {
                throw RespecializeException.transferToInterpreterInvalidateAndThrow();
            }
        }

        /** Profiled version of {@link ExactMath#truncate(double)}. */
        protected static final long truncate(final Node node, final InlinedConditionProfile profile, final double value) {
            return (long) (profile.profile(node, value < 0.0) ? Math.ceil(value) : Math.floor(value));
        }

        protected static final boolean inSafeIntegerRange(final double d) {
            // The ends of the interval are also included, since they are powers of two
            return MIN_SAFE_INTEGER_LONG <= d && d <= MAX_SAFE_INTEGER_LONG;
        }

        protected static final long rhsNegatedOnDifferentSign(final long lhs, final long rhs, final InlinedConditionProfile differentSignProfile, final Node node) {
            return differentSignProfile.profile(node, differentSign(lhs, rhs)) ? -rhs : rhs;
        }

        private static boolean differentSign(final long lhs, final long rhs) {
            return lhs < 0 ^ rhs < 0;
        }
    }

    protected abstract static class AbstractFloatArithmeticPrimitiveNode extends AbstractArithmeticPrimitiveNode {
        private static final int LARGE_NUMBER_EXP = 64;
        private static final double LARGE_NUMBER = Math.pow(2, LARGE_NUMBER_EXP);

        protected static final double timesToPower(final double matissa, final long exponent) {
            return Math.scalb(matissa, (int) MiscUtils.clamp(exponent, Integer.MIN_VALUE, Integer.MAX_VALUE));
        }

        protected static final long exponentNonZero(final double receiver, final InlinedBranchProfile subnormalFloatProfile, final Node node) {
            final int exp = Math.getExponent(receiver);
            if (exp == Double.MIN_EXPONENT - 1) {
                // we have a subnormal float (actual zero was handled above)
                subnormalFloatProfile.enter(node);
                // make it normal by multiplying a large number and subtract the number's exponent
                return Math.getExponent(receiver * LARGE_NUMBER) - LARGE_NUMBER_EXP;
            } else {
                return exp;
            }
        }
    }

    @GenerateInline
    @GenerateCached(false)
    protected abstract static class PrimFloatAtReceiverToDoubleNode extends AbstractNode {
        protected abstract double execute(Node inliningTarget, Object value);

        @Specialization
        protected static final double doDouble(final double value) {
            return value;
        }

        @Specialization
        protected static final double doFloat(final FloatObject value) {
            return value.getValue();
        }

        @Fallback
        protected static final double doFallback(@SuppressWarnings("unused") final Object value) {
            throw PrimitiveFailed.BAD_RECEIVER;
        }
    }

    @GenerateInline
    @GenerateCached(false)
    protected abstract static class PrimFloatArgumentToDoubleNode extends AbstractNode {
        protected abstract double execute(Node inliningTarget, Object value);

        @Specialization
        protected static final double doDouble(final double value) {
            return value;
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final double doLong(final long value) {
            return value;
        }

        @Specialization
        protected static final double doFloat(final FloatObject value) {
            return value.getValue();
        }

        @Fallback
        protected static final double doFallback(@SuppressWarnings("unused") final Object value) {
            throw PrimitiveFailed.BAD_ARGUMENT;
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArithmeticPrimitivesFactory.getFactories();
    }
}
