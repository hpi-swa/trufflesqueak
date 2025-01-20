/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.math.BigInteger;
import java.util.List;

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
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.AbstractArithmeticPrimitiveNode;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class LargeIntegers extends AbstractPrimitiveFactoryHolder {
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
                        @Shared("differentSignProfile") @Cached final InlinedConditionProfile differentSignProfile) {
            return Math.addExact(lhs, rhsNegatedOnDifferentSign(lhs, rhs, differentSignProfile, node));
        }

        @Specialization(replaces = "doLong")
        protected final Object doLongWithOverflow(final long lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("differentSignProfile") @Cached final InlinedConditionProfile differentSignProfile) {
            return LargeIntegerObject.add(getContext(), lhs, rhsNegatedOnDifferentSign(lhs, rhs, differentSignProfile, node));
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs,
                        @Bind final Node node,
                        @Shared("differentSignProfile") @Cached final InlinedConditionProfile differentSignProfile) {
            if (differentSignProfile.profile(node, !lhs.sameSign(rhs))) {
                return lhs.subtract(rhs);
            } else {
                return lhs.add(rhs);
            }
        }

        @Specialization
        protected final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs,
                        @Bind final Node node,
                        @Shared("differentSignProfile") @Cached final InlinedConditionProfile differentSignProfile) {
            if (differentSignProfile.profile(node, rhs.differentSign(getContext(), lhs))) {
                return LargeIntegerObject.subtract(lhs, rhs);
            } else {
                return rhs.add(lhs);
            }
        }

        @Specialization
        protected final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("differentSignProfile") @Cached final InlinedConditionProfile differentSignProfile) {
            if (differentSignProfile.profile(node, lhs.differentSign(getContext(), rhs))) {
                return lhs.subtract(rhs);
            } else {
                return lhs.add(rhs);
            }
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
            return LargeIntegerObject.subtract(getContext(), lhs, rhsNegatedOnDifferentSign(lhs, rhs, differentSignProfile, node));
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs,
                        @Bind final Node node,
                        @Shared("differentSignProfile") @Cached final InlinedConditionProfile differentSignProfile) {
            if (differentSignProfile.profile(node, !lhs.sameSign(rhs))) {
                return lhs.add(rhs);
            } else {
                return lhs.subtract(rhs);
            }
        }

        @Specialization
        protected final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs,
                        @Bind final Node node,
                        @Shared("differentSignProfile") @Cached final InlinedConditionProfile differentSignProfile) {
            if (differentSignProfile.profile(node, rhs.differentSign(getContext(), lhs))) {
                return rhs.add(lhs);
            } else {
                return LargeIntegerObject.subtract(lhs, rhs);
            }
        }

        @Specialization
        protected final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("differentSignProfile") @Cached final InlinedConditionProfile differentSignProfile) {
            if (differentSignProfile.profile(node, lhs.differentSign(getContext(), rhs))) {
                return lhs.add(rhs);
            } else {
                return lhs.subtract(rhs);
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
            return LargeIntegerObject.multiply(getContext(), lhs, rhs);
        }

        @Specialization
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs, @SuppressWarnings("unused") final boolean neg) {
            return rhs.multiply(lhs);
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs, @SuppressWarnings("unused") final boolean neg) {
            return lhs.multiply(rhs);
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
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final LargeIntegerObject arg) {
            return receiver.and(arg);
        }

        @Specialization
        protected static final Object doLong(final long receiver, final LargeIntegerObject arg,
                        @Bind final Node node,
                        @Shared("positiveProfile") @Cached final InlinedConditionProfile positiveProfile) {
            if (positiveProfile.profile(node, receiver >= 0)) {
                return receiver & arg.longValue();
            } else {
                return arg.and(receiver);
            }
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final long arg,
                        @Bind final Node node,
                        @Shared("positiveProfile") @Cached final InlinedConditionProfile positiveProfile) {
            if (positiveProfile.profile(node, arg >= 0)) {
                return receiver.longValue() & arg;
            } else {
                return receiver.and(arg);
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
    public abstract static class PrimDigitBitShiftMagnitudeNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final long arg) {
            return receiver.shiftLeft((int) arg);
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
        protected static final long doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.compareTo(rhs);
        }

        @Specialization
        protected final long doLongLargeInteger(final long lhs, final LargeIntegerObject rhs,
                        @Bind final Node node,
                        @Shared("fitsIntoLongProfile") @Cached final InlinedConditionProfile fitsIntoLongProfile) {
            if (fitsIntoLongProfile.profile(node, rhs.fitsIntoLong())) {
                final long value = rhs.longValue();
                return value == lhs ? 0L : value < lhs ? -1L : 1L;
            } else {
                return rhs.isNegative(getContext()) ? 1L : -1L;
            }
        }

        @Specialization
        protected final long doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("fitsIntoLongProfile") @Cached final InlinedConditionProfile fitsIntoLongProfile) {
            if (fitsIntoLongProfile.profile(node, lhs.fitsIntoLong())) {
                final long value = lhs.longValue();
                return value == rhs ? 0L : value < rhs ? -1L : 1L;
            } else {
                return lhs.isNegative(getContext()) ? -1L : 1L;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitDivNegative")
    protected abstract static class PrimDigitDivNegativeNode extends AbstractArithmeticPrimitiveNode implements Primitive2WithFallback {
        @Specialization
        protected final ArrayObject doLong(final long rcvr, final long arg, final boolean negative,
                        @Bind final Node node,
                        @Cached final InlinedBranchProfile signProfile) {
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
            return image.asArrayOfObjects(LargeIntegerObject.createLongMinOverflowResult(image), rcvr % arg);
        }

        @Specialization
        @TruffleBoundary
        protected final ArrayObject doLargeInteger(final LargeIntegerObject rcvr, final LargeIntegerObject arg, final boolean negative) {
            final SqueakImageContext image = getContext();
            final BigInteger[] divide = rcvr.getBigInteger().divideAndRemainder(arg.getBigInteger());
            final Object[] result = digitDivNegative(negative, image, divide);
            if (divide[1].bitLength() < Long.SIZE) {
                result[1] = divide[1].longValue();
            } else {
                result[1] = new LargeIntegerObject(image, divide[1]);
            }
            return image.asArrayOfObjects(result);
        }

        @Specialization
        protected final ArrayObject doLongLargeInteger(final long rcvr, final LargeIntegerObject arg, @SuppressWarnings("unused") final boolean negative) {
            assert !arg.fitsIntoLong() : "non-reduced large integer!";
            return getContext().asArrayOfLongs(0L, rcvr);
        }

        @Specialization
        @TruffleBoundary
        protected final ArrayObject doLargeIntegerLong(final LargeIntegerObject rcvr, final long arg, final boolean negative) {
            final SqueakImageContext image = getContext();
            final BigInteger bigInteger = BigInteger.valueOf(arg);
            final BigInteger[] divide = rcvr.getBigInteger().divideAndRemainder(bigInteger);
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
            if (firstLen > thirdLen || secondLen > thirdLen) {
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

        @GenerateInline
        @GenerateCached(false)
        protected abstract static class ToIntsNode extends AbstractNode {
            protected abstract int[] execute(Node inliningTarget, Object value);

            @Specialization
            protected static final int[] doLargeInteger(final LargeIntegerObject value) {
                return UnsafeUtils.toIntsExact(value.getBytes());
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
            protected static final long doLargeInteger(final LargeIntegerObject value) {
                return value.longValueExact();
            }

            @Fallback
            protected static final long doFallback(@SuppressWarnings("unused") final Object value) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = {"primNormalizePositive", "primNormalizeNegative"})
    protected abstract static class PrimNormalizeNode extends AbstractArithmeticPrimitiveNode implements Primitive0WithFallback {

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject value) {
            return value.reduceIfPossible();
        }

        /**
         * Left to support LargeIntegerObjects adapted from NativeObjects (see
         * SecureHashAlgorithmTest>>testEmptyInput).
         */
        @Specialization(guards = {"receiver.isByteType()", "getContext().isLargeIntegerClass(receiver.getSqueakClass())"})
        protected final Object doNativeObject(final NativeObject receiver) {
            return new LargeIntegerObject(getContext(), receiver.getSqueakClass(), receiver.getByteStorage().clone()).reduceIfPossible();
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return LargeIntegersFactory.getFactories();
    }
}
