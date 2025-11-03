/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import static de.hpi.swa.trufflesqueak.nodes.SqueakGuards.isOverflowDivision;
import static de.hpi.swa.trufflesqueak.nodes.plugins.LargeIntegers.isZero;

import java.util.List;

import com.oracle.truffle.api.ExactMath;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
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
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakGuards;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.FloatObjectNodes.FloatObjectNormalizeNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.LargeIntegers;
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
    public abstract static class PrimAddNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(rewriteOn = ArithmeticException.class)
        public static final long doLong(final long lhs, final long rhs) {
            return Math.addExact(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        public static final Object doLongWithOverflow(final long lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.add(image, lhs, rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        public static final double doLongDouble(final long lhs, final double rhs) {
            return lhs + rhs;
        }

        @Specialization(guards = "image.isLargeInteger(rhs)")
        public static final Object doLongLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return PrimAddLargeIntegersNode.doLargeIntegerLong(rhs, lhs, image);
        }

        @Specialization
        protected static final Object doLongFloat(final long lhs, final FloatObject rhs,
                        @Bind final Node node,
                        @Cached final FloatObjectNormalizeNode normalizeNode) {
            return normalizeNode.execute(node, lhs + rhs.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 2)
    public abstract static class PrimSubtractNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(rewriteOn = ArithmeticException.class)
        public static final long doLong(final long lhs, final long rhs) {
            return Math.subtractExact(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        public static final Object doLongWithOverflow(final long lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.subtract(image, lhs, rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        public static final double doLongDouble(final long lhs, final double rhs) {
            return lhs - rhs;
        }

        @Specialization(guards = "image.isLargeInteger(rhs)")
        public static final Object doLongLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.subtract(image, lhs, rhs);
        }

        @Specialization
        protected static final Object doLongFloat(final long lhs, final FloatObject rhs,
                        @Bind final Node node,
                        @Cached final FloatObjectNormalizeNode normalizeNode) {
            return normalizeNode.execute(node, lhs - rhs.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 3)
    public abstract static class PrimLessThanNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        public static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs < rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        public static final boolean doDouble(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.wrap(lhs < rhs);
            }
        }

        @Specialization(guards = "image.isLargeInteger(rhs)")
        public static final boolean doLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return BooleanObject.wrap(LargeIntegers.compareTo(image, rhs, lhs) > 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 4)
    public abstract static class PrimGreaterThanNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        public static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs > rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        public static final boolean doDouble(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.wrap(lhs > rhs);
            }
        }

        @Specialization(guards = "image.isLargeInteger(rhs)")
        public static final boolean doLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return BooleanObject.wrap(LargeIntegers.compareTo(image, rhs, lhs) < 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 5)
    public abstract static class PrimLessOrEqualNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        public static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs <= rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        public static final boolean doDouble(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.wrap(lhs <= rhs);
            }
        }

        @Specialization(guards = "image.isLargeInteger(rhs)")
        public static final boolean doLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return BooleanObject.wrap(LargeIntegers.compareTo(image, rhs, lhs) >= 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 6)
    public abstract static class PrimGreaterOrEqualNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        public static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs >= rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        public static final boolean doDouble(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.wrap(lhs >= rhs);
            }
        }

        @Specialization(guards = "image.isLargeInteger(rhs)")
        public static final boolean doLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return BooleanObject.wrap(LargeIntegers.compareTo(image, rhs, lhs) <= 0);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 7)
    public abstract static class PrimEqualNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        public static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs == rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        public static final boolean doDouble(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.FALSE;
            }
        }

        @Specialization(guards = "image.isLargeInteger(rhs)")
        public static final boolean doLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return BooleanObject.wrap(LargeIntegers.compareTo(image, rhs, lhs) == 0);
        }

        /** Quick return `false` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(getContext(), rhs)", "!isPointersObject(rhs)"})
        protected static final boolean doQuickFalse(final long lhs, final AbstractSqueakObject rhs) {
            return BooleanObject.FALSE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 8)
    public abstract static class PrimNotEqualNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        public static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs != rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        public static final boolean doDouble(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.TRUE;
            }
        }

        @Specialization(guards = "image.isLargeInteger(rhs)")
        public static final boolean doLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return BooleanObject.wrap(LargeIntegers.compareTo(image, rhs, lhs) != 0);
        }

        /** Quick return `true` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(getContext(), rhs)", "!isPointersObject(rhs)"})
        protected static final boolean doQuickTrue(final long lhs, final AbstractSqueakObject rhs) {
            return BooleanObject.TRUE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 9)
    public abstract static class PrimMultiplyNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(rewriteOn = ArithmeticException.class)
        public static final long doLong(final long lhs, final long rhs) {
            return Math.multiplyExact(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        public static final Object doLongWithOverflow(final long lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.multiply(image, lhs, rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()", rewriteOn = RespecializeException.class)
        public static final double doLongDoubleFinite(final long lhs, final double rhs) throws RespecializeException {
            return ensureFinite(lhs * rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()", replaces = "doLongDoubleFinite")
        public static final Object doLongDouble(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Cached final FloatObjectNormalizeNode normalizeNode) {
            return normalizeNode.execute(node, lhs * rhs);
        }

        @Specialization(guards = "image.isLargeInteger(rhs)")
        public static final Object doLongLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.multiply(image, rhs, lhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 10)
    public abstract static class PrimDivideNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)", "isIntegralWhenDividedBy(lhs, rhs)"})
        public static final long doLong(final long lhs, final long rhs) {
            return lhs / rhs;
        }

        @Specialization(replaces = "doLong")
        public static final Object doLongFraction(final long lhs, final long rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile,
                        @Exclusive @Cached final InlinedConditionProfile isOverflowProfile,
                        @Exclusive @Cached final InlinedConditionProfile isIntegralProfile,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            if (isZeroProfile.profile(node, rhs == 0)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            } else if (isOverflowProfile.profile(node, SqueakGuards.isOverflowDivision(lhs, rhs))) {
                return LargeIntegers.createLongMinOverflowResult(image);
            } else if (isIntegralProfile.profile(node, SqueakGuards.isIntegralWhenDividedBy(lhs, rhs))) {
                return lhs / rhs;
            } else {
                return image.asFraction(lhs, rhs, writeNode, node);
            }
        }

        @Specialization(guards = {"isPrimitiveDoMixedArithmetic()"}, rewriteOn = RespecializeException.class)
        public static final double doLongDoubleFinite(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile) throws RespecializeException {
            if (isZeroProfile.profile(node, rhs == 0)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            } else {
                return ensureFinite(lhs / rhs);
            }
        }

        @Specialization(guards = {"isPrimitiveDoMixedArithmetic()"}, replaces = "doLongDoubleFinite")
        public static final Object doLongDouble(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile,
                        @Cached final FloatObjectNormalizeNode normalizeNode) {
            if (isZeroProfile.profile(node, rhs == 0)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            } else {
                return normalizeNode.execute(node, lhs / rhs);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 11)
    public abstract static class PrimFloorModNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        /** Profiled version of {@link Math#floorMod(long, long)}. */
        @Specialization
        public static final long doLong(final long lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile,
                        @Exclusive @Cached final InlinedConditionProfile sameSignProfile) {
            if (isZeroProfile.profile(node, rhs == 0)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            } else {
                final long r = lhs % rhs;
                // if the signs are different and modulo not zero, adjust result
                if (sameSignProfile.profile(node, (lhs ^ rhs) < 0 && r != 0)) {
                    return r + rhs;
                } else {
                    return r;
                }
            }
        }

        @Specialization(guards = {"image.isLargeInteger(rhs)"})
        protected static final Object doLongLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile) {
            if (isZeroProfile.profile(node, isZero(rhs))) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            } else {
                return LargeIntegers.floorMod(image, lhs, rhs);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 12)
    public abstract static class PrimFloorDivideNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        /** Profiled version of {@link Math#floorDiv(long, long)}. */
        @Specialization
        public static final Object doLong(final long lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile,
                        @Exclusive @Cached final InlinedConditionProfile isOverflowDivisionProfile,
                        @Exclusive @Cached final InlinedConditionProfile sameSignProfile) {
            if (isZeroProfile.profile(node, rhs == 0)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            } else if (isOverflowDivisionProfile.profile(node, isOverflowDivision(lhs, rhs))) {
                return LargeIntegers.createLongMinOverflowResult(getContext(node));
            } else {
                final long q = lhs / rhs;
                // if the signs are different and modulo not zero, round down
                if (sameSignProfile.profile(node, (lhs ^ rhs) < 0 && (q * rhs != lhs))) {
                    return q - 1;
                } else {
                    return q;
                }
            }
        }

        @Specialization(guards = {"image.isLargeInteger(rhs)"})
        public static final long doLongLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile) {
            if (isZeroProfile.profile(node, isZero(rhs))) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            } else {
                return LargeIntegers.floorDivide(image, lhs, rhs);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 13)
    protected abstract static class PrimQuoNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doLong(final long lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile,
                        @Exclusive @Cached final InlinedConditionProfile isOverflowDivisionProfile) {
            if (isZeroProfile.profile(node, rhs == 0)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            } else if (isOverflowDivisionProfile.profile(node, isOverflowDivision(lhs, rhs))) {
                return LargeIntegers.createLongMinOverflowResult(getContext(node));
            } else {
                return lhs / rhs;
            }
        }

        @Specialization(guards = {"image.isLargeInteger(rhs)"})
        protected static final long doLongLargeInteger(final long lhs, final NativeObject rhs,
                        @SuppressWarnings("unused") @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile) {
            if (isZeroProfile.profile(node, isZero(rhs))) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            } else {
                return LargeIntegers.divide(lhs, rhs);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 14)
    public abstract static class PrimBitAndNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        public static final long doLong(final long receiver, final long arg) {
            return receiver & arg;
        }

        @Specialization(guards = {"image.isLargeInteger(arg)"}, rewriteOn = ArithmeticException.class)
        public static final long doLongLargeQuick(final long receiver, final NativeObject arg,
                        @SuppressWarnings("unused") @Bind final SqueakImageContext image) {
            return doLong(receiver, LargeIntegers.longValueExact(arg));
        }

        @Specialization(guards = {"image.isLargeInteger(arg)"}, replaces = "doLongLargeQuick")
        public static final Object doLongLarge(final long receiver, final NativeObject arg,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.and(image, arg, receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 15)
    public abstract static class PrimBitOrNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        public static final long doLong(final long receiver, final long arg) {
            return receiver | arg;
        }

        @Specialization(guards = {"image.isLargeInteger(arg)"}, rewriteOn = ArithmeticException.class)
        public static final long doLongLargeQuick(final long receiver, final NativeObject arg,
                        @SuppressWarnings("unused") @Bind final SqueakImageContext image) {
            return doLong(receiver, LargeIntegers.longValueExact(arg));
        }

        @Specialization(guards = {"image.isLargeInteger(arg)"}, replaces = "doLongLargeQuick")
        public static final Object doLongLarge(final long receiver, final NativeObject arg,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.or(image, receiver, arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 16)
    protected abstract static class PrimBitXorNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final long doLong(final long receiver, final long arg) {
            return receiver ^ arg;
        }

        @Specialization(guards = {"image.isLargeInteger(arg)"}, rewriteOn = ArithmeticException.class)
        protected static final long doLongLargeQuick(final long receiver, final NativeObject arg,
                        @SuppressWarnings("unused") @Bind final SqueakImageContext image) {
            return doLong(receiver, LargeIntegers.longValueExact(arg));
        }

        @Specialization(guards = {"image.isLargeInteger(arg)"}, replaces = "doLongLargeQuick")
        protected static final Object doLongLarge(final long receiver, final NativeObject arg,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.xor(image, receiver, arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 17)
    public abstract static class PrimBitShiftNode extends AbstractArithmeticPrimitiveNode implements Primitive1 {
        @Specialization(guards = {"arg >= 0"})
        public static final Object doLongPositive(final long receiver, final long arg,
                        @Bind final Node node,
                        @Exclusive @Cached final InlinedConditionProfile isOverflowProfile) {
            if (isOverflowProfile.profile(node, SqueakGuards.isLShiftLongOverflow(receiver, arg))) {
                /*
                 * -1 in check needed, because we do not want to shift a positive long into negative
                 * long (most significant bit indicates positive/negative).
                 */
                return LargeIntegers.shiftLeftPositive(getContext(node), receiver, (int) arg);
            } else {
                return receiver << arg;
            }
        }

        @Specialization(guards = {"arg < 0"})
        public static final long doLongNegativeInLongSizeRange(final long receiver, final long arg,
                        @Bind final Node node,
                        @Exclusive @Cached final InlinedConditionProfile inLongSizeRangeProfile) {
            if (inLongSizeRangeProfile.profile(node, inLongSizeRange(arg))) {
                /*
                 * The result of a right shift can only become smaller than the receiver and 0 or -1
                 * at minimum, so no BigInteger needed here.
                 */
                return receiver >> -arg;
            } else {
                return receiver >= 0 ? 0L : -1L;
            }
        }

        protected static final boolean inLongSizeRange(final long arg) {
            return -Long.SIZE < arg;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isLong(receiver)", "!isLong(arg)"})
        protected static final Object doFallback(final Object receiver, final Object arg) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 18)
    public abstract static class PrimMakePointNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        public static final PointersObject doPoint(final Object xPos, final Object yPos,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            return image.asPoint(writeNode, node, xPos, yPos);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 20)
    protected abstract static class PrimRemLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"image.isLargeInteger(lhs)", "rhs != 0"})
        protected static final long doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.remainder(image, lhs, rhs);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)", "!isZero(rhs)"})
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.remainder(image, lhs, rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 21)
    public abstract static class PrimAddLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "image.isLargeInteger(lhs)")
        public static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.add(image, lhs, rhs);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        public static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.add(image, lhs, rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 22)
    public abstract static class PrimSubtractLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "image.isLargeInteger(lhs)")
        public static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.subtract(image, lhs, rhs);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        public static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.subtract(image, lhs, rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 23)
    public abstract static class PrimLessThanLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "image.isLargeInteger(lhs)")
        public static final boolean doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return BooleanObject.wrap(LargeIntegers.compareTo(image, lhs, rhs) < 0);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        public static final boolean doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile sameSignProfile) {
            if (sameSignProfile.profile(node, LargeIntegers.sameSign(lhs, rhs))) {
                return BooleanObject.wrap(LargeIntegers.compareTo(image, lhs, rhs) < 0);
            } else {
                return BooleanObject.wrap(image.isLargeNegativeInteger(lhs));
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 24)
    public abstract static class PrimGreaterThanLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "image.isLargeInteger(lhs)")
        public static final boolean doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return BooleanObject.wrap(LargeIntegers.compareTo(image, lhs, rhs) > 0);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        public static final boolean doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile sameSignProfile) {
            if (sameSignProfile.profile(node, LargeIntegers.sameSign(lhs, rhs))) {
                return BooleanObject.wrap(LargeIntegers.compareTo(image, lhs, rhs) > 0);
            } else {
                return BooleanObject.wrap(image.isLargeNegativeInteger(rhs));
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 25)
    public abstract static class PrimLessOrEqualLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "image.isLargeInteger(lhs)")
        public static final boolean doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return BooleanObject.wrap(LargeIntegers.compareTo(image, lhs, rhs) <= 0);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        public static final boolean doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile sameSignProfile) {
            final boolean lhsNeg = image.isLargeNegativeInteger(lhs);
            if (sameSignProfile.profile(node, LargeIntegers.sameSign(lhs, rhs))) {
                return BooleanObject.wrap(LargeIntegers.compareTo(image, lhs, rhs) <= 0);
            } else {
                return BooleanObject.wrap(lhsNeg);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 26)
    public abstract static class PrimGreaterOrEqualLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "image.isLargeInteger(lhs)")
        public static final boolean doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return BooleanObject.wrap(LargeIntegers.compareTo(image, lhs, rhs) >= 0);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        public static final boolean doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile sameSignProfile) {
            if (sameSignProfile.profile(node, LargeIntegers.sameSign(lhs, rhs))) {
                return BooleanObject.wrap(LargeIntegers.compareTo(image, lhs, rhs) >= 0);
            } else {
                return BooleanObject.wrap(image.isLargeNegativeInteger(rhs));
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 27)
    public abstract static class PrimEqualLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "image.isLargeInteger(lhs)")
        public static final boolean doLargeIntegerLong(final NativeObject lhs, @SuppressWarnings("unused") final long rhs,
                        @SuppressWarnings("unused") @Bind final SqueakImageContext image) {
            assert !LargeIntegers.fitsIntoLong(lhs) : "non-reduced large integer!";
            return BooleanObject.FALSE;
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        public static final boolean doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile sameSignProfile) {
            if (sameSignProfile.profile(node, LargeIntegers.sameSign(lhs, rhs))) {
                return BooleanObject.wrap(LargeIntegers.compareTo(image, lhs, rhs) == 0);
            } else {
                return BooleanObject.FALSE;
            }
        }

        /** Quick return `false` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(getContext(), rhs)", "!isPointersObject(rhs)"})
        protected static final boolean doQuickFalse(final NativeObject lhs, final AbstractSqueakObject rhs) {
            return BooleanObject.FALSE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 28)
    public abstract static class PrimNotEqualLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "image.isLargeInteger(lhs)")
        public static final boolean doLargeIntegerLong(final NativeObject lhs, @SuppressWarnings("unused") final long rhs,
                        @SuppressWarnings("unused") @Bind final SqueakImageContext image) {
            assert !LargeIntegers.fitsIntoLong(lhs) : "non-reduced large integer!";
            return BooleanObject.TRUE;
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        public static final boolean doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @SuppressWarnings("unused") @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile sameSignProfile) {
            return !PrimEqualLargeIntegersNode.doLargeInteger(lhs, rhs, image, node, sameSignProfile);
        }

        /** Quick return `true` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(getContext(), rhs)", "!isPointersObject(rhs)"})
        protected static final boolean doQuickTrue(final Object lhs, final AbstractSqueakObject rhs) {
            return BooleanObject.TRUE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 29)
    public abstract static class PrimMultiplyLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "image.isLargeInteger(lhs)")
        public static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.multiply(image, lhs, rhs);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        public static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.multiply(image, lhs, rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 30)
    public abstract static class PrimDivideLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"image.isLargeInteger(lhs)"})
        public static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Exclusive @Cached final InlinedConditionProfile isZeroProfile,
                        @Exclusive @Cached final InlinedConditionProfile successProfile) {
            if (isZeroProfile.profile(node, rhs == 0)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            }
            final Object[] result = LargeIntegers.divideAndRemainder(image, lhs, rhs);
            if (successProfile.profile(node, result[1] instanceof final Long remainder && remainder == 0)) {
                return result[0];
            } else {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        public static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Exclusive @Cached final InlinedConditionProfile isZeroProfile,
                        @Exclusive @Cached final InlinedConditionProfile successProfile) {
            if (isZeroProfile.profile(node, isZero(rhs))) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            }
            final Object[] result = LargeIntegers.divideAndRemainder(image, lhs, rhs);
            if (successProfile.profile(node, result[1] instanceof final Long remainder && remainder == 0)) {
                return result[0];
            } else {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 31)
    public abstract static class PrimFloorModLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "image.isLargeInteger(lhs)")
        public static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.floorMod(image, lhs, rhs);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        public static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.floorMod(image, lhs, rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 32)
    protected abstract static class PrimFloorDivideLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"image.isLargeInteger(lhs)", "rhs != 0"})
        protected static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.floorDivide(image, lhs, rhs);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)", "!isZero(rhs)"})
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.floorDivide(image, lhs, rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 33)
    protected abstract static class PrimQuoLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"image.isLargeInteger(lhs)", "rhs != 0"})
        protected static final Object doLargeInteger(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.divide(image, lhs, rhs);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)", "!isZero(rhs)"})
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return LargeIntegers.divide(image, lhs, rhs);
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
    protected abstract static class PrimFloatAddNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doAdd(final FloatObject lhs, final Object rhs,
                        @Bind final Node node,
                        @Cached final PrimFloatArgumentToDoubleNode toDoubleNode,
                        @Cached final FloatObjectNormalizeNode normalizeNode) {
            return normalizeNode.execute(node, lhs.getValue() + toDoubleNode.execute(node, rhs));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 42)
    protected abstract static class PrimFloatSubtractNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doSubtract(final FloatObject lhs, final Object rhs,
                        @Bind final Node node,
                        @Cached final PrimFloatArgumentToDoubleNode toDoubleNode,
                        @Cached final FloatObjectNormalizeNode normalizeNode) {
            return normalizeNode.execute(node, lhs.getValue() - toDoubleNode.execute(node, rhs));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 43)
    protected abstract static class PrimFloatLessThanNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doDouble(final FloatObject lhs, final double rhs) {
            return BooleanObject.wrap(lhs.getValue() < rhs);
        }

        @Specialization
        protected static final boolean doFloat(final FloatObject lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        public static final boolean doLong(final FloatObject lhsObject, final long rhs,
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
    protected abstract static class PrimFloatGreaterThanNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
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
    protected abstract static class PrimFloatLessOrEqualNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
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
    protected abstract static class PrimFloatGreaterOrEqualNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
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
    protected abstract static class PrimFloatEqualNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
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
    protected abstract static class PrimFloatNotEqualNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
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
    protected abstract static class PrimFloatMultiplyNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doMultiply(final FloatObject lhs, final Object rhs,
                        @Bind final Node node,
                        @Cached final PrimFloatArgumentToDoubleNode toDoubleNode,
                        @Cached final FloatObjectNormalizeNode normalizeNode) {
            return normalizeNode.execute(node, lhs.getValue() * toDoubleNode.execute(node, rhs));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 50)
    protected abstract static class PrimFloatDivideNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doDivide(final FloatObject lhs, final Object rhs,
                        @Bind final Node node,
                        @Cached final PrimFloatArgumentToDoubleNode toDoubleNode,
                        @Cached final InlinedConditionProfile isZeroProfile,
                        @Cached final FloatObjectNormalizeNode normalizeNode) {
            final double rhsValue = toDoubleNode.execute(node, rhs);
            if (isZeroProfile.profile(node, rhsValue == 0.0)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            } else {
                return normalizeNode.execute(node, lhs.getValue() / rhsValue);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 51)
    protected abstract static class PrimTruncatedNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final long doFloat(final FloatObject receiver,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile inSafeIntegerRangeProfile) {
            return PrimSmallFloatTruncatedNode.doDouble(receiver.getValue(), node, inSafeIntegerRangeProfile);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 52)
    protected abstract static class PrimFloatFractionPartNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final Object doFloat(final FloatObject receiver,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isFiniteProfile,
                        @Cached final InlinedConditionProfile isNanProfile,
                        @Cached final InlinedConditionProfile inSafeIntegerRangeProfile,
                        @Cached final InlinedConditionProfile isNegativeInfinityProfile) {
            if (isFiniteProfile.profile(node, receiver.isFinite())) {
                return PrimSmallFloatFractionPartNode.doDouble(receiver.getValue(), node, inSafeIntegerRangeProfile);
            } else if (isNanProfile.profile(node, receiver.isNaN())) {
                return receiver.shallowCopy();
            } else {
                assert receiver.isInfinite();
                return isNegativeInfinityProfile.profile(node, receiver.getValue() == Double.NEGATIVE_INFINITY) ? -0.0D : 0.0D;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 53)
    protected abstract static class PrimFloatExponentNode extends AbstractFloatArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final long doFloat(final FloatObject receiver,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isZeroProfile,
                        @Cached final InlinedBranchProfile subnormalFloatProfile) {
            if (isZeroProfile.profile(node, receiver.isZero())) {
                return 0L;
            } else {
                return exponentNonZero(receiver.getValue(), subnormalFloatProfile, node);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 54)
    protected abstract static class PrimFloatTimesTwoPowerNode extends AbstractFloatArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(rewriteOn = RespecializeException.class)
        protected static final Object doDoubleFinite(final FloatObject matissa, final long exponent,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile) throws RespecializeException {
            if (isZeroProfile.profile(node, matissa.isZero() || exponent == 0)) {
                return matissa; /* Can be either 0.0 or -0.0. */
            } else {
                return ensureFinite(timesToPower(matissa.getValue(), exponent));
            }
        }

        @Specialization(replaces = "doDoubleFinite")
        protected static final Object doDouble(final FloatObject matissa, final long exponent,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile,
                        @Cached final FloatObjectNormalizeNode normalizeNode) {
            if (isZeroProfile.profile(node, matissa.isZero() || exponent == 0)) {
                return matissa; /* Can be either 0.0 or -0.0. */
            } else {
                return normalizeNode.execute(node, timesToPower(matissa.getValue(), exponent));
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 55)
    protected abstract static class PrimSquareRootNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final Object doFloat(final FloatObject receiver,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isPositiveFiniteProfile,
                        @Cached final InlinedConditionProfile isPositiveInfinityProfile) {
            if (isPositiveFiniteProfile.profile(node, receiver.isPositive() && receiver.isFinite())) {
                return Math.sqrt(receiver.getValue());
            } else if (isPositiveInfinityProfile.profile(node, receiver.isPositiveInfinity())) {
                return receiver.shallowCopy();
            } else {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 56)
    protected abstract static class PrimSinNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final Object doFloat(final FloatObject receiver,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isFiniteProfile) {
            if (isFiniteProfile.profile(node, receiver.isFinite())) {
                return Math.sin(receiver.getValue());
            } else {
                return FloatObject.valueOf(getContext(node), Double.NaN);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 57)
    protected abstract static class PrimArcTanNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final Object doFloat(final FloatObject receiver,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isNaNProfile) {
            if (isNaNProfile.profile(node, receiver.isNaN())) {
                return receiver.shallowCopy();
            } else {
                return Math.atan(receiver.getValue());
            }
        }
    }

    @ImportStatic(Double.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 58)
    protected abstract static class PrimLogNNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final Object doFloat(final FloatObject receiver,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isZeroProfile,
                        @Cached final InlinedConditionProfile isFiniteProfile,
                        @Cached final InlinedConditionProfile isPositiveInfinityProfile,
                        @Cached final InlinedConditionProfile isNegativeInfinityOrNaNProfile) {
            if (isZeroProfile.profile(node, receiver.isZero())) {
                return FloatObject.valueOf(getContext(node), Double.NEGATIVE_INFINITY);
            } else if (isFiniteProfile.profile(node, receiver.isFinite())) {
                return Math.log(receiver.getValue());
            } else if (isPositiveInfinityProfile.profile(node, receiver.isPositiveInfinity())) {
                return receiver.shallowCopy();
            } else if (isNegativeInfinityOrNaNProfile.profile(node, receiver.isNegativeInfinity() || receiver.isNaN())) {
                return FloatObject.valueOf(getContext(node), Double.NaN);
            } else {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 59)
    protected abstract static class PrimExpNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final Object doFloat(final FloatObject receiver,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isFiniteProfile,
                        @Cached final InlinedConditionProfile isNegativeInfinityProfile,
                        @Cached final InlinedConditionProfile isPositiveInfinityOrNaNProfile,
                        @Cached final FloatObjectNormalizeNode normalizeNode) {
            if (isFiniteProfile.profile(node, receiver.isFinite())) {
                return normalizeNode.execute(node, Math.exp(receiver.getValue()));
            } else if (isNegativeInfinityProfile.profile(node, receiver.isNegativeInfinity())) {
                return 0.0D;
            } else if (isPositiveInfinityOrNaNProfile.profile(node, receiver.isPositiveInfinity() || receiver.isNaN())) {
                return receiver.shallowCopy();
            } else {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 159)
    public abstract static class PrimHashMultiplyNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        public static final int HASH_MULTIPLY_CONSTANT = 1664525;
        public static final int HASH_MULTIPLY_MASK = 0xFFFFFFF;

        @Specialization(guards = "image.isLargeInteger(receiver)")
        protected static final long doLargeInteger(final NativeObject receiver,
                        @SuppressWarnings("unused") @Bind final SqueakImageContext image) {
            return doLong(LargeIntegers.longValue(receiver));
        }

        @Specialization
        protected static final long doLong(final long receiver) {
            return receiver * HASH_MULTIPLY_CONSTANT & HASH_MULTIPLY_MASK;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 541)
    public abstract static class PrimSmallFloatAddNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        public static final double doDouble(final double lhs, final double rhs) {
            return lhs + rhs;
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        public static final double doLong(final double lhs, final long rhs) {
            return doDouble(lhs, rhs);
        }

        @Specialization
        protected static final Object doFloat(final double lhs, final FloatObject rhs,
                        @Bind final Node node,
                        @Cached final FloatObjectNormalizeNode normalizeNode) {
            return normalizeNode.execute(node, lhs + rhs.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 542)
    public abstract static class PrimSmallFloatSubtractNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        public static final double doDouble(final double lhs, final double rhs) {
            return lhs - rhs;
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        public static final double doLong(final double lhs, final long rhs) {
            return doDouble(lhs, rhs);
        }

        @Specialization
        protected static final Object doFloat(final double lhs, final FloatObject rhs,
                        @Bind final Node node,
                        @Shared("normalizeNode") @Cached final FloatObjectNormalizeNode normalizeNode) {
            return normalizeNode.execute(node, lhs - rhs.getValue());
        }

        @Specialization(guards = "isFraction(rhs, node)")
        protected static final Object doFraction(final double lhs, final PointersObject rhs,
                        @Bind final Node node,
                        @Shared("normalizeNode") @Cached final FloatObjectNormalizeNode normalizeNode,
                        @Cached final AbstractPointersObjectNodes.AbstractPointersObjectReadNode readNode) {
            return normalizeNode.execute(node, lhs - SqueakImageContext.fromFraction(rhs, readNode, node));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 543)
    public abstract static class PrimSmallFloatLessThanNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        public static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs < rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        public static final boolean doLong(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return BooleanObject.wrap((long) lhs < rhs);
            } else {
                return doDouble(lhs, rhs);
            }
        }

        @Specialization
        protected static final boolean doFloat(final double lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 544)
    public abstract static class PrimSmallFloatGreaterThanNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        public static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs > rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        public static final boolean doLong(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return BooleanObject.wrap((long) lhs > rhs);
            } else {
                return doDouble(lhs, rhs);
            }
        }

        @Specialization
        protected static final boolean doFloat(final double lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 545)
    public abstract static class PrimSmallFloatLessOrEqualNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        public static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs <= rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        public static final boolean doLong(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return BooleanObject.wrap((long) lhs <= rhs);
            } else {
                return doDouble(lhs, rhs);
            }
        }

        @Specialization
        protected static final boolean doFloat(final double lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 546)
    public abstract static class PrimSmallFloatGreaterOrEqualNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        public static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs >= rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        public static final boolean doLong(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return BooleanObject.wrap((long) lhs >= rhs);
            } else {
                return doDouble(lhs, rhs);
            }
        }

        @Specialization
        protected static final boolean doFloat(final double lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 547)
    public abstract static class PrimSmallFloatEqualNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        public static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs == rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        public static final boolean doLong(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return BooleanObject.wrap((long) lhs == rhs);
            } else {
                return BooleanObject.FALSE;
            }
        }

        @Specialization
        protected static final boolean doFloat(final double lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 548)
    public abstract static class PrimSmallFloatNotEqualNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        public static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs != rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        public static final boolean doLong(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return BooleanObject.wrap((long) lhs != rhs);
            } else {
                return BooleanObject.TRUE;
            }
        }

        @Specialization
        protected static final boolean doFloat(final double lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 549)
    public abstract static class PrimSmallFloatMultiplyNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(rewriteOn = RespecializeException.class)
        public static final double doDoubleFinite(final double lhs, final double rhs) throws RespecializeException {
            return ensureFinite(lhs * rhs);
        }

        @Specialization(replaces = "doDoubleFinite")
        public static final Object doDouble(final double lhs, final double rhs,
                        @Bind final Node node,
                        @Shared("normalizeNode") @Cached final FloatObjectNormalizeNode normalizeNode) {
            return normalizeNode.execute(node, lhs * rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()", rewriteOn = RespecializeException.class)
        public static final double doLongFinite(final double lhs, final long rhs) throws RespecializeException {
            return doDoubleFinite(lhs, rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()", replaces = "doLongFinite")
        public static final Object doLong(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("normalizeNode") @Cached final FloatObjectNormalizeNode normalizeNode) {
            return doDouble(lhs, rhs, node, normalizeNode);
        }

        @Specialization
        protected static final Object doFloat(final double lhs, final FloatObject rhs,
                        @Bind final Node node,
                        @Shared("normalizeNode") @Cached final FloatObjectNormalizeNode normalizeNode) {
            return doDouble(lhs, rhs.getValue(), node, normalizeNode);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 550)
    public abstract static class PrimSmallFloatDivideNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(rewriteOn = RespecializeException.class)
        public static final double doDoubleFinite(final double lhs, final double rhs,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile) throws RespecializeException {
            if (isZeroProfile.profile(node, SqueakGuards.isZero(rhs))) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            } else {
                return ensureFinite(lhs / rhs);
            }
        }

        @Specialization(replaces = "doDoubleFinite")
        public static final Object doDouble(final double lhs, final double rhs,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile,
                        @Shared("normalizeNode") @Cached final FloatObjectNormalizeNode normalizeNode) {
            if (isZeroProfile.profile(node, SqueakGuards.isZero(rhs))) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            } else {
                return normalizeNode.execute(node, lhs / rhs);
            }
        }

        @Specialization(guards = {"isPrimitiveDoMixedArithmetic()"}, rewriteOn = RespecializeException.class)
        public static final double doDoubleLongFinite(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile) throws RespecializeException {
            return doDoubleFinite(lhs, rhs, node, isZeroProfile);
        }

        @Specialization(guards = {"isPrimitiveDoMixedArithmetic()"}, replaces = "doDoubleLongFinite")
        public static final Object doDoubleLong(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile,
                        @Shared("normalizeNode") @Cached final FloatObjectNormalizeNode normalizeNode) {
            return doDouble(lhs, rhs, node, isZeroProfile, normalizeNode);
        }

        @Specialization
        protected static final Object doFloat(final double lhs, final FloatObject rhs,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile,
                        @Shared("normalizeNode") @Cached final FloatObjectNormalizeNode normalizeNode) {
            return doDouble(lhs, rhs.getValue(), node, isZeroProfile, normalizeNode);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 551)
    protected abstract static class PrimSmallFloatTruncatedNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final long doDouble(final double receiver,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile inSafeIntegerRangeProfile) {
            if (inSafeIntegerRangeProfile.profile(node, inSafeIntegerRange(receiver))) {
                return (long) ExactMath.truncate(receiver);
            } else {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 552)
    protected abstract static class PrimSmallFloatFractionPartNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final double doDouble(final double receiver,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile inSafeIntegerRangeProfile) {
            if (inSafeIntegerRangeProfile.profile(node, inSafeIntegerRange(receiver))) {
                return receiver - (long) receiver;
            } else {
                return LargeIntegers.fractionPart(receiver);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 553)
    protected abstract static class PrimSmallFloatExponentNode extends AbstractFloatArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final long doDouble(final double receiver,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isZeroProfile,
                        @Cached final InlinedBranchProfile subnormalFloatProfile) {
            if (isZeroProfile.profile(node, SqueakGuards.isZero(receiver))) {
                return 0L;
            } else {
                return exponentNonZero(receiver, subnormalFloatProfile, node);
            }
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
                        @Cached final FloatObjectNormalizeNode normalizeNode) {
            return normalizeNode.execute(node, timesToPower(matissa, exponent));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 555)
    protected abstract static class PrimSmallFloatSquareRootNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"isZeroOrGreater(receiver)"})
        protected static final double doDouble(final double receiver) {
            assert Double.isFinite(receiver);
            return Math.sqrt(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 556)
    protected abstract static class PrimSmallFloatSineNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final double doDouble(final double receiver) {
            assert Double.isFinite(receiver);
            return Math.sin(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 557)
    protected abstract static class PrimSmallFloatArctanNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final double doDouble(final double receiver) {
            assert Double.isFinite(receiver);
            return Math.atan(receiver);
        }
    }

    @ImportStatic(Double.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 558)
    protected abstract static class PrimSmallFloatLogNNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = "isGreaterThanZero(receiver)")
        protected static final double doDouble(final double receiver) {
            assert Double.isFinite(receiver);
            return Math.log(receiver);
        }

        @Specialization(guards = "isZero(receiver)")
        protected final FloatObject doFloatZero(@SuppressWarnings("unused") final double receiver) {
            return FloatObject.valueOf(getContext(), Double.NEGATIVE_INFINITY);
        }

        @Specialization(guards = "isLessThanZero(receiver)")
        protected final FloatObject doDoubleNegative(@SuppressWarnings("unused") final double receiver) {
            return FloatObject.valueOf(getContext(), Double.NaN);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 559)
    protected abstract static class PrimSmallFloatExpNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization(rewriteOn = RespecializeException.class)
        protected static final double doDoubleFinite(final double receiver) throws RespecializeException {
            assert Double.isFinite(receiver);
            return ensureFinite(Math.exp(receiver));
        }

        @Specialization(replaces = "doDoubleFinite")
        protected static final Object doDouble(final double receiver,
                        @Bind final Node node,
                        @Cached final FloatObjectNormalizeNode normalizeNode) {
            assert Double.isFinite(receiver);
            return normalizeNode.execute(node, Math.exp(receiver));
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
        private static final long MAX_SAFE_INTEGER_LONG = 1L << FloatObject.PRECISION;
        private static final long MIN_SAFE_INTEGER_LONG = -MAX_SAFE_INTEGER_LONG;

        public static final double ensureFinite(final double value) throws RespecializeException {
            if (Double.isFinite(value)) {
                return value;
            } else {
                throw RespecializeException.transferToInterpreterInvalidateAndThrow();
            }
        }

        protected static final boolean inSafeIntegerRange(final double d) {
            // The ends of the interval are also included, since they are powers of two
            return MIN_SAFE_INTEGER_LONG <= d && d <= MAX_SAFE_INTEGER_LONG;
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
