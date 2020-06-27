/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.math.BigInteger;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.PrimitiveValueProfile;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.AbstractArithmeticPrimitiveNode;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class LargeIntegers extends AbstractPrimitiveFactoryHolder {
    private static final String MODULE_NAME = "LargeIntegers v2.0 (TruffleSqueak)";

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primAnyBitFromTo")
    protected abstract static class PrimAnyBitFromToNode extends AbstractArithmeticPrimitiveNode implements TernaryPrimitive {
        private final BranchProfile startLargerThanStopProfile = BranchProfile.create();
        private final ConditionProfile firstAndLastDigitIndexIdenticalProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile firstDigitNonZeroProfile = BranchProfile.create();
        private final BranchProfile middleDigitsNonZeroProfile = BranchProfile.create();
        private final PrimitiveValueProfile lastDigitNonZeroProfile = PrimitiveValueProfile.createEqualityProfile();

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
                return BooleanObject.wrap(lastDigitNonZeroProfile.profile((digitOf(receiver, lastDigitIndex - 1) << leftShift & 0xFF) != 0));
            }
        }

        @Specialization(guards = {"start >= 1", "stopArg >= 1"}, rewriteOn = {ArithmeticException.class})
        protected final boolean doLargeIntegerAsLong(final LargeIntegerObject receiver, final long start, final long stopArg) {
            return doLong(receiver.longValueExact(), start, stopArg);
        }

        @Specialization(guards = {"start >= 1", "stopArg >= 1"})
        protected final boolean doLargeInteger(final LargeIntegerObject receiver, final long start, final long stopArg) {
            final long stop = Math.min(stopArg, receiver.bitLength());
            if (start > stop) {
                startLargerThanStopProfile.enter();
                return BooleanObject.FALSE;
            }
            final byte[] bytes = receiver.getBytes();
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
                return BooleanObject.wrap(lastDigitNonZeroProfile.profile((bytes[lastDigitIndex - 1] << leftShift & 0xFF) != 0));
            }
        }

        private static byte digitOf(final long value, final long index) {
            return (byte) (value >> Byte.SIZE * index);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitAdd")
    protected abstract static class PrimDigitAddNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile differentSignProfile) {
            if (differentSignProfile.profile(differentSign(lhs, rhs))) {
                return Math.subtractExact(lhs, rhs);
            } else {
                return Math.addExact(lhs, rhs);
            }
        }

        @Specialization(replaces = "doLong")
        protected static final Object doLongWithOverflow(final long lhs, final long rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile differentSignProfile,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            if (differentSignProfile.profile(differentSign(lhs, rhs))) {
                return LargeIntegerObject.subtract(image, lhs, rhs);
            } else {
                return LargeIntegerObject.add(image, lhs, rhs);
            }
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile sameSignProfile) {
            if (sameSignProfile.profile(lhs.sameSign(rhs))) {
                return lhs.add(rhs);
            } else {
                return lhs.subtract(rhs);
            }
        }

        @Specialization
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile differentSignProfile) {
            if (differentSignProfile.profile(rhs.differentSign(lhs))) {
                return LargeIntegerObject.subtract(lhs, rhs);
            } else {
                return rhs.add(lhs);
            }
        }

        @Specialization
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile differentSignProfile) {
            if (differentSignProfile.profile(lhs.differentSign(rhs))) {
                return lhs.subtract(rhs);
            } else {
                return lhs.add(rhs);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitSubtract")
    protected abstract static class PrimDigitSubtractNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile differentSignProfile) {
            if (differentSignProfile.profile(differentSign(lhs, rhs))) {
                return Math.addExact(lhs, rhs);
            } else {
                return Math.subtractExact(lhs, rhs);
            }
        }

        @Specialization(replaces = "doLong")
        protected static final Object doLongWithOverflow(final long lhs, final long rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile differentSignProfile,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            if (differentSignProfile.profile(differentSign(lhs, rhs))) {
                return LargeIntegerObject.add(image, lhs, rhs);
            } else {
                return LargeIntegerObject.subtract(image, lhs, rhs);
            }
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile sameSignProfile) {
            if (sameSignProfile.profile(lhs.sameSign(rhs))) {
                return lhs.subtract(rhs);
            } else {
                return lhs.add(rhs);
            }
        }

        @Specialization
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile differentSignProfile) {
            if (differentSignProfile.profile(rhs.differentSign(lhs))) {
                return rhs.add(lhs);
            } else {
                return LargeIntegerObject.subtract(lhs, rhs);
            }
        }

        @Specialization
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile differentSignProfile) {
            if (differentSignProfile.profile(lhs.differentSign(rhs))) {
                return lhs.add(rhs);
            } else {
                return lhs.subtract(rhs);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitMultiplyNegative")
    protected abstract static class PrimDigitMultiplyNegativeNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs) {
            return Math.multiplyExact(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        protected static final Object doLongWithOverflow(final long lhs, final long rhs,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return LargeIntegerObject.multiply(image, lhs, rhs);
        }

        @Specialization
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return rhs.multiply(lhs);
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.multiply(rhs);
        }

        @Specialization
        protected static final double doDouble(final double lhs, final double rhs) {
            return lhs * rhs;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitBitAnd")
    protected abstract static class PrimDigitBitAndNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        @Specialization
        protected static final long doLong(final long receiver, final long arg) {
            return receiver & arg;
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final LargeIntegerObject arg) {
            return receiver.and(arg);
        }

        @Specialization
        protected static final Object doLong(final long receiver, final LargeIntegerObject arg,
                        @Cached("createBinaryProfile()") final ConditionProfile positiveProfile) {
            if (positiveProfile.profile(receiver >= 0)) {
                return receiver & arg.longValue();
            } else {
                return arg.and(receiver);
            }
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final long arg,
                        @Cached("createBinaryProfile()") final ConditionProfile positiveProfile) {
            if (positiveProfile.profile(arg >= 0)) {
                return receiver.longValue() & arg;
            } else {
                return receiver.and(arg);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitBitOr")
    protected abstract static class PrimDigitBitOrNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        @Specialization
        protected static final long bitOr(final long receiver, final long arg) {
            return receiver | arg;
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final LargeIntegerObject arg) {
            return receiver.or(arg);
        }

        @Specialization
        protected static final Object doLong(final long receiver, final LargeIntegerObject arg) {
            return arg.or(receiver);
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final long arg) {
            return receiver.or(arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitBitShiftMagnitude")
    public abstract static class PrimDigitBitShiftMagnitudeNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final long arg) {
            return receiver.shiftLeft((int) arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitBitXor")
    protected abstract static class PrimBitXorNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        @Specialization
        protected static final long doLong(final long lhs, final long rhs) {
            return lhs ^ rhs;
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.xor(rhs);
        }

        @Specialization
        protected static final Object doLong(final long lhs, final LargeIntegerObject rhs) {
            return rhs.xor(lhs);
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final long rhs) {
            return lhs.xor(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitCompare")
    protected abstract static class PrimDigitCompareNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        @Specialization
        protected static final long doLong(final long lhs, final long rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile smallerProfile,
                        @Cached("createBinaryProfile()") final ConditionProfile equalProfile) {
            if (smallerProfile.profile(lhs < rhs)) {
                return -1L;
            } else if (equalProfile.profile(lhs == rhs)) {
                return 0L;
            } else {
                return +1L;
            }
        }

        @Specialization
        protected static final long doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.compareTo(rhs);
        }

        @Specialization
        protected static final long doLongLargeInteger(final long lhs, final LargeIntegerObject rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile fitsIntoLongProfile) {
            if (fitsIntoLongProfile.profile(rhs.fitsIntoLong())) {
                final long value = rhs.longValue();
                return value == lhs ? 0L : value < lhs ? -1L : 1L;
            } else {
                return rhs.isNegative() ? 1L : -1L;
            }
        }

        @Specialization
        protected static final long doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile fitsIntoLongProfile) {
            if (fitsIntoLongProfile.profile(lhs.fitsIntoLong())) {
                final long value = lhs.longValue();
                return value == rhs ? 0L : value < rhs ? -1L : 1L;
            } else {
                return lhs.isNegative() ? -1L : 1L;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitDivNegative")
    protected abstract static class PrimDigitDivNegativeNode extends AbstractArithmeticPrimitiveNode implements TernaryPrimitive {
        @Specialization
        protected static final ArrayObject doLong(final long rcvr, final long arg, final boolean negative,
                        @Cached final BranchProfile signProfile,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            long divide = rcvr / arg;
            if (negative && divide >= 0 || !negative && divide < 0) {
                signProfile.enter();
                if (divide == Long.MIN_VALUE) {
                    return createArrayWithLongMinOverflowResult(image, rcvr, arg);
                }
                divide = -divide;
            }
            return image.asArrayOfLongs(divide, rcvr % arg);
        }

        @TruffleBoundary
        private static ArrayObject createArrayWithLongMinOverflowResult(final SqueakImageContext image, final long rcvr, final long arg) {
            return image.asArrayOfObjects(LargeIntegerObject.createLongMinOverflowResult(image), rcvr % arg);
        }

        @Specialization
        @TruffleBoundary
        protected static final ArrayObject doLargeInteger(final LargeIntegerObject rcvr, final LargeIntegerObject arg, final boolean negative,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final BigInteger[] divide = rcvr.getBigInteger().divideAndRemainder(arg.getBigInteger());
            final Object[] result = new Object[2];
            if (negative != divide[0].signum() < 0) {
                if (divide[0].bitLength() < Long.SIZE) {
                    final long lresult = divide[0].longValue();
                    if (lresult == Long.MIN_VALUE) {
                        result[0] = LargeIntegerObject.createLongMinOverflowResult(image);
                    } else {
                        result[0] = -lresult;
                    }
                } else {
                    result[0] = new LargeIntegerObject(image, divide[0].negate());
                }
            } else {
                if (divide[0].bitLength() < Long.SIZE) {
                    result[0] = divide[0].longValue();
                } else {
                    result[0] = new LargeIntegerObject(image, divide[0]);
                }
            }
            if (divide[1].bitLength() < Long.SIZE) {
                result[1] = divide[1].longValue();
            } else {
                result[1] = new LargeIntegerObject(image, divide[1]);
            }
            return image.asArrayOfObjects(result);
        }

        @Specialization
        protected static final ArrayObject doLongLargeInteger(final long rcvr, final LargeIntegerObject arg, @SuppressWarnings("unused") final boolean negative,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            assert !arg.fitsIntoLong() : "non-reduced large integer!";
            return image.asArrayOfLongs(0L, rcvr);
        }

        @Specialization
        @TruffleBoundary
        protected static final ArrayObject doLargeIntegerLong(final LargeIntegerObject rcvr, final long arg, final boolean negative,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final BigInteger[] divide = rcvr.getBigInteger().divideAndRemainder(BigInteger.valueOf(arg));
            final Object[] result = new Object[2];
            if (negative != divide[0].signum() < 0) {
                if (divide[0].bitLength() < Long.SIZE) {
                    final long lresult = divide[0].longValue();
                    if (lresult == Long.MIN_VALUE) {
                        result[0] = LargeIntegerObject.createLongMinOverflowResult(image);
                    } else {
                        result[0] = -lresult;
                    }
                } else {
                    result[0] = new LargeIntegerObject(image, divide[0].negate());
                }
            } else {
                if (divide[0].bitLength() < Long.SIZE) {
                    result[0] = divide[0].longValue();
                } else {
                    result[0] = new LargeIntegerObject(image, divide[0]);
                }
            }
            result[1] = divide[1].longValue();
            return image.asArrayOfObjects(result);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primGetModuleName")
    protected abstract static class PrimGetModuleNameNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final Object doGet(@SuppressWarnings("unused") final Object rcvr,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.asByteString(MODULE_NAME);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primMontgomeryDigitLength")
    protected abstract static class PrimMontgomeryDigitLengthNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final long doDigitLength(@SuppressWarnings("unused") final Object receiver) {
            return 32L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primMontgomeryTimesModulo")
    protected abstract static class PrimMontgomeryTimesModuloNode extends AbstractArithmeticPrimitiveNode implements QuaternaryPrimitive {
        /*
         * Optimized version of montgomeryTimesModulo for integer-sized arguments.
         */
        @Specialization(rewriteOn = {ArithmeticException.class})
        protected static final long doLongQuick(final long receiver, final long a, final long m, final long mInv) {
            if (!(fitsInOneWord(receiver) && fitsInOneWord(a) && fitsInOneWord(m))) {
                throw new ArithmeticException();
            }
            final long accum3 = receiver * a;
            final long u = accum3 * mInv & 0xFFFFFFFFL;
            final long accum2 = u * m;
            long accum = (accum2 & 0xFFFFFFFFL) + (accum3 & 0xFFFFFFFFL);
            accum = (accum >> 32) + (accum2 >> 32) + (accum3 >> 32);
            long result = accum & 0xFFFFFFFFL;
            if (!(accum >> 32 == 0 && result < m)) {
                result = result - m & 0xFFFFFFFFL;
            }
            return result;
        }

        private static boolean fitsInOneWord(final long value) {
            return value <= NativeObject.INTEGER_MAX;
        }

        @Specialization(replaces = "doLongQuick")
        @TruffleBoundary
        protected static final Object doLong(final long receiver, final long a, final long m, final long mInv,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doLargeInteger(image, toInts(receiver), toInts(a), toInts(m), mInv);
        }

        @Specialization
        @TruffleBoundary
        protected static final Object doLong(final long receiver, final LargeIntegerObject a, final long m, final long mInv,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doLargeInteger(image, toInts(receiver), toInts(a), toInts(m), mInv);
        }

        @Specialization
        @TruffleBoundary
        protected static final Object doLong(final long receiver, final long a, final LargeIntegerObject m, final long mInv,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doLargeInteger(image, toInts(receiver), toInts(a), toInts(m), mInv);
        }

        @Specialization
        @TruffleBoundary
        protected static final Object doLong(final long receiver, final LargeIntegerObject a, final LargeIntegerObject m, final long mInv,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doLargeInteger(image, toInts(receiver), toInts(a), toInts(m), mInv);
        }

        @Specialization
        @TruffleBoundary
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final long a, final long m, final long mInv,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doLargeInteger(image, toInts(receiver), toInts(a), toInts(m), mInv);
        }

        @Specialization
        @TruffleBoundary
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final LargeIntegerObject a, final long m, final long mInv,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doLargeInteger(image, toInts(receiver), toInts(a), toInts(m), mInv);
        }

        @Specialization
        @TruffleBoundary
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final long a, final LargeIntegerObject m, final long mInv,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doLargeInteger(image, toInts(receiver), toInts(a), toInts(m), mInv);
        }

        @Specialization
        @TruffleBoundary
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final LargeIntegerObject a, final LargeIntegerObject m, final LargeIntegerObject mInv,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doLargeInteger(image, toInts(receiver), toInts(a), toInts(m), mInv.longValueExact());
        }

        private static int[] toInts(final LargeIntegerObject value) {
            return UnsafeUtils.toIntsExact(value.getBytes());
        }

        private static int[] toInts(final long value) {
            if (fitsInOneWord(value)) {
                return new int[]{(int) value};
            } else {
                return new int[]{(int) value, (int) (value >> 32)};
            }
        }

        private static Object doLargeInteger(final SqueakImageContext image, final int[] firstInts, final int[] secondInts, final int[] thirdInts, final long mInv) {
            final int firstLen = firstInts.length;
            final int secondLen = secondInts.length;
            final int thirdLen = thirdInts.length;
            if (firstLen > thirdLen || secondLen > thirdLen) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            final int limit1 = firstLen - 1;
            final int limit2 = secondLen - 1;
            final int limit3 = thirdLen - 1;
            final int[] result = new int[thirdLen];

            long accum = 0;
            long accum2 = 0;
            long accum3 = 0;
            int lastDigit = 0;
            for (int i = 0; i <= limit1; i++) {
                accum3 = firstInts[i] & 0xFFFFFFFFL;
                accum3 = accum3 * (secondInts[0] & 0xFFFFFFFFL) + (result[0] & 0xFFFFFFFFL);
                final long u = accum3 * mInv & 0xFFFFFFFFL;
                accum2 = u * (thirdInts[0] & 0xFFFFFFFFL);
                accum = (accum2 & 0xFFFFFFFFL) + (accum3 & 0xFFFFFFFFL);
                accum = (accum >> 32) + (accum2 >> 32) + (accum3 >> 32 & 0xFFFFFFFFL);
                for (int k = 1; k <= limit2; k++) {
                    accum3 = firstInts[i] & 0xFFFFFFFFL;
                    accum3 = accum3 * (secondInts[k] & 0xFFFFFFFFL) + (result[k] & 0xFFFFFFFFL);
                    accum2 = u * (thirdInts[k] & 0xFFFFFFFFL);
                    accum = accum + (accum2 & 0xFFFFFFFFL) + (accum3 & 0xFFFFFFFFL);
                    result[k - 1] = (int) (accum & 0xFFFFFFFFL);
                    accum = (accum >> 32) + (accum2 >> 32) + (accum3 >> 32 & 0xFFFFFFFFL);
                }
                for (int k = secondLen; k <= limit3; k++) {
                    accum2 = u * (thirdInts[k] & 0xFFFFFFFFL);
                    accum = accum + (result[k] & 0xFFFFFFFFL) + (accum2 & 0xFFFFFFFFL);
                    result[k - 1] = (int) (accum & 0xFFFFFFFFL);
                    accum = (accum >> 32) + (accum2 >> 32) & 0xFFFFFFFFL;
                }
                accum += lastDigit;
                result[limit3] = (int) (accum & 0xFFFFFFFFL);
                lastDigit = (int) (accum >> 32);
            }
            for (int i = firstLen; i <= limit3; i++) {
                accum = result[0] & 0xFFFFFFFFL;
                final long u = accum * mInv & 0xFFFFFFFFL;
                accum += u * (thirdInts[0] & 0xFFFFFFFFL);
                accum = accum >> 32;
                for (int k = 1; k <= limit3; k++) {
                    accum2 = u * (thirdInts[k] & 0xFFFFFFFFL);
                    accum = accum + (result[k] & 0xFFFFFFFFL) + (accum2 & 0xFFFFFFFFL);
                    result[k - 1] = (int) (accum & 0xFFFFFFFFL);
                    accum = (accum >> 32) + (accum2 >> 32) & 0xFFFFFFFFL;
                }
                accum += lastDigit;
                result[limit3] = (int) (accum & 0xFFFFFFFFL);
                lastDigit = (int) (accum >> 32);
            }
            if (!(lastDigit == 0 && cDigitComparewithlen(thirdInts, result, thirdLen) == 1)) {
                accum = 0;
                for (int i = 0; i <= limit3; i++) {
                    accum = accum + result[i] - (thirdInts[i] & 0xFFFFFFFFL);
                    result[i] = (int) (accum & 0xFFFFFFFFL);
                    accum = 0 - (accum >> 63);
                }
            }
            final byte[] resultBytes = UnsafeUtils.toBytes(result);
            return new LargeIntegerObject(image, image.largePositiveIntegerClass, resultBytes).reduceIfPossible(); // normalize
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
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = {"primNormalizePositive", "primNormalizeNegative"})
    protected abstract static class PrimNormalizeNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject value) {
            return value.reduceIfPossible();
        }

        /**
         * Left to support LargeIntegerObjects adapted from NativeObjects (see
         * SecureHashAlgorithmTest>>testEmptyInput).
         */
        @TruffleBoundary
        @Specialization(guards = {"receiver.isByteType()", "receiver.getSqueakClass().isLargeIntegerClass()"})
        protected static final Object doNativeObject(final NativeObject receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return new LargeIntegerObject(image, receiver.getSqueakClass(), receiver.getByteStorage().clone()).reduceIfPossible();
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return LargeIntegersFactory.getFactories();
    }
}
