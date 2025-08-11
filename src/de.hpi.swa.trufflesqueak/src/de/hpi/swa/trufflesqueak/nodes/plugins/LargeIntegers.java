/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
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
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.RespecializeException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
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
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.ReflectionUtils;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class LargeIntegers extends AbstractPrimitiveFactoryHolder {
    private static final Field BIG_INTEGER_MAG_FIELD = ReflectionUtils.lookupField(BigInteger.class, "mag");

    private static final BigInteger ONE_SHIFTED_BY_64 = BigInteger.ONE.shiftLeft(64);
    public static final BigInteger LONG_MIN_OVERFLOW_RESULT = BigInteger.valueOf(Long.MIN_VALUE).abs();
    @CompilationFinal(dimensions = 1) public static final byte[] LONG_MIN_OVERFLOW_RESULT_BYTES = toByteArray(LONG_MIN_OVERFLOW_RESULT);

    private static final ArithmeticException BIG_INTEGER_OUT_OF_LONG_RANGE = new ArithmeticException("BigInteger out of long range");
    private static final String MODULE_NAME = "LargeIntegers v2.0 (TruffleSqueak)";

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primAnyBitFromTo")
    protected abstract static class PrimAnyBitFromToNode extends AbstractArithmeticPrimitiveNode implements Primitive2WithFallback {
        private final BranchProfile startLargerThanStopProfile = BranchProfile.create();
        private final ConditionProfile firstAndLastDigitIndexIdenticalProfile = ConditionProfile.create();
        private final BranchProfile firstDigitNonZeroProfile = BranchProfile.create();
        private final BranchProfile middleDigitsNonZeroProfile = BranchProfile.create();

        @Specialization(guards = {"start >= 1", "stopArg >= 1"})
        protected final boolean doLong(final long receiver, final long start, final long stopArg) {
            final long stop = Math.min(stopArg, Long.highestOneBit(receiver));
            if (start > stop) {
                startLargerThanStopProfile.enter();
                return BooleanObject.FALSE;
            }
            final int firstDigitIndex = ((int) start - 1) / 8 + 1;
            final int lastDigitIndex = ((int) stop - 1) / 8 + 1;
            final int rightShift = -(((int) start - 1) % 8);
            final int leftShift = 7 - ((int) stop - 1) % 8;
            if (firstAndLastDigitIndexIdenticalProfile.profile(firstDigitIndex == lastDigitIndex)) {
                final int mask = 0xFF >> rightShift & 0xFF >> leftShift;
                final byte digit = digitOf(receiver, firstDigitIndex - 1);
                return BooleanObject.wrap((digit & mask) != 0);
            } else {
                if (digitOf(receiver, firstDigitIndex - 1) << rightShift != 0) {
                    firstDigitNonZeroProfile.enter();
                    return BooleanObject.TRUE;
                }
                for (long i = firstDigitIndex + 1; i < lastDigitIndex; i++) {
                    if (digitOf(receiver, i - 1) != 0) {
                        middleDigitsNonZeroProfile.enter();
                        return BooleanObject.TRUE;
                    }
                }
                return BooleanObject.wrap((digitOf(receiver, lastDigitIndex - 1) << leftShift & 0xFF) != 0);
            }
        }

        @Specialization(guards = {"start >= 1", "stopArg >= 1"}, rewriteOn = {ArithmeticException.class})
        protected final boolean doLargeIntegerAsLong(final NativeObject receiver, final long start, final long stopArg,
                        @Bind final SqueakImageContext image) {
            return doLong(longValueExact(image, receiver), start, stopArg);
        }

        @Specialization(guards = {"start >= 1", "stopArg >= 1"})
        protected final boolean doLargeInteger(final NativeObject receiver, final long start, final long stopArg) {
            final long stop = Math.min(stopArg, highBitOfLargeInt(receiver));
            if (start > stop) {
                startLargerThanStopProfile.enter();
                return BooleanObject.FALSE;
            }
            final byte[] bytes = receiver.getByteStorage();
            final int firstDigitIndex = ((int) start - 1) / 8 + 1;
            final int lastDigitIndex = ((int) stop - 1) / 8 + 1;
            final int rightShift = -(((int) start - 1) % 8);
            final int leftShift = 7 - ((int) stop - 1) % 8;
            if (firstAndLastDigitIndexIdenticalProfile.profile(firstDigitIndex == lastDigitIndex)) {
                final int mask = 0xFF >> rightShift & 0xFF >> leftShift;
                final byte digit = bytes[firstDigitIndex - 1];
                return BooleanObject.wrap((digit & mask) != 0);
            } else {
                if (bytes[firstDigitIndex - 1] << rightShift != 0) {
                    firstDigitNonZeroProfile.enter();
                    return BooleanObject.TRUE;
                }
                for (int i = firstDigitIndex + 1; i < lastDigitIndex; i++) {
                    if (bytes[i - 1] != 0) {
                        middleDigitsNonZeroProfile.enter();
                        return BooleanObject.TRUE;
                    }
                }
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
                        @Cached final InlinedConditionProfile differentSignProfile) {
            return Math.addExact(lhs, rhsNegatedOnDifferentSign(lhs, rhs, differentSignProfile, node));
        }

        @Specialization(replaces = "doLong")
        protected static final Object doLongWithOverflow(final long lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return digitAdd(image, lhs, rhs);
        }

        @Specialization
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return digitAdd(image, lhs, rhs);
        }

        @Specialization
        protected final Object doLongLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return digitAdd(image, rhs, lhs);
        }

        @Specialization
        protected final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return digitAdd(image, lhs, rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitSubtract")
    protected abstract static class PrimDigitSubtractNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("differentSignProfile") @Cached final InlinedConditionProfile differentSignProfile) {
            return Math.subtractExact(lhs, rhsNegatedOnDifferentSign(lhs, rhs, differentSignProfile, node));
        }

        @Specialization(replaces = "doLong")
        protected final Object doLongWithOverflow(final long lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("differentSignProfile") @Cached final InlinedConditionProfile differentSignProfile) {
            return subtract(getContext(), lhs, rhsNegatedOnDifferentSign(lhs, rhs, differentSignProfile, node));
        }

        @Specialization
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Shared("differentSignProfile") @Cached final InlinedConditionProfile differentSignProfile) {
            if (differentSignProfile.profile(node, differentSign(lhs, rhs))) {
                return add(image, lhs, rhs);
            } else {
                return subtract(image, lhs, rhs);
            }
        }

        @Specialization
        protected final Object doLongLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Shared("differentSignProfile") @Cached final InlinedConditionProfile differentSignProfile) {
            if (differentSignProfile.profile(node, differentSign(getContext(), rhs, lhs))) {
                return add(image, rhs, lhs);
            } else {
                return subtract(image, lhs, rhs);
            }
        }

        @Specialization
        protected final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Shared("differentSignProfile") @Cached final InlinedConditionProfile differentSignProfile) {
            if (differentSignProfile.profile(node, differentSign(getContext(), lhs, rhs))) {
                return add(image, lhs, rhs);
            } else {
                return subtract(image, lhs, rhs);
            }
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
        protected final Object doLongWithOverflow(final long lhs, final long rhs, @SuppressWarnings("unused") final boolean neg) {
            return multiply(getContext(), lhs, rhs);
        }

        @Specialization
        protected static final Object doLongLargeInteger(final long lhs, final NativeObject rhs, @SuppressWarnings("unused") final boolean neg,
                        @Bind final SqueakImageContext image) {
            return multiply(image, rhs, lhs);
        }

        @Specialization
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs, @SuppressWarnings("unused") final boolean neg,
                        @Bind final SqueakImageContext image) {
            return multiply(image, lhs, rhs);
        }

        @Specialization
        protected static final double doDouble(final double lhs, final double rhs, @SuppressWarnings("unused") final boolean neg) {
            return lhs * rhs;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitBitAnd")
    protected abstract static class PrimDigitBitAndNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final long doLong(final long receiver, final long arg) {
            return receiver & arg;
        }

        @Specialization
        protected static final Object doLargeInteger(final NativeObject receiver, final NativeObject arg,
                        @Bind final SqueakImageContext image) {
            return and(image, receiver, arg);
        }

        @Specialization
        protected static final Object doLong(final long receiver, final NativeObject arg,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Shared("positiveProfile") @Cached final InlinedConditionProfile positiveProfile) {
            if (positiveProfile.profile(node, receiver >= 0)) {
                return receiver & longValue(image, arg);
            } else {
                return and(image, arg, receiver);
            }
        }

        @Specialization
        protected static final Object doLargeInteger(final NativeObject receiver, final long arg,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Shared("positiveProfile") @Cached final InlinedConditionProfile positiveProfile) {
            if (positiveProfile.profile(node, arg >= 0)) {
                return longValue(image, receiver) & arg;
            } else {
                return and(image, receiver, arg);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitBitOr")
    protected abstract static class PrimDigitBitOrNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final long bitOr(final long receiver, final long arg) {
            return receiver | arg;
        }

        @Specialization
        protected static final Object doLargeInteger(final NativeObject receiver, final NativeObject arg,
                        @Bind final SqueakImageContext image) {
            return or(image, receiver, arg);
        }

        @Specialization
        protected static final Object doLong(final long receiver, final NativeObject arg,
                        @Bind final SqueakImageContext image) {
            return or(image, arg, receiver);
        }

        @Specialization
        protected static final Object doLargeInteger(final NativeObject receiver, final long arg,
                        @Bind final SqueakImageContext image) {
            return or(image, receiver, arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitBitShiftMagnitude")
    public abstract static class PrimDigitBitShiftMagnitudeNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doLargeInteger(final NativeObject receiver, final long arg,
                        @Bind final SqueakImageContext image) {
            return shiftLeft(image, receiver, (int) arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitBitXor")
    protected abstract static class PrimBitXorNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final long doLong(final long lhs, final long rhs) {
            return lhs ^ rhs;
        }

        @Specialization
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return xor(image, lhs, rhs);
        }

        @Specialization
        protected static final Object doLong(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return xor(image, rhs, lhs);
        }

        @Specialization
        protected static final Object doLargeInteger(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return xor(image, lhs, rhs);
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

        @Specialization
        protected static final long doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return compareTo(image, lhs, rhs);
        }

        @Specialization
        protected static final long doLongLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Shared("fitsIntoLongProfile") @Cached final InlinedConditionProfile fitsIntoLongProfile) {
            if (fitsIntoLongProfile.profile(node, fitsIntoLong(rhs))) {
                final long value = longValue(image, rhs);
                return value == lhs ? 0L : value < lhs ? -1L : 1L;
            } else {
                return isNegative(image, rhs) ? 1L : -1L;
            }
        }

        @Specialization
        protected static final long doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Shared("fitsIntoLongProfile") @Cached final InlinedConditionProfile fitsIntoLongProfile) {
            if (fitsIntoLongProfile.profile(node, fitsIntoLong(lhs))) {
                final long value = longValue(image, lhs);
                return value == rhs ? 0L : value < rhs ? -1L : 1L;
            } else {
                return isNegative(image, lhs) ? -1L : 1L;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitDivNegative")
    protected abstract static class PrimDigitDivNegativeNode extends AbstractArithmeticPrimitiveNode implements Primitive2WithFallback {
        @Specialization
        protected final ArrayObject doLong(final long rcvr, final long arg, final boolean negative,
                        @Bind final Node node,
                        @Shared("signProfile") @Cached final InlinedBranchProfile signProfile) {
            final SqueakImageContext image = getContext();
            long divide = rcvr / arg;
            if (negative && divide >= 0 || !negative && divide < 0) {
                signProfile.enter(node);
                if (divide == Long.MIN_VALUE) {
                    return createArrayWithLongMinOverflowResult(image, rcvr, arg);
                }
                divide = -divide;
            }
            return image.asArrayOfLongs(divide, rcvr % arg);
        }

        @TruffleBoundary
        private static ArrayObject createArrayWithLongMinOverflowResult(final SqueakImageContext image, final long rcvr, final long arg) {
            return image.asArrayOfObjects(createLongMinOverflowResult(image), rcvr % arg);
        }

        @Specialization
        @TruffleBoundary
        protected final ArrayObject doLargeInteger(final NativeObject rcvr, final NativeObject arg, final boolean negative,
                        @Bind final SqueakImageContext image) {
            final BigInteger[] divide = toBigInteger(image, rcvr).divideAndRemainder(toBigInteger(image, arg));
            final Object[] result = digitDivNegative(negative, image, divide);
            if (divide[1].bitLength() < Long.SIZE) {
                result[1] = divide[1].longValue();
            } else {
                result[1] = toNativeObject(image, divide[1]);
            }
            return image.asArrayOfObjects(result);
        }

        @Specialization
        protected final ArrayObject doLongLargeInteger(final long rcvr, final NativeObject arg, @SuppressWarnings("unused") final boolean negative,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Shared("signProfile") @Cached final InlinedBranchProfile signProfile) {
            if (LargeIntegers.fitsIntoLong(arg)) {
                return doLong(rcvr, LargeIntegers.longValue(image, arg), negative, node, signProfile);
            } else {
                return getContext().asArrayOfLongs(0L, rcvr);
            }
        }

        @Specialization
        @TruffleBoundary
        protected final ArrayObject doLargeIntegerLong(final NativeObject rcvr, final long arg, final boolean negative,
                        @Bind final SqueakImageContext image) {
            final BigInteger[] divide = toBigInteger(image, rcvr).divideAndRemainder(BigInteger.valueOf(arg));
            final Object[] result = digitDivNegative(negative, image, divide);
            result[1] = divide[1].longValue();
            return image.asArrayOfObjects(result);
        }

        private static Object[] digitDivNegative(final boolean negative, final SqueakImageContext image, final BigInteger[] divide) {
            final Object[] result = new Object[2];
            if (negative != divide[0].signum() < 0) {
                if (divide[0].bitLength() < Long.SIZE) {
                    final long lresult = divide[0].longValue();
                    if (lresult == Long.MIN_VALUE) {
                        result[0] = createLongMinOverflowResult(image);
                    } else {
                        result[0] = -lresult;
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
        protected final Object doGet(@SuppressWarnings("unused") final Object rcvr) {
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
        protected final Object doGeneric(final Object receiver, final Object a, final Object m, final Object mInv,
                        @Bind final Node node,
                        @Cached final ToIntsNode receiverToIntsNode,
                        @Cached final ToIntsNode aToIntsNode,
                        @Cached final ToIntsNode mToIntsNode,
                        @Cached final ToExactLongNode toExactLongNode) {
            return doLargeInteger(receiverToIntsNode.execute(node, receiver), aToIntsNode.execute(node, a), mToIntsNode.execute(node, m), toExactLongNode.execute(node, mInv));
        }

        private static boolean fitsInOneWord(final long value) {
            return value <= NativeObject.INTEGER_MAX;
        }

        private Object doLargeInteger(final int[] firstInts, final int[] secondInts, final int[] thirdInts, final long mInv) {
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
            final byte[] resultBytes = UnsafeUtils.toBytes(result);
            final SqueakImageContext image = getContext();
            return normalize(image, NativeObject.newNativeBytes(image, image.largePositiveIntegerClass, resultBytes), true);
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

            @Specialization
            protected static final int[] doLargeInteger(final NativeObject value) {
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

            @Specialization
            protected static final long doLargeInteger(final NativeObject value,
                            @Bind final SqueakImageContext image) {
                return longValueExact(image, value);
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
        @Specialization(guards = {"receiver.isByteType()", "getContext().isLargeIntegerClass(receiver.getSqueakClass())"})
        protected final Object doNativeObject(final NativeObject receiver) {
            return normalize(getContext(), receiver, true);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primNormalizeNegative")
    protected abstract static class PrimNormalizeNegativeNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"receiver.isByteType()", "getContext().isLargeIntegerClass(receiver.getSqueakClass())"})
        protected final Object doNativeObject(final NativeObject receiver) {
            return normalize(getContext(), receiver, false);
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return LargeIntegersFactory.getFactories();
    }

    /*
     * Arithmetic Operations
     */

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object add(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return normalize(image, toBigInteger(image, lhs).add(toBigInteger(image, rhs)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object add(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return normalize(image, toBigInteger(image, lhs).add(BigInteger.valueOf(rhs)));
    }

    public static Object add(final SqueakImageContext image, final long lhs, final long rhs) {
        /* Inlined version of Math.addExact(x, y) with large integer fallback. */
        final long result = lhs + rhs;
        // HD 2-12 Overflow iff both arguments have the opposite sign of the result
        if (((lhs ^ result) & (rhs ^ result)) < 0) {
            return addLarge(image, lhs, rhs);
        }
        return result;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    private static NativeObject addLarge(final SqueakImageContext image, final long lhs, final long rhs) {
        return toNativeObject(image, BigInteger.valueOf(lhs).add(BigInteger.valueOf(rhs)));
    }

    public static Object digitAdd(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return digitAdd(image, lhs.getByteStorage(), rhs.getByteStorage(), image.isLargeNegativeIntegerObject(lhs));
    }

    public static Object digitAdd(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return digitAdd(image, lhs.getByteStorage(), createLargeFromSmallInteger(rhs), image.isLargeNegativeIntegerObject(lhs));
    }

    public static Object digitAdd(final SqueakImageContext image, final long lhs, final long rhs) {
        final long x = lhs;
        final long y = differentSign(lhs, rhs) ? rhs : -rhs;
        final long r = x + y;
        // HD 2-12 Overflow iff both arguments have the opposite sign of the result
        if (((x ^ r) & (y ^ r)) < 0) {
            return digitAdd(image, createLargeFromSmallInteger(lhs), createLargeFromSmallInteger(rhs), lhs < 0);
        }
        return r;
    }

    private static byte[] createLargeFromSmallInteger(final long value) {
        final int byteSize = byteSizeOfCSI(value);
        final byte[] result = new byte[byteSize];
        final long abs = Math.abs(value);
        for (int i = 0; i < byteSize; i++) {
            result[i] = (byte) (abs >> (i * 8));
        }
        return result;
    }

    private static int byteSizeOfCSI(final long csi) {
        if (csi >= 0) {
            if (csi < 256) {
                return 1;
            } else if (csi < 65536) {
                return 2;
            } else if (csi < 16777216) {
                return 3;
            } else if (csi < 4294967296L) {
                return 4;
            } else if (csi < 1099511627776L) {
                return 5;
            } else if (csi < 281474976710656L) {
                return 6;
            } else if (csi < 72057594037927936L) {
                return 7;
            } else {
                return 8;
            }
        } else {
            if (csi > -256) {
                return 1;
            } else if (csi > -65536) {
                return 2;
            } else if (csi > -16777216) {
                return 3;
            } else if (csi > -4294967296L) {
                return 4;
            } else if (csi > -1099511627776L) {
                return 5;
            } else if (csi > -281474976710656L) {
                return 6;
            } else if (csi > -72057594037927936L) {
                return 7;
            } else {
                return 8;
            }
        }
    }

    private static Object digitAdd(final SqueakImageContext image, final byte[] lhsBytes, final byte[] rhsBytes, final boolean neg) {
        final int lhsBytesLen = lhsBytes.length;
        final int rhsBytesLen = rhsBytes.length;
        final byte[] bigInt = ArrayUtils.swapOrderCopy(new BigInteger(1, ArrayUtils.swapOrderCopy(lhsBytes)).add(new BigInteger(1, ArrayUtils.swapOrderCopy(rhsBytes))).toByteArray());
        final int firstDigitLen = (lhsBytesLen + 3) / 4;
        final int secondDigitLen = (rhsBytesLen + 3) / 4;
        final byte[] shortInt;
        final int shortDigitLen;
        final byte[] longInt;
        final int longDigitLen;
        if (firstDigitLen <= secondDigitLen) {
            shortInt = lhsBytes;
            shortDigitLen = firstDigitLen;
            longInt = rhsBytes;
            longDigitLen = secondDigitLen;
        } else {
            shortInt = rhsBytes;
            shortDigitLen = secondDigitLen;
            longInt = lhsBytes;
            longDigitLen = firstDigitLen;
        }
        final int sumLen = longDigitLen * 4;
        final NativeObject sum = NativeObject.newNativeBytes(image, neg ? image.largeNegativeIntegerClass : image.largePositiveIntegerClass, sumLen);
        final int over = cDigitAddlenwithleninto(shortInt, shortDigitLen, longInt, longDigitLen, sum.getByteStorage());
        if (over > 0) {
            final byte[] newSum = new byte[sumLen + 1];
            System.arraycopy(sum.getByteStorage(), 0, newSum, 0, sumLen);
            newSum[longDigitLen * 4] = (byte) over;
            // writeWordLE(newSum, longDigitLen, over);
            sum.setStorage(newSum);
            return sum;
        } else {
            final Object res = normalize(image, sum, !neg);
            if (res instanceof NativeObject n) {
                final int len = n.getByteLength();
                assert Arrays.equals(n.getByteStorage(), 0, len, bigInt, 0, len);
            }
            return res;
        }
    }

    private static int cDigitAddlenwithleninto(final byte[] shortInt, final int shortLen, final byte[] longInteger, final int longLen, final byte[] result) {
        long accum = 0;
        for (int i = 0; i < shortLen; i += 1) {
            accum = (((accum) >> 32) + (readWordLE(shortInt, i) + (readWordLE(longInteger, i))));
            writeWordLE(result, i, (int) (accum & 0xFFFFFFFFL));
        }
        for (int i = shortLen; i < longLen; i += 1) {
            accum = ((accum) >> 32) + readWordLE(longInteger, i);
            writeWordLE(result, i, (int) (accum & 0xFFFFFFFFL));
        }
        return (int) (accum >> 32 & 0xFFFFFFFFL);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object subtract(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return normalize(image, toBigInteger(image, lhs).subtract(toBigInteger(image, rhs)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object subtract(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return normalize(image, toBigInteger(image, lhs).subtract(BigInteger.valueOf(rhs)));
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

    @TruffleBoundary(transferToInterpreterOnException = false)
    private static NativeObject subtractLarge(final SqueakImageContext image, final long lhs, final long rhs) {
        return toNativeObject(image, BigInteger.valueOf(lhs).subtract(BigInteger.valueOf(rhs)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object subtract(final SqueakImageContext image, final long lhs, final NativeObject rhs) {
        return normalize(image, BigInteger.valueOf(lhs).subtract(toBigInteger(image, rhs)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object multiply(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return normalize(image, toBigInteger(image, lhs).multiply(toBigInteger(image, rhs)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object multiply(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        if (rhs == 0) {
            return 0L;
        }
        return normalize(image, toBigInteger(image, lhs).multiply(BigInteger.valueOf(rhs)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
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
                return toNativeObject(image, BigInteger.valueOf(lhs).multiply(BigInteger.valueOf(rhs)));
            }
        }
        return result;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object divide(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return normalize(image, toBigInteger(image, lhs).divide(toBigInteger(image, rhs)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object divide(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return normalize(image, toBigInteger(image, lhs).divide(BigInteger.valueOf(rhs)));
    }

    public static long divide(@SuppressWarnings("unused") final long lhs, final NativeObject rhs) {
        assert !fitsIntoLong(rhs) : "non-reduced large integer!";
        return 0L;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object floorDivide(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return normalize(image, floorDivide(toBigInteger(image, lhs), toBigInteger(image, rhs)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object floorDivide(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return normalize(image, floorDivide(toBigInteger(image, lhs), BigInteger.valueOf(rhs)));
    }

    public static long floorDivide(final SqueakImageContext image, final long lhs, final NativeObject rhs) {
        assert !fitsIntoLong(rhs) : "non-reduced large integer!";
        if (lhs != 0 && lhs < 0 ^ isNegative(image, rhs)) {
            return -1L;
        } else {
            return 0L;
        }
    }

    private static BigInteger floorDivide(final BigInteger x, final BigInteger y) {
        final BigInteger[] r = x.divideAndRemainder(y);
        // if the signs are different and modulo not zero, round down
        if (x.signum() != y.signum() && r[1].signum() != 0) {
            return r[0].subtract(BigInteger.ONE);
        } else {
            return r[0];
        }
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object floorMod(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return floorMod(image, toBigInteger(image, lhs), toBigInteger(image, rhs));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object floorMod(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return floorMod(image, toBigInteger(image, lhs), BigInteger.valueOf(rhs));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object floorMod(final SqueakImageContext image, final long lhs, final NativeObject rhs) {
        return floorMod(image, BigInteger.valueOf(lhs), toBigInteger(image, rhs));
    }

    private static Object floorMod(final SqueakImageContext image, final BigInteger lhs, final BigInteger rhs) {
        return normalize(image, lhs.subtract(floorDivide(lhs, rhs).multiply(rhs)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static long remainder(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return toBigInteger(image, lhs).remainder(BigInteger.valueOf(rhs)).longValue();
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object remainder(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return normalize(image, toBigInteger(image, lhs).remainder(toBigInteger(image, rhs)));
    }

    /*
     * Comparison
     */

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static int compareTo(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return toBigInteger(image, lhs).compareTo(toBigInteger(image, rhs));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static int compareTo(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        final BigInteger integer = toBigInteger(image, lhs);
        if (integer.bitLength() < Long.SIZE) {
            return Long.compare(integer.longValue(), rhs);
        } else {
            return integer.signum();
        }
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static boolean lessThanOrEqualTo(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        if (lhs.getByteLength() < Long.SIZE) {
            return longValue(image, lhs) <= rhs;
        } else {
            return isNegative(image, lhs);
        }
    }

    public static boolean lessThanOneShiftedBy64(final NativeObject value) {
        return value.getByteLength() < Long.SIZE + 1;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static boolean inRange(final SqueakImageContext image, final NativeObject value, final long minValue, final long maxValue) {
        if (value.getByteLength() < Long.SIZE) {
            final long longValueExact = longValue(image, value);
            return minValue <= longValueExact && longValueExact <= maxValue;
        }
        return false;
    }

    private static void sameResult(final Object result, final Object result2) {
        if (result instanceof NativeObject r) {
            assert Arrays.equals(r.getByteStorage(), ((NativeObject) result2).getByteStorage());
        } else {
            assert (long) result == (long) result2;
        }
    }

    /*
     * Bit operations
     */

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object and(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return normalize(image, toBigInteger(image, lhs).and(toBigInteger(image, rhs)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object and(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return normalize(image, toBigInteger(image, lhs).and(BigInteger.valueOf(rhs)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object or(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return normalize(image, toBigInteger(image, lhs).or(toBigInteger(image, rhs)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object or(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return normalize(image, toBigInteger(image, lhs).or(BigInteger.valueOf(rhs)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object xor(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return normalize(image, toBigInteger(image, lhs).xor(toBigInteger(image, rhs)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object xor(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return normalize(image, toBigInteger(image, lhs).xor(BigInteger.valueOf(rhs)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object shiftLeft(final SqueakImageContext image, final NativeObject lhs, final int rhs) {
        final BigInteger integer = toBigInteger(image, lhs);
        if (integer.signum() < 0 && rhs < 0) {
            return normalize(image, integer.abs().shiftLeft(rhs).negate());
        }
        return normalize(image, integer.shiftLeft(rhs));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object shiftLeftPositive(final SqueakImageContext image, final long lhs, final int rhs) {
        assert rhs >= 0 : "This method must be used with a positive 'b' argument";
        return normalize(image, BigInteger.valueOf(lhs).shiftLeft(rhs));
    }

    /*
     * Checks
     */
    public static boolean differentSign(final NativeObject lhs, final NativeObject rhs) {
        return lhs.getSqueakClass() != rhs.getSqueakClass();
    }

    public static boolean differentSign(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return isNegative(image, lhs) ^ rhs < 0;
    }

    private static boolean differentSign(final long lhs, final long rhs) {
        return lhs < 0 ^ rhs < 0;
    }

    public static long rhsNegatedOnDifferentSign(final long lhs, final long rhs, final InlinedConditionProfile differentSignProfile, final Node node) {
        return differentSignProfile.profile(node, differentSign(lhs, rhs)) ? -rhs : rhs;
    }

    public static boolean fitsIntoLong(final NativeObject value) {
        return value.getByteLength() < Long.BYTES;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static boolean isIntegralWhenDividedBy(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return toBigInteger(image, lhs).remainder(toBigInteger(image, rhs)).signum() == 0;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static boolean isIntegralWhenDividedBy(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return toBigInteger(image, lhs).remainder(BigInteger.valueOf(rhs)).signum() == 0;
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

    /*
     * LargeInteger
     */
    @TruffleBoundary
    public static NativeObject createLongMinOverflowResult(final SqueakImageContext image) {
        return toNativeObject(image, LONG_MIN_OVERFLOW_RESULT);
    }

    /* Utilities */

    private static int highBitOfLargeInt(final NativeObject value) {
        final byte[] storage = value.getByteStorage();
        return cDigitHighBitLen(storage, storage.length);
    }

    private static int cDigitHighBitLen(final byte[] bytes, final int len) {
        int realLength = len;
        byte lastDigit;
        do {
            if (realLength == 0) {
                return 0;
            }
        } while ((lastDigit = bytes[--realLength]) == 0);
        return highBitOfByte(lastDigit) + (8 * realLength);
    }

    private static int highBitOfByte(final byte value) {
        return Integer.SIZE - Integer.numberOfLeadingZeros(value);
    }

    /*
     * BigInteger conversion
     */

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object truncateExact(final SqueakImageContext image, final double value) {
        return normalize(image, new BigDecimal(value).toBigInteger());
    }

    public static Object normalize(final SqueakImageContext image, final BigInteger value) {
        if (value.bitLength() < Long.SIZE) {
            return value.longValue();
        } else {
            return toNativeObject(image, value);
        }
    }

    public static Object normalize(final SqueakImageContext image, final NativeObject value, final boolean isPositive) {
        final byte[] bytes = value.getByteStorage();
        final int byteSize = bytes.length;
        // Compute number of 32-bit digits (rounding up)
        int digitLen = (byteSize + 3) / 4;
        // Strip leading zero words
        while (digitLen != 0 && readWordLE(bytes, digitLen - 1) == 0) {
            digitLen--;
        }
        if (digitLen == 0) {
            return 0L;
        }
        long val = readWordLE(bytes, digitLen - 1);
        if (digitLen <= 2) {
            long val2 = val;
            if (digitLen > 1) {
                val2 = (val2 << 32) + readWordLE(bytes, 0);
            }
            if (val2 >= 0) {
                return isPositive ? val2 : -val2;
            } else if (val2 == Long.MIN_VALUE && !isPositive) {
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
            byteLen -= 1;
        }

        if (byteLen < byteSize) {
            return largeIntGrowTo(image, value, byteLen);
        } else {
            return value;
        }
    }

    /**
     * Reads a 32-bit word from the byte array in **little-endian** order. Word index 0 is the least
     * significant 4 bytes.
     */
    private static long readWordLE(final byte[] bytes, final int wordIndex) {
        final int base = wordIndex * 4;
        final long b0 = (base + 0 < bytes.length) ? (bytes[base + 0] & 0xFFL) : 0;
        final long b1 = (base + 1 < bytes.length) ? (bytes[base + 1] & 0xFFL) : 0;
        final long b2 = (base + 2 < bytes.length) ? (bytes[base + 2] & 0xFFL) : 0;
        final long b3 = (base + 3 < bytes.length) ? (bytes[base + 3] & 0xFFL) : 0;
        return (b0 | (b1 << 8) | (b2 << 16) | (b3 << 24)) & 0xFFFFFFFFL;
    }

    static void writeWordLE(final byte[] bytes, final int wordIndex, final long value) {
        final int base = wordIndex * 4;
        bytes[base + 0] = (byte) value;
        bytes[base + 1] = (byte) (value >> 8);
        bytes[base + 2] = (byte) (value >> 16);
        bytes[base + 3] = (byte) (value >> 24);
    }

    private static NativeObject largeIntGrowTo(final SqueakImageContext image, final NativeObject value, final int byteLen) {
        final byte[] bytes = value.getByteStorage();
        final byte[] newBytes = new byte[byteLen];
        System.arraycopy(bytes, 0, newBytes, 0, Math.min(bytes.length, byteLen));
        return NativeObject.newNativeBytes(image, value.getSqueakClass(), newBytes);
    }

    public static int intValue(final SqueakImageContext image, final NativeObject value) {
        final byte[] bytes = value.getByteStorage();
        final int signum = image.largePositiveIntegerClass == value.getSqueakClass() ? 1 : -1;
        assert bytes.length > 0 : "Fixme? 0L : -0L?";
        int result = 0;
        int shift = 0;
        for (final byte b : bytes) {
            result |= (b & 0xFF) << shift;
            shift += 8;
        }
        return signum * result;
    }

    public static int intValueExact(final SqueakImageContext image, final NativeObject value) {
        if (value.getByteLength() <= Integer.BYTES) {
            return intValue(image, value);
        } else {
            throw new ArithmeticException("BigInteger out of int range");
        }
    }

    public static long longValue(final SqueakImageContext image, final NativeObject value) {
        final byte[] bytes = value.getByteStorage();
        final int signum = image.largePositiveIntegerClass == value.getSqueakClass() ? 1 : -1;
        assert bytes.length > 0 : "Fixme? 0L : -0L?";
        long result = 0;
        int shift = 0;
        for (final byte b : bytes) {
            result |= (b & 0xFFL) << shift;
            shift += 8;
        }
        return signum * result;
    }

    public static long longValueExact(final SqueakImageContext image, final NativeObject value) {
        /*
         * FIXME: <=? negative numbers can overflow (`((2 raisedTo: 63) negated bitAnd: (2 raisedTo:
         * 63) negated - 1)`)
         */
        if (highBitOfLargeInt(value) <= 63) {
            return longValue(image, value);
        } else {
            throw BIG_INTEGER_OUT_OF_LONG_RANGE;
        }
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static long toSignedLong(final SqueakImageContext image, final NativeObject value) {
        final int bitLength = value.getByteLength();
        assert isPositive(SqueakImageContext.getSlow(), value) && bitLength <= Long.SIZE;
        if (bitLength == Long.SIZE) {
            return toBigInteger(image, value).subtract(ONE_SHIFTED_BY_64).longValue();
        } else {
            return longValue(image, value);
        }
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static NativeObject toUnsigned(final SqueakImageContext image, final long value) {
        assert value < 0;
        return toNativeObject(image, BigInteger.valueOf(value).add(ONE_SHIFTED_BY_64));
    }

    @TruffleBoundary
    public static BigInteger toBigInteger(final SqueakImageContext image, final NativeObject value) {
        return new BigInteger(image.largePositiveIntegerClass == value.getSqueakClass() ? 1 : -1, ArrayUtils.swapOrderCopy(value.getByteStorage()));
    }

    public static NativeObject toNativeObject(final SqueakImageContext image, final BigInteger result) {
        return NativeObject.newNativeBytes(image, result.signum() >= 0 ? image.largePositiveIntegerClass : image.largeNegativeIntegerClass, toByteArray(result));
    }

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
            throw new RuntimeException(e);
        }
    }
}
