/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.RespecializeException;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.AbstractArithmeticPrimitiveNode;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.ReflectionUtils;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;
import de.hpi.swa.trufflesqueak.util.VarHandleUtils;

public final class LargeIntegers extends AbstractPrimitiveFactoryHolder {
    private static final Field BIG_INTEGER_MAG_FIELD = ReflectionUtils.lookupField(BigInteger.class, "mag");
    private static final Constructor<?> BIG_INTEGER_INT_ARRAY_INT_CTOR = ReflectionUtils.lookupConstructor(BigInteger.class, int[].class, int.class);

    private static final BigInteger ONE_SHIFTED_BY_64 = BigInteger.ONE.shiftLeft(64);
    @CompilationFinal(dimensions = 1) public static final byte[] LONG_MIN_OVERFLOW_RESULT_BYTES = toByteArray(BigInteger.valueOf(Long.MIN_VALUE).abs());

    private static final ArithmeticException LARGE_INTEGER_OUT_OF_INT_RANGE = new ArithmeticException("Large integer out of int range");
    private static final ArithmeticException LARGE_INTEGER_OUT_OF_LONG_RANGE = new ArithmeticException("Large integer out of long range");
    private static final String MODULE_NAME = "LargeIntegers v2.0 (TruffleSqueak)";

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primAnyBitFromTo")
    protected abstract static class PrimAnyBitFromToNode extends AbstractArithmeticPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"start >= 1", "stopArg >= 1"})
        protected static final boolean doLong(final long receiver, final long start, final long stopArg,
                        @Bind final Node node,
                        @Shared("startLargerThanStopProfile") @Cached final InlinedBranchProfile startLargerThanStopProfile,
                        @Shared("firstAndLastDigitIndexIdenticalProfile") @Cached final InlinedConditionProfile firstAndLastDigitIndexIdenticalProfile,
                        @Shared("firstDigitNonZeroProfile") @Cached final InlinedBranchProfile firstDigitNonZeroProfile,
                        @Shared("middleDigitsNonZeroProfile") @Cached final InlinedBranchProfile middleDigitsNonZeroProfile,
                        @Shared("lastDigitProfile") @Cached final InlinedBranchProfile lastDigitProfile) {
            final long stop = Math.min(stopArg, Long.highestOneBit(receiver));
            if (start > stop) {
                startLargerThanStopProfile.enter(node);
                return BooleanObject.FALSE;
            }
            final int firstDigitIndex = ((int) start - 1) / 8 + 1;
            final int lastDigitIndex = ((int) stop - 1) / 8 + 1;
            final int rightShift = -(((int) start - 1) % 8);
            final int leftShift = 7 - ((int) stop - 1) % 8;
            if (firstAndLastDigitIndexIdenticalProfile.profile(node, firstDigitIndex == lastDigitIndex)) {
                final int mask = 0xFF >> rightShift & 0xFF >> leftShift;
                final byte digit = digitOf(receiver, firstDigitIndex - 1);
                return BooleanObject.wrap((digit & mask) != 0);
            } else {
                if (digitOf(receiver, firstDigitIndex - 1) << rightShift != 0) {
                    firstDigitNonZeroProfile.enter(node);
                    return BooleanObject.TRUE;
                }
                for (long i = firstDigitIndex + 1; i < lastDigitIndex; i++) {
                    if (digitOf(receiver, i - 1) != 0) {
                        middleDigitsNonZeroProfile.enter(node);
                        return BooleanObject.TRUE;
                    }
                }
                lastDigitProfile.enter(node);
                return BooleanObject.wrap((digitOf(receiver, lastDigitIndex - 1) << leftShift & 0xFF) != 0);
            }
        }

        @Specialization(guards = {"image.isLargeInteger(receiver)", "start >= 1", "stopArg >= 1"}, rewriteOn = {ArithmeticException.class})
        protected static final boolean doLargeIntegerAsLong(final NativeObject receiver, final long start, final long stopArg,
                        @SuppressWarnings("unused") @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Shared("startLargerThanStopProfile") @Cached final InlinedBranchProfile startLargerThanStopProfile,
                        @Shared("firstAndLastDigitIndexIdenticalProfile") @Cached final InlinedConditionProfile firstAndLastDigitIndexIdenticalProfile,
                        @Shared("firstDigitNonZeroProfile") @Cached final InlinedBranchProfile firstDigitNonZeroProfile,
                        @Shared("middleDigitsNonZeroProfile") @Cached final InlinedBranchProfile middleDigitsNonZeroProfile,
                        @Shared("lastDigitProfile") @Cached final InlinedBranchProfile lastDigitProfile) {
            return doLong(longValueExact(receiver), start, stopArg, node, startLargerThanStopProfile, firstAndLastDigitIndexIdenticalProfile, firstDigitNonZeroProfile, middleDigitsNonZeroProfile,
                            lastDigitProfile);
        }

        @Specialization(guards = {"image.isLargeInteger(receiver)", "start >= 1", "stopArg >= 1"})
        protected static final boolean doLargeInteger(final NativeObject receiver, final long start, final long stopArg,
                        @SuppressWarnings("unused") @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Shared("startLargerThanStopProfile") @Cached final InlinedBranchProfile startLargerThanStopProfile,
                        @Shared("firstAndLastDigitIndexIdenticalProfile") @Cached final InlinedConditionProfile firstAndLastDigitIndexIdenticalProfile,
                        @Shared("firstDigitNonZeroProfile") @Cached final InlinedBranchProfile firstDigitNonZeroProfile,
                        @Shared("middleDigitsNonZeroProfile") @Cached final InlinedBranchProfile middleDigitsNonZeroProfile,
                        @Shared("lastDigitProfile") @Cached final InlinedBranchProfile lastDigitProfile) {
            final long stop = Math.min(stopArg, highBitOfLargeInt(receiver));
            if (start > stop) {
                startLargerThanStopProfile.enter(node);
                return BooleanObject.FALSE;
            }
            final byte[] bytes = receiver.getByteStorage();
            final int firstDigitIndex = ((int) start - 1) / 8 + 1;
            final int lastDigitIndex = ((int) stop - 1) / 8 + 1;
            final int rightShift = -(((int) start - 1) % 8);
            final int leftShift = 7 - ((int) stop - 1) % 8;
            if (firstAndLastDigitIndexIdenticalProfile.profile(node, firstDigitIndex == lastDigitIndex)) {
                final int mask = 0xFF >> rightShift & 0xFF >> leftShift;
                final byte digit = bytes[firstDigitIndex - 1];
                return BooleanObject.wrap((digit & mask) != 0);
            } else {
                if (bytes[firstDigitIndex - 1] << rightShift != 0) {
                    firstDigitNonZeroProfile.enter(node);
                    return BooleanObject.TRUE;
                }
                for (int i = firstDigitIndex + 1; i < lastDigitIndex; i++) {
                    if (bytes[i - 1] != 0) {
                        middleDigitsNonZeroProfile.enter(node);
                        return BooleanObject.TRUE;
                    }
                }
                lastDigitProfile.enter(node);
                return BooleanObject.wrap((bytes[lastDigitIndex - 1] << leftShift & 0xFF) != 0);
            }
        }

        private static byte digitOf(final long value, final long index) {
            return (byte) (value >> Byte.SIZE * index);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitAdd")
    protected abstract static class PrimDigitAddNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile sameSignProfile) {
            return Math.addExact(lhs, rhsNegatedOnDifferentSign(lhs, rhs, sameSignProfile, node));
        }

        @Specialization(replaces = "doLong")
        protected static final Object doLongWithOverflow(final long lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return add(image, lhs, rhs);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return add(image, lhs, rhs);
        }

        @Specialization(guards = "image.isLargeInteger(rhs)")
        protected static final Object doLongLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return add(image, rhs, lhs);
        }

        @Specialization(guards = "image.isLargeInteger(lhs)")
        protected static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return add(image, lhs, rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitSubtract")
    protected abstract static class PrimDigitSubtractNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("sameSignProfile") @Cached final InlinedConditionProfile sameSignProfile) {
            return Math.subtractExact(lhs, rhsNegatedOnDifferentSign(lhs, rhs, sameSignProfile, node));
        }

        @Specialization(replaces = "doLong")
        protected static final Object doLongWithOverflow(final long lhs, final long rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Shared("sameSignProfile") @Cached final InlinedConditionProfile sameSignProfile) {
            return subtract(image, lhs, rhsNegatedOnDifferentSign(lhs, rhs, sameSignProfile, node));
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return subtract(image, lhs, rhs);
        }

        @Specialization(guards = "image.isLargeInteger(rhs)")
        protected static final Object doLongLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return subtract(image, lhs, rhs);
        }

        @Specialization(guards = "image.isLargeInteger(lhs)")
        protected static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return subtract(image, lhs, rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitMultiplyNegative")
    protected abstract static class PrimDigitMultiplyNegativeNode extends AbstractArithmeticPrimitiveNode implements Primitive2WithFallback {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs, @SuppressWarnings("unused") final boolean neg) {
            return Math.multiplyExact(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        protected static final Object doLongWithOverflow(final long lhs, final long rhs, @SuppressWarnings("unused") final boolean neg,
                        @Bind final SqueakImageContext image) {
            return multiply(image, lhs, rhs);
        }

        @Specialization(guards = "image.isLargeInteger(rhs)")
        protected static final Object doLongLargeInteger(final long lhs, final NativeObject rhs, @SuppressWarnings("unused") final boolean neg,
                        @Bind final SqueakImageContext image) {
            return multiply(image, rhs, lhs);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs, @SuppressWarnings("unused") final boolean neg,
                        @Bind final SqueakImageContext image) {
            return multiply(image, lhs, rhs);
        }

        @Specialization(guards = "image.isLargeInteger(lhs)")
        protected static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs, @SuppressWarnings("unused") final boolean neg,
                        @Bind final SqueakImageContext image) {
            return multiply(image, lhs, rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitBitAnd")
    public abstract static class PrimDigitBitAndNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        public static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return and(image, lhs, rhs);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)"})
        public static final Object doLargeInteger(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return and(image, lhs, rhs);
        }

        @Specialization(guards = {"image.isLargeInteger(rhs)"})
        public static final Object doLong(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return and(image, rhs, lhs);
        }

        @Specialization(guards = {"lhs >= 0", "rhs >= 0"})
        protected static final long doLong(final long lhs, final long rhs) {
            return lhs & rhs;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitBitOr")
    public abstract static class PrimDigitBitOrNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        public static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return or(image, lhs, rhs);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)"})
        public static final Object doLargeInteger(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return or(image, rhs, lhs);
        }

        @Specialization(guards = {"image.isLargeInteger(rhs)"})
        public static final Object doLong(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return or(image, lhs, rhs);
        }

        @Specialization(guards = {"lhs >= 0", "rhs >= 0"})
        protected static final long doLong(final long lhs, final long rhs) {
            return lhs | rhs;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitBitShiftMagnitude")
    public abstract static class PrimDigitBitShiftMagnitudeNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "image.isLargeInteger(lhs)")
        protected static final Object doLargeInteger(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return shiftLeft(image, lhs, MiscUtils.toIntExact(rhs));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitBitXor")
    protected abstract static class PrimBitXorNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return xor(image, lhs, rhs);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)"})
        protected static final Object doLargeInteger(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return xor(image, rhs, lhs);
        }

        @Specialization(guards = {"lhs >= 0", "image.isLargeInteger(rhs)"})
        protected static final Object doLong(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return xor(image, lhs, rhs);
        }

        @Specialization(guards = {"lhs >= 0", "rhs >= 0"})
        protected static final long doLong(final long lhs, final long rhs) {
            return lhs ^ rhs;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitCompare")
    protected abstract static class PrimDigitCompareNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final long doLong(final long lhs, final long rhs,
                        @Bind final Node node,
                        @Exclusive @Cached final InlinedConditionProfile smallerProfile,
                        @Exclusive @Cached final InlinedConditionProfile equalProfile) {
            if (smallerProfile.profile(node, lhs < rhs)) {
                return -1L;
            } else if (equalProfile.profile(node, lhs == rhs)) {
                return 0L;
            } else {
                return +1L;
            }
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        protected static final long doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return compareTo(image, lhs, rhs);
        }

        @Specialization(guards = "image.isLargeInteger(rhs)")
        protected static final long doLongLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Shared("fitsIntoLongProfile") @Cached final InlinedConditionProfile fitsIntoLongProfile) {
            if (fitsIntoLongProfile.profile(node, fitsIntoLong(rhs))) {
                final long value = longValue(rhs);
                return value == lhs ? 0L : value < lhs ? -1L : 1L;
            } else {
                return isNegative(image, rhs) ? 1L : -1L;
            }
        }

        @Specialization(guards = "image.isLargeInteger(lhs)")
        protected static final long doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Shared("fitsIntoLongProfile") @Cached final InlinedConditionProfile fitsIntoLongProfile) {
            if (fitsIntoLongProfile.profile(node, fitsIntoLong(lhs))) {
                final long value = longValue(lhs);
                return value == rhs ? 0L : value < rhs ? -1L : 1L;
            } else {
                return isNegative(image, lhs) ? -1L : 1L;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitDivNegative")
    protected abstract static class PrimDigitDivNegativeNode extends AbstractArithmeticPrimitiveNode implements Primitive2WithFallback {
        @TruffleBoundary
        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        protected static final ArrayObject doLargeInteger(final NativeObject lhs, final NativeObject rhs, final boolean negative,
                        @Bind final SqueakImageContext image) {
            final BigInteger[] divide = toBigInteger(image, lhs).divideAndRemainder(toBigInteger(image, rhs));
            final Object[] result = digitDivNegative(negative, image, divide);
            if (divide[1].bitLength() < Long.SIZE) {
                result[1] = divide[1].longValue();
            } else {
                result[1] = toNativeObject(image, divide[1]);
            }
            return image.asArrayOfObjects(result);
        }

        @TruffleBoundary
        @Specialization(guards = "image.isLargeInteger(lhs)")
        protected static final ArrayObject doLargeIntegerLong(final NativeObject lhs, final long rhs, final boolean negative,
                        @Bind final SqueakImageContext image) {
            final BigInteger[] divide = toBigInteger(image, lhs).divideAndRemainder(BigInteger.valueOf(rhs));
            final Object[] result = digitDivNegative(negative, image, divide);
            result[1] = divide[1].longValue();
            return image.asArrayOfObjects(result);
        }

        @Specialization(guards = "image.isLargeInteger(rhs)")
        protected static final ArrayObject doLongLargeInteger(final long lhs, final NativeObject rhs, @SuppressWarnings("unused") final boolean negative,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Shared("signProfile") @Cached final InlinedBranchProfile signProfile) {
            if (LargeIntegers.fitsIntoLong(rhs)) {
                return doLong(lhs, LargeIntegers.longValue(rhs), negative, image, node, signProfile);
            } else {
                return image.asArrayOfLongs(0L, lhs);
            }
        }

        @Specialization
        protected static final ArrayObject doLong(final long lhs, final long rhs, final boolean negative,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Shared("signProfile") @Cached final InlinedBranchProfile signProfile) {
            long divide = lhs / rhs;
            if (negative && divide >= 0 || !negative && divide < 0) {
                signProfile.enter(node);
                if (divide == Long.MIN_VALUE) {
                    return image.asArrayOfObjects(createLongMinOverflowResult(image), lhs % rhs);
                }
                divide = -divide;
            }
            return image.asArrayOfLongs(divide, lhs % rhs);
        }

        private static Object[] digitDivNegative(final boolean negative, final SqueakImageContext image, final BigInteger[] divide) {
            final Object[] result = new Object[2];
            if (negative != divide[0].signum() < 0) {
                if (divide[0].bitLength() < Long.SIZE) {
                    final long longValue = divide[0].longValue();
                    if (longValue == Long.MIN_VALUE) {
                        result[0] = createLongMinOverflowResult(image);
                    } else {
                        result[0] = -longValue;
                    }
                } else {
                    result[0] = toNativeObject(image, divide[0].negate());
                }
            } else {
                if (divide[0].bitLength() < Long.SIZE) {
                    result[0] = divide[0].longValue();
                } else {
                    result[0] = toNativeObject(image, divide[0]);
                }
            }
            return result;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primGetModuleName")
    protected abstract static class PrimGetModuleNameNode extends AbstractArithmeticPrimitiveNode implements Primitive0 {
        @Specialization
        protected final Object doGet(@SuppressWarnings("unused") final Object receiver) {
            return getContext().asByteString(MODULE_NAME);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primMontgomeryDigitLength")
    protected abstract static class PrimMontgomeryDigitLengthNode extends AbstractArithmeticPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final long doDigitLength(@SuppressWarnings("unused") final Object receiver) {
            return 32L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primMontgomeryTimesModulo")
    protected abstract static class PrimMontgomeryTimesModuloNode extends AbstractArithmeticPrimitiveNode implements Primitive3 {
        private static final long LONG_MASK = 0xFFFFFFFFL;

        /*
         * Optimized version of montgomeryTimesModulo for integer-sized arguments.
         */
        @Specialization(rewriteOn = {RespecializeException.class})
        protected static final long doLongQuick(final long receiver, final long a, final long m, final long mInv) throws RespecializeException {
            if (!(fitsInOneWord(receiver) && fitsInOneWord(a) && fitsInOneWord(m))) {
                throw RespecializeException.transferToInterpreterInvalidateAndThrow();
            }
            final long accum3 = receiver * a;
            final long u = accum3 * mInv & LONG_MASK;
            final long accum2 = u * m;
            long accum = (accum2 & LONG_MASK) + (accum3 & LONG_MASK);
            accum = (accum >> 32) + (accum2 >> 32) + (accum3 >> 32);
            long result = accum & LONG_MASK;
            if (!(accum >> 32 == 0 && result < m)) {
                result = result - m & LONG_MASK;
            }
            return result;
        }

        @Specialization(replaces = "doLongQuick")
        protected static final Object doGeneric(final Object receiver, final Object a, final Object m, final Object mInv,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final ToIntsNode receiverToIntsNode,
                        @Cached final ToIntsNode aToIntsNode,
                        @Cached final ToIntsNode mToIntsNode,
                        @Cached final ToExactLongNode toExactLongNode) {
            return doLargeInteger(image, receiverToIntsNode.execute(node, receiver), aToIntsNode.execute(node, a), mToIntsNode.execute(node, m), toExactLongNode.execute(node, mInv));
        }

        private static boolean fitsInOneWord(final long value) {
            return value <= NativeObject.INTEGER_MAX;
        }

        private static Object doLargeInteger(final SqueakImageContext image, final int[] firstInts, final int[] secondInts, final int[] thirdInts, final long mInv) {
            final int firstLen = firstInts.length;
            final int secondLen = secondInts.length;
            final int thirdLen = thirdInts.length;
            if (thirdLen == 0 || firstLen > thirdLen || secondLen > thirdLen) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            final int limit1 = firstLen - 1;
            final int limit2 = secondLen - 1;
            final int limit3 = thirdLen - 1;
            final int[] result = new int[thirdLen];

            long accum;
            long accum2;
            int lastDigit = 0;
            for (int i = 0; i <= limit1; i++) {
                long accum3 = firstInts[i] & LONG_MASK;
                accum3 = accum3 * (secondInts[0] & LONG_MASK) + (result[0] & LONG_MASK);
                final long u = accum3 * mInv & LONG_MASK;
                accum2 = u * (thirdInts[0] & LONG_MASK);
                accum = (accum2 & LONG_MASK) + (accum3 & LONG_MASK);
                accum = (accum >> 32) + (accum2 >> 32) + (accum3 >> 32 & LONG_MASK);
                for (int k = 1; k <= limit2; k++) {
                    accum3 = firstInts[i] & LONG_MASK;
                    accum3 = accum3 * (secondInts[k] & LONG_MASK) + (result[k] & LONG_MASK);
                    accum2 = u * (thirdInts[k] & LONG_MASK);
                    accum = accum + (accum2 & LONG_MASK) + (accum3 & LONG_MASK);
                    result[k - 1] = (int) (accum & LONG_MASK);
                    accum = (accum >> 32) + (accum2 >> 32) + (accum3 >> 32 & LONG_MASK);
                }
                for (int k = secondLen; k <= limit3; k++) {
                    accum2 = u * (thirdInts[k] & LONG_MASK);
                    accum = accum + (result[k] & LONG_MASK) + (accum2 & LONG_MASK);
                    result[k - 1] = (int) (accum & LONG_MASK);
                    accum = (accum >> 32) + (accum2 >> 32) & LONG_MASK;
                }
                accum += lastDigit;
                result[limit3] = (int) (accum & LONG_MASK);
                lastDigit = (int) (accum >> 32);
            }
            for (int i = firstLen; i <= limit3; i++) {
                accum = result[0] & LONG_MASK;
                final long u = accum * mInv & LONG_MASK;
                accum += u * (thirdInts[0] & LONG_MASK);
                accum = accum >> 32;
                for (int k = 1; k <= limit3; k++) {
                    accum2 = u * (thirdInts[k] & LONG_MASK);
                    accum = accum + (result[k] & LONG_MASK) + (accum2 & LONG_MASK);
                    result[k - 1] = (int) (accum & LONG_MASK);
                    accum = (accum >> 32) + (accum2 >> 32) & LONG_MASK;
                }
                accum += lastDigit;
                result[limit3] = (int) (accum & LONG_MASK);
                lastDigit = (int) (accum >> 32);
            }
            if (!(lastDigit == 0 && cDigitComparewithlen(thirdInts, result, thirdLen) == 1)) {
                accum = 0;
                for (int i = 0; i <= limit3; i++) {
                    accum = accum + result[i] - (thirdInts[i] & LONG_MASK);
                    result[i] = (int) (accum & LONG_MASK);
                    accum = -(accum >> 63);
                }
            }
            return normalize(image, UnsafeUtils.toBytes(result), false);
        }

        private static int cDigitComparewithlen(final int[] first, final int[] second, final int len) {
            int firstDigit;
            int secondDigit;
            int index = len - 1;
            while (index >= 0) {
                if ((secondDigit = second[index]) != (firstDigit = first[index])) {
                    if (secondDigit < firstDigit) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
                --index;
            }
            return 0;
        }

        @GenerateInline
        @GenerateCached(false)
        protected abstract static class ToIntsNode extends AbstractNode {
            protected abstract int[] execute(Node inliningTarget, Object value);

            @Specialization(guards = "image.isLargeInteger(value)")
            protected static final int[] doLargeInteger(final NativeObject value,
                            @SuppressWarnings("unused") @Bind final SqueakImageContext image) {
                return UnsafeUtils.toIntsExact(value.getByteStorage());
            }

            @Specialization
            protected static final int[] doLong(final long value) {
                if (fitsInOneWord(value)) {
                    return new int[]{(int) value};
                } else {
                    return new int[]{(int) value, (int) (value >> 32)};
                }
            }

            @Fallback
            protected static final int[] doFallback(@SuppressWarnings("unused") final Object value) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @GenerateInline
        @GenerateCached(false)
        protected abstract static class ToExactLongNode extends AbstractNode {
            protected abstract long execute(Node inliningTarget, Object value);

            @Specialization
            protected static final long doLong(final long value) {
                return value;
            }

            @Specialization(guards = "image.isLargeInteger(value)")
            protected static final long doLargeInteger(final NativeObject value,
                            @SuppressWarnings("unused") @Bind final SqueakImageContext image) {
                return longValueExact(value);
            }

            @Fallback
            protected static final long doFallback(@SuppressWarnings("unused") final Object value) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primNormalizePositive")
    protected abstract static class PrimNormalizePositiveNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"image.isLargePositiveInteger(receiver)"})
        protected static final Object doNativeObject(final NativeObject receiver,
                        @Bind final SqueakImageContext image) {
            return normalize(image, receiver.getByteStorage(), false);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primNormalizeNegative")
    protected abstract static class PrimNormalizeNegativeNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"image.isLargeNegativeInteger(receiver)"})
        protected static final Object doNativeObject(final NativeObject receiver,
                        @Bind final SqueakImageContext image) {
            return normalize(image, receiver.getByteStorage(), true);
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return LargeIntegersFactory.getFactories();
    }

    /*
     * Arithmetic Operations
     */

    public static Object add(final SqueakImageContext image, final long lhs, final long rhs) {
        /* Inlined version of Math.addExact(x, y) with large integer fallback. */
        final long result = lhs + rhs;
        // HD 2-12 Overflow iff both arguments have the opposite sign of the result
        if (((lhs ^ result) & (rhs ^ result)) < 0) {
            return addLarge(image, lhs, rhs);
        }
        return result;
    }

    @TruffleBoundary
    public static Object addLarge(final SqueakImageContext image, final long lhs, final long rhs) {
        return add(image, BigInteger.valueOf(lhs), BigInteger.valueOf(rhs));
    }

    @TruffleBoundary
    public static Object add(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return add(image, toBigInteger(image, lhs), toBigInteger(image, rhs));
    }

    @TruffleBoundary
    public static Object add(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return add(image, toBigInteger(image, lhs), BigInteger.valueOf(rhs));
    }

    private static Object add(final SqueakImageContext image, final BigInteger lhs, final BigInteger rhs) {
        return normalize(image, lhs.add(rhs));
    }

    public static Object subtract(final SqueakImageContext image, final long lhs, final long rhs) {
        /* Inlined version of Math.subtractExact(x, y) with large integer fallback. */
        final long result = lhs - rhs;
        // HD 2-12 Overflow iff the arguments have different signs and
        // the sign of the result is different than the sign of x
        if (((lhs ^ rhs) & (lhs ^ result)) < 0) {
            return subtractLarge(image, lhs, rhs);
        }
        return result;
    }

    @TruffleBoundary
    public static Object subtractLarge(final SqueakImageContext image, final long lhs, final long rhs) {
        return subtract(image, BigInteger.valueOf(lhs), BigInteger.valueOf(rhs));
    }

    @TruffleBoundary
    public static Object subtract(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return subtract(image, toBigInteger(image, lhs), toBigInteger(image, rhs));
    }

    @TruffleBoundary
    public static Object subtract(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return subtract(image, toBigInteger(image, lhs), BigInteger.valueOf(rhs));
    }

    @TruffleBoundary
    public static Object subtract(final SqueakImageContext image, final long lhs, final NativeObject rhs) {
        return subtract(image, BigInteger.valueOf(lhs), toBigInteger(image, rhs));
    }

    private static Object subtract(final SqueakImageContext image, final BigInteger lhs, final BigInteger rhs) {
        return normalize(image, lhs.subtract(rhs));
    }

    public static Object multiply(final SqueakImageContext image, final long lhs, final long rhs) {
        /* Inlined version of Math.multiplyExact(x, y) with large integer fallback. */
        final long result = lhs * rhs;
        final long ax = Math.abs(lhs);
        final long ay = Math.abs(rhs);
        if ((ax | ay) >>> 31 != 0) {
            // Some bits greater than 2^31 that might cause overflow
            // Check the result using the divide operator
            // and check for the special case of Long.MIN_VALUE * -1
            if (rhs != 0 && result / rhs != lhs || lhs == Long.MIN_VALUE && rhs == -1) {
                return multiplyLarge(image, lhs, rhs);
            }
        }
        return result;
    }

    @TruffleBoundary
    private static Object multiplyLarge(final SqueakImageContext image, final long lhs, final long rhs) {
        return multiply(image, BigInteger.valueOf(lhs), BigInteger.valueOf(rhs));
    }

    @TruffleBoundary
    public static Object multiply(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return multiply(image, toBigInteger(image, lhs), toBigInteger(image, rhs));
    }

    @TruffleBoundary
    public static Object multiply(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return multiply(image, toBigInteger(image, lhs), BigInteger.valueOf(rhs));
    }

    private static Object multiply(final SqueakImageContext image, final BigInteger lhs, final BigInteger rhs) {
        return normalize(image, lhs.multiply(rhs));
    }

    @TruffleBoundary
    public static Object[] divideAndRemainder(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return divideAndRemainder(image, toBigInteger(image, lhs), BigInteger.valueOf(rhs));
    }

    @TruffleBoundary
    public static Object[] divideAndRemainder(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return divideAndRemainder(image, toBigInteger(image, lhs), toBigInteger(image, rhs));
    }

    private static Object[] divideAndRemainder(final SqueakImageContext image, final BigInteger lhs, final BigInteger rhs) {
        final BigInteger[] divideAndRemainder = lhs.divideAndRemainder(rhs);
        return new Object[]{normalize(image, divideAndRemainder[0]), normalize(image, divideAndRemainder[1])};
    }

    @TruffleBoundary
    public static Object divide(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return divide(image, toBigInteger(image, lhs), toBigInteger(image, rhs));
    }

    @TruffleBoundary
    public static Object divide(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return divide(image, toBigInteger(image, lhs), BigInteger.valueOf(rhs));
    }

    private static Object divide(final SqueakImageContext image, final BigInteger lhs, final BigInteger rhs) {
        return normalize(image, lhs.divide(rhs));
    }

    public static long divide(@SuppressWarnings("unused") final long lhs, final NativeObject rhs) {
        assert !fitsIntoLong(rhs) : "non-reduced large integer!";
        return 0L;
    }

    @TruffleBoundary
    public static Object floorDivide(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return normalize(image, floorDivide(toBigInteger(image, lhs), toBigInteger(image, rhs)));
    }

    @TruffleBoundary
    public static Object floorDivide(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return normalize(image, floorDivide(toBigInteger(image, lhs), BigInteger.valueOf(rhs)));
    }

    private static BigInteger floorDivide(final BigInteger x, final BigInteger y) {
        final BigInteger[] r = x.divideAndRemainder(y);
        /* if the signs are different and modulo not zero, round down */
        if (x.signum() != y.signum() && r[1].signum() != 0) {
            return r[0].subtract(BigInteger.ONE);
        } else {
            return r[0];
        }
    }

    public static long floorDivide(final SqueakImageContext image, final long lhs, final NativeObject rhs) {
        assert !fitsIntoLong(rhs) : "non-reduced large integer!";
        if (lhs != 0 && lhs < 0 ^ isNegative(image, rhs)) {
            return -1L;
        } else {
            return 0L;
        }
    }

    @TruffleBoundary
    public static Object floorMod(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return floorMod(image, toBigInteger(image, lhs), toBigInteger(image, rhs));
    }

    @TruffleBoundary
    public static Object floorMod(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return floorMod(image, toBigInteger(image, lhs), BigInteger.valueOf(rhs));
    }

    @TruffleBoundary
    public static Object floorMod(final SqueakImageContext image, final long lhs, final NativeObject rhs) {
        return floorMod(image, BigInteger.valueOf(lhs), toBigInteger(image, rhs));
    }

    private static Object floorMod(final SqueakImageContext image, final BigInteger lhs, final BigInteger rhs) {
        return normalize(image, lhs.subtract(floorDivide(lhs, rhs).multiply(rhs)));
    }

    @TruffleBoundary
    public static long remainder(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        /* remainder must fit into long */
        return toBigInteger(image, lhs).remainder(BigInteger.valueOf(rhs)).longValue();
    }

    @TruffleBoundary
    public static Object remainder(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return normalize(image, toBigInteger(image, lhs).remainder(toBigInteger(image, rhs)));
    }

    /*
     * Comparison
     */

    @TruffleBoundary
    public static int compareTo(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return toBigInteger(image, lhs).compareTo(toBigInteger(image, rhs));
    }

    public static int compareTo(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        if (fitsIntoInt(lhs)) {
            return Long.compare(longValue(lhs), rhs);
        } else {
            return image.isLargeNegativeInteger(lhs) ? -1 : 1;
        }
    }

    public static boolean lessThanOneShiftedBy64(final NativeObject value) {
        return highBitOfLargeInt(value) <= Long.SIZE;
    }

    public static boolean inRange(final NativeObject value, final long minValue, final long maxValue) {
        if (fitsIntoLong(value)) {
            final long longValueExact = longValue(value);
            return minValue <= longValueExact && longValueExact <= maxValue;
        } else {
            return false;
        }
    }

    /*
     * Bit operations
     */

    @TruffleBoundary
    public static Object and(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return and(image, toBigInteger(image, lhs), toBigInteger(image, rhs));
    }

    @TruffleBoundary
    public static Object and(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return and(image, toBigInteger(image, lhs), BigInteger.valueOf(rhs));
    }

    private static Object and(final SqueakImageContext image, final BigInteger lhs, final BigInteger rhs) {
        return normalize(image, lhs.and(rhs));
    }

    @TruffleBoundary
    public static Object or(final SqueakImageContext image, final long lhs, final NativeObject rhs) {
        return or(image, BigInteger.valueOf(lhs), toBigInteger(image, rhs));
    }

    @TruffleBoundary
    public static Object or(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return or(image, toBigInteger(image, lhs), toBigInteger(image, rhs));
    }

    private static Object or(final SqueakImageContext image, final BigInteger lhs, final BigInteger rhs) {
        return normalize(image, lhs.or(rhs));
    }

    @TruffleBoundary
    public static Object xor(final SqueakImageContext image, final long lhs, final NativeObject rhs) {
        return xor(image, BigInteger.valueOf(lhs), toBigInteger(image, rhs));
    }

    @TruffleBoundary
    public static Object xor(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return xor(image, toBigInteger(image, lhs), toBigInteger(image, rhs));
    }

    private static Object xor(final SqueakImageContext image, final BigInteger lhs, final BigInteger rhs) {
        return normalize(image, lhs.xor(rhs));
    }

    @TruffleBoundary
    public static Object shiftLeft(final SqueakImageContext image, final NativeObject value, final int shiftCount) {
        final BigInteger integer = toBigInteger(image, value);
        final BigInteger result;
        if (integer.signum() < 0 && shiftCount < 0) {
            result = integer.abs().shiftLeft(shiftCount).negate();
        } else {
            result = integer.shiftLeft(shiftCount);
        }
        return normalize(image, result);
    }

    @TruffleBoundary
    public static Object shiftLeftPositive(final SqueakImageContext image, final long lhs, final int rhs) {
        assert rhs >= 0 : "This method must be used with a positive 'b' argument";
        return normalize(image, BigInteger.valueOf(lhs).shiftLeft(rhs));
    }

    /*
     * Checks
     */

    public static boolean sameSign(final NativeObject lhs, final NativeObject rhs) {
        return lhs.getSqueakClass() == rhs.getSqueakClass();
    }

    public static boolean sameSign(final long lhs, final long rhs) {
        return (lhs ^ rhs) >= 0;
    }

    private static long rhsNegatedOnDifferentSign(final long lhs, final long rhs, final InlinedConditionProfile sameSignProfile, final Node node) {
        return sameSignProfile.profile(node, sameSign(lhs, rhs)) ? rhs : -rhs;
    }

    public static boolean fitsIntoInt(final NativeObject value) {
        /* Optimized version of 'highBitOfLargeInt(value) < Integer.SIZE'. */
        final byte[] bytes = value.getByteStorage();
        final int bytesLength = bytes.length;
        return bytesLength < Integer.BYTES || (bytesLength == Integer.BYTES && (bytes[3] & 128) == 0);
    }

    public static boolean fitsIntoLong(final NativeObject value) {
        /* Optimized version of 'highBitOfLargeInt(value) < Long.SIZE'. */
        final byte[] bytes = value.getByteStorage();
        final int bytesLength = bytes.length;
        return bytesLength < Long.BYTES || (bytesLength == Long.BYTES && (bytes[7] & 128) == 0);
    }

    public static boolean isNegative(final SqueakImageContext image, final NativeObject value) {
        return value.getSqueakClass() == image.largeNegativeIntegerClass;
    }

    public static boolean isPositive(final SqueakImageContext image, final NativeObject value) {
        return value.getSqueakClass() == image.largePositiveIntegerClass;
    }

    public static boolean isZero(final NativeObject value) {
        return value.getByteLength() == 0;
    }

    public static boolean isZeroOrPositive(final SqueakImageContext image, final NativeObject value) {
        return isZero(value) || isPositive(image, value);
    }

    /* Utilities */

    public static NativeObject createLongMinOverflowResult(final SqueakImageContext image) {
        return NativeObject.newNativeBytes(image.largePositiveIntegerClass, LONG_MIN_OVERFLOW_RESULT_BYTES.clone());
    }

    private static int highBitOfLargeInt(final NativeObject value) {
        final byte[] storage = value.getByteStorage();
        return cDigitHighBitLen(storage, storage.length);
    }

    private static int cDigitHighBitLen(final byte[] bytes, final int len) {
        int realLength = len;
        long lastDigit;
        do {
            if (realLength == 0) {
                return 0;
            }
        } while ((lastDigit = cDigitOfAt(bytes, --realLength)) == 0);
        return cHighBit32(lastDigit) + (32 * realLength);
    }

    private static int cHighBit32(final long value) {
        return Long.SIZE - Long.numberOfLeadingZeros(value);
    }

    /*
     * Large float operations
     */

    @TruffleBoundary
    public static double fractionPart(final double value) {
        return value - new BigDecimal(value).toBigInteger().doubleValue();
    }

    /*
     * BigInteger conversion
     */

    public static Object normalize(final SqueakImageContext image, final BigInteger value) {
        if (value.bitLength() < Long.SIZE) {
            return value.longValue();
        } else {
            return toNativeObject(image, value);
        }
    }

    public static Object normalize(final SqueakImageChunk chunk, final boolean isNegative) {
        final Object object = normalize(chunk.getImage(), chunk.getBytes(), isNegative);
        if (object instanceof NativeObject o) {
            o.initializeFrom(chunk);
        } else {
            chunk.setObject(object);
        }
        return object;
    }

    public static Object normalize(final SqueakImageContext image, final byte[] bytes, final boolean isNegative) {
        final int byteSize = bytes.length;
        // Compute number of 32-bit digits (rounding up)
        int digitLen = (byteSize + 3) / 4;
        // Strip leading zero words
        while (digitLen != 0 && cDigitOfAt(bytes, digitLen - 1) == 0) {
            digitLen--;
        }
        if (digitLen == 0) {
            return 0L;
        }
        long val = cDigitOfAt(bytes, digitLen - 1);
        if (digitLen <= 2) {
            long val2 = val;
            if (digitLen > 1) {
                val2 = (val2 << 32) + cDigitOfAt(bytes, 0);
            }
            if (val2 >= 0) {
                return isNegative ? -val2 : val2;
            } else if (val2 == Long.MIN_VALUE && isNegative) {
                return Long.MIN_VALUE;
            }
        }

        // Return shortened copy if possible
        int byteLen = digitLen * 4;
        if (val <= 0xFFFF) {
            byteLen -= 2;
        } else {
            val >>= 16;
        }
        if (val <= 0xFF) {
            byteLen--;
        }

        final ClassObject squeakClass = isNegative ? image.largeNegativeIntegerClass : image.largePositiveIntegerClass;
        final byte[] result = byteLen < byteSize ? largeIntGrowTo(bytes, byteLen) : bytes;
        return NativeObject.newNativeBytes(squeakClass, result);
    }

    /**
     * Reads a 32-bit word from the byte array in **little-endian** order. Word index 0 is the least
     * significant 4 bytes.
     */
    private static long cDigitOfAt(final byte[] bytes, final int wordIndex) {
        final int byteIndex = wordIndex * Integer.BYTES;
        final int remainingBytes = bytes.length - byteIndex;
        if (remainingBytes <= 0) {
            return 0L;
        } else if (remainingBytes >= Integer.BYTES) {
            return VarHandleUtils.getInt(bytes, wordIndex) & 0xFFFFFFFFL;
        } else if (remainingBytes == 1) {
            return bytes[byteIndex] & 0xFFL;
        } else if (remainingBytes == 2) {
            return bytes[byteIndex] & 0xFFL | (bytes[byteIndex + 1] & 0xFFL) << 8;
        } else {
            return bytes[byteIndex] & 0xFFL | (bytes[byteIndex + 1] & 0xFFL) << 8 | (bytes[byteIndex + 2] & 0xFFL) << 16;
        }
    }

    private static byte[] largeIntGrowTo(final byte[] bytes, final int byteLen) {
        final byte[] newBytes = new byte[byteLen];
        System.arraycopy(bytes, 0, newBytes, 0, Math.min(bytes.length, byteLen));
        return newBytes;
    }

    public static int intValue(final SqueakImageContext image, final NativeObject value) {
        final byte[] bytes = value.getByteStorage();
        final int signum = image.largePositiveIntegerClass == value.getSqueakClass() ? 1 : -1;
        assert bytes.length > 0 : "Large integer with zero-length bytes not expected";
        int result = 0;
        int shift = 0;
        for (final byte b : bytes) {
            result |= (b & 0xFF) << shift;
            shift += 8;
        }
        return signum * result;
    }

    public static int intValueExact(final SqueakImageContext image, final NativeObject value) {
        if (fitsIntoInt(value)) {
            return intValue(image, value);
        } else {
            throw LARGE_INTEGER_OUT_OF_INT_RANGE;
        }
    }

    public static long longValue(final NativeObject value) {
        final byte[] bytes = value.getByteStorage();
        assert bytes.length > 0 : "Large integer with zero-length bytes not expected";
        final int digitLen = (bytes.length + 3) / 4;
        final long firstInt = cDigitOfAt(bytes, 0);
        if (digitLen > 1) {
            return firstInt + (cDigitOfAt(bytes, 1) << 32);
        } else {
            return firstInt;
        }
    }

    public static long longValueExact(final NativeObject value) {
        if (fitsIntoLong(value)) {
            return longValue(value);
        } else {
            throw LARGE_INTEGER_OUT_OF_LONG_RANGE;
        }
    }

    @TruffleBoundary
    public static long toSignedLong(final SqueakImageContext image, final NativeObject value) {
        final int bitLength = highBitOfLargeInt(value);
        assert isPositive(image, value) && bitLength <= Long.SIZE;
        if (bitLength == Long.SIZE) {
            return toBigInteger(image, value).subtract(ONE_SHIFTED_BY_64).longValue();
        } else {
            return longValue(value);
        }
    }

    @TruffleBoundary
    public static NativeObject toUnsigned(final SqueakImageContext image, final long value) {
        assert value < 0;
        return toNativeObject(image, BigInteger.valueOf(value).add(ONE_SHIFTED_BY_64));
    }

    @TruffleBoundary
    public static BigInteger toBigInteger(final SqueakImageContext image, final NativeObject value) {
        final byte[] bytes = value.getByteStorage();
        final int byteLen = bytes.length;
        final int remainingBytes = byteLen % Integer.BYTES;
        final int digitLen = (byteLen + 3) / Integer.BYTES;
        final int[] magnitude = new int[digitLen];
        final int offset = digitLen - 1;
        final int limit = offset - (remainingBytes == 0 ? 0 : 1);
        for (int i = 0; i <= limit; i++) {
            magnitude[offset - i] = VarHandleUtils.getInt(bytes, i);
        }
        if (remainingBytes == 1) {
            magnitude[0] = bytes[byteLen - 1] & 0xFF;
        } else if (remainingBytes == 2) {
            magnitude[0] = bytes[byteLen - 2] & 0xFF | (bytes[byteLen - 1] & 0xFF) << 8;
        } else if (remainingBytes == 3) {
            magnitude[0] = bytes[byteLen - 3] & 0xFF | (bytes[byteLen - 2] & 0xFF) << 8 | (bytes[byteLen - 1] & 0xFF) << 16;
        }
        return toBigInteger(magnitude, image.isLargeNegativeInteger(value) ? -1 : 1);
    }

    private static BigInteger toBigInteger(final int[] magnitude, final int signum) {
        try {
            return (BigInteger) BIG_INTEGER_INT_ARRAY_INT_CTOR.newInstance(magnitude, signum);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (final IllegalAccessException | InvocationTargetException e) {
            throw SqueakException.create("Error constructing BigInteger", e);
        }
    }

    public static NativeObject toNativeObject(final SqueakImageContext image, final BigInteger result) {
        return NativeObject.newNativeBytes(result.signum() >= 0 ? image.largePositiveIntegerClass : image.largeNegativeIntegerClass, toByteArray(result));
    }

    @TruffleBoundary
    private static byte[] toByteArray(final BigInteger value) {
        final int byteLen = (value.abs().bitLength() + 7) / 8;
        final byte[] byteArray = new byte[byteLen];

        final int[] mag = getMagnitude(value);
        final int magLen = mag.length;

        for (int i = 0, bytesCopied = 4, nextInt = 0, intIndex = 0; i <= byteLen - 1; i++) {
            if (bytesCopied == 4) {
                nextInt = mag[magLen - intIndex++ - 1];
                bytesCopied = 1;
            } else {
                nextInt >>>= 8;
                bytesCopied++;
            }
            byteArray[i] = (byte) nextInt;
        }
        return byteArray;
    }

    private static int[] getMagnitude(final BigInteger value) {
        try {
            return (int[]) BIG_INTEGER_MAG_FIELD.get(value);
        } catch (final IllegalAccessException e) {
            throw SqueakException.create("Error accessing magnitude", e);
        }
    }
}
