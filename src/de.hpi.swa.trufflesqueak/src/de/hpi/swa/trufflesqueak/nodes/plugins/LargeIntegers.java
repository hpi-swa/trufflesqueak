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
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
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
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.ReflectionUtils;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class LargeIntegers extends AbstractPrimitiveFactoryHolder {
    private static final Field BIG_INTEGER_MAG_FIELD = ReflectionUtils.lookupField(BigInteger.class, "mag");

    private static final BigInteger ONE_SHIFTED_BY_64 = BigInteger.ONE.shiftLeft(64);
    @CompilationFinal(dimensions = 1) public static final byte[] LONG_MIN_OVERFLOW_RESULT_BYTES = toByteArray(BigInteger.valueOf(Long.MIN_VALUE).abs());

    private static final ArithmeticException LARGE_INTEGER_OUT_OF_INT_RANGE = new ArithmeticException("Large integer out of int range");
    private static final ArithmeticException LARGE_INTEGER_OUT_OF_LONG_RANGE = new ArithmeticException("Large integer out of long range");
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

        @Specialization(guards = {"image.isLargeInteger(receiver)", "start >= 1", "stopArg >= 1"}, rewriteOn = {ArithmeticException.class})
        protected final boolean doLargeIntegerAsLong(final NativeObject receiver, final long start, final long stopArg,
                        @SuppressWarnings("unused") @Bind final SqueakImageContext image) {
            return doLong(longValueExact(receiver), start, stopArg);
        }

        @Specialization(guards = {"image.isLargeInteger(receiver)", "start >= 1", "stopArg >= 1"})
        protected final boolean doLargeInteger(final NativeObject receiver, final long start, final long stopArg,
                        @SuppressWarnings("unused") @Bind final SqueakImageContext image) {
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
                        @Cached final InlinedConditionProfile sameSignProfile) {
            return Math.addExact(lhs, rhsNegatedOnDifferentSign(lhs, rhs, sameSignProfile, node));
        }

        @Specialization(replaces = "doLong")
        protected static final Object doLongWithOverflow(final long lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return digitAdd(image, lhs, rhs);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return digitAdd(image, lhs, rhs);
        }

        @Specialization(guards = "image.isLargeInteger(rhs)")
        protected static final Object doLongLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return digitAdd(image, lhs, rhs);
        }

        @Specialization(guards = "image.isLargeInteger(lhs)")
        protected static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
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
            return digitSubtract(image, lhs, rhs);
        }

        @Specialization(guards = "image.isLargeInteger(rhs)")
        protected static final Object doLongLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return digitSubtract(image, lhs, rhs);
        }

        @Specialization(guards = "image.isLargeInteger(lhs)")
        protected static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return digitSubtract(image, lhs, rhs);
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
            return digitMultiplyNegative(image, lhs, rhs, neg);
        }

        @Specialization(guards = "image.isLargeInteger(rhs)")
        protected static final Object doLongLargeInteger(final long lhs, final NativeObject rhs, @SuppressWarnings("unused") final boolean neg,
                        @Bind final SqueakImageContext image) {
            return digitMultiplyNegative(image, lhs, rhs, neg);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs, final boolean neg,
                        @Bind final SqueakImageContext image) {
            return digitMultiplyNegative(image, lhs.getByteStorage(), rhs.getByteStorage(), neg);
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
        protected static final long doLong(final long receiver, final long rhs) {
            return receiver & rhs;
        }

        @Specialization(guards = {"image.isLargePositiveInteger(lhs)", "image.isLargePositiveInteger(rhs)"})
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return digitAnd(image, lhs.getByteStorage(), rhs.getByteStorage());
        }

        @Specialization(guards = {"lhs >= 0", "image.isLargePositiveInteger(rhs)"})
        protected static final Object doLong(final long lhs, final NativeObject rhs,
                        @SuppressWarnings("unused") @Bind final SqueakImageContext image) {
            return lhs & longValue(rhs);
        }

        @Specialization(guards = {"image.isLargePositiveInteger(lhs)", "rhs >= 0"})
        protected static final Object doLargeInteger(final NativeObject lhs, final long rhs,
                        @SuppressWarnings("unused") @Bind final SqueakImageContext image) {
            return longValue(lhs) & rhs;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitBitOr")
    protected abstract static class PrimDigitBitOrNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final long bitOr(final long lhs, final long rhs) {
            return lhs | rhs;
        }

        @Specialization(guards = {"image.isLargePositiveInteger(lhs)", "image.isLargePositiveInteger(rhs)"})
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return digitOr(image, lhs.getByteStorage(), rhs.getByteStorage());
        }

        @Specialization(guards = {"lhs >= 0", "image.isLargePositiveInteger(rhs)"})
        protected static final Object doLong(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return digitOr(image, rhs.getByteStorage(), lhs);
        }

        @Specialization(guards = {"image.isLargePositiveInteger(lhs)", "rhs >= 0"})
        protected static final Object doLargeInteger(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return digitOr(image, lhs.getByteStorage(), rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitBitShiftMagnitude")
    public abstract static class PrimDigitBitShiftMagnitudeNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "image.isLargeInteger(lhs)")
        protected static final Object doLargeInteger(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return digitBitShiftMagnitude(image, lhs, MiscUtils.toIntExact(rhs));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitBitXor")
    protected abstract static class PrimBitXorNode extends AbstractArithmeticPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final long doLong(final long lhs, final long rhs) {
            return lhs ^ rhs;
        }

        @Specialization(guards = {"image.isLargePositiveInteger(lhs)", "image.isLargePositiveInteger(rhs)"})
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return digitXor(image, lhs.getByteStorage(), rhs.getByteStorage());
        }

        @Specialization(guards = {"lhs >= 0", "image.isLargePositiveInteger(rhs)"})
        protected static final Object doLong(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return digitXor(image, rhs.getByteStorage(), lhs);
        }

        @Specialization(guards = {"image.isLargePositiveInteger(lhs)", "rhs >= 0"})
        protected static final Object doLargeInteger(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return digitXor(image, lhs.getByteStorage(), rhs);
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
                        @SuppressWarnings("unused") @Bind final SqueakImageContext image) {
            return digitCompare(lhs, rhs);
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
        @Specialization
        protected static final ArrayObject doLong(final long rcvr, final long arg, final boolean negative,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Shared("signProfile") @Cached final InlinedBranchProfile signProfile) {
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

        private static ArrayObject createArrayWithLongMinOverflowResult(final SqueakImageContext image, final long rcvr, final long arg) {
            return image.asArrayOfObjects(createLongMinOverflowResult(image), rcvr % arg);
        }

        @TruffleBoundary
        @Specialization(guards = {"image.isLargeInteger(rcvr)", "image.isLargeInteger(arg)"})
        protected static final ArrayObject doLargeInteger(final NativeObject rcvr, final NativeObject arg, final boolean negative,
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

        @Specialization(guards = "image.isLargeInteger(arg)")
        protected static final ArrayObject doLongLargeInteger(final long rcvr, final NativeObject arg, @SuppressWarnings("unused") final boolean negative,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Shared("signProfile") @Cached final InlinedBranchProfile signProfile) {
            if (LargeIntegers.fitsIntoLong(arg)) {
                return doLong(rcvr, LargeIntegers.longValue(arg), negative, image, node, signProfile);
            } else {
                return image.asArrayOfLongs(0L, rcvr);
            }
        }

        @TruffleBoundary
        @Specialization(guards = "image.isLargeInteger(rcvr)")
        protected static final ArrayObject doLargeIntegerLong(final NativeObject rcvr, final long arg, final boolean negative,
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
            return normalize(image, resultBytes, false);
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
    private static Object addLarge(final SqueakImageContext image, final long lhs, final long rhs) {
        final byte[] lhsBytes = createLargeFromSmallInteger(lhs);
        final byte[] rhsBytes = createLargeFromSmallInteger(rhs);
        final boolean neg = lhs < 0;
        if (sameSign(lhs, rhs)) {
            return digitAdd(image, lhsBytes, rhsBytes, neg);
        } else {
            return digitSubtract(image, lhsBytes, rhsBytes, neg);
        }
    }

    public static Object digitAdd(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return digitAdd(image, lhs.getByteStorage(), rhs.getByteStorage(), image.isLargeNegativeInteger(lhs));
    }

    public static Object digitAdd(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return digitAdd(image, lhs.getByteStorage(), createLargeFromSmallInteger(rhs), image.isLargeNegativeInteger(lhs));
    }

    public static Object digitAdd(final SqueakImageContext image, final long lhs, final NativeObject rhs) {
        return digitAdd(image, createLargeFromSmallInteger(lhs), rhs.getByteStorage(), lhs < 0);
    }

    public static Object digitAdd(final SqueakImageContext image, final long lhs, final long rhs) {
        final long x = lhs;
        final long y = sameSign(lhs, rhs) ? -rhs : rhs;
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
        final byte[] sum = new byte[sumLen];
        final int over = cDigitAddlenwithleninto(shortInt, shortDigitLen, longInt, longDigitLen, sum);
        if (over > 0) {
            final byte[] newSum = new byte[sumLen + 1];
            System.arraycopy(sum, 0, newSum, 0, sumLen);
            newSum[longDigitLen * 4] = (byte) over;
            return NativeObject.newNativeBytes(image, neg ? image.largeNegativeIntegerClass : image.largePositiveIntegerClass, newSum);
        } else {
            return normalize(image, sum, neg);
        }
    }

    private static int cDigitAddlenwithleninto(final byte[] shortInt, final int shortLen, final byte[] longInteger, final int longLen, final byte[] result) {
        long accum = 0;
        for (int i = 0; i < shortLen; i++) {
            accum = (((accum) >> 32) + (cDigitOfAt(shortInt, i) + (cDigitOfAt(longInteger, i))));
            cDigitOfAtPut(result, i, (int) (accum & 0xFFFFFFFFL));
        }
        for (int i = shortLen; i < longLen; i++) {
            accum = ((accum) >> 32) + cDigitOfAt(longInteger, i);
            cDigitOfAtPut(result, i, (int) (accum & 0xFFFFFFFFL));
        }
        return (int) (accum >> 32 & 0xFFFFFFFFL);
    }

    public static Object digitSubtract(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return digitSubtract(image, lhs.getByteStorage(), createLargeFromSmallInteger(rhs), image.isLargeNegativeInteger(lhs));
    }

    public static Object digitSubtract(final SqueakImageContext image, final long lhs, final NativeObject rhs) {
        return digitSubtract(image, createLargeFromSmallInteger(lhs), rhs.getByteStorage(), lhs < 0);
    }

    public static Object digitSubtract(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhsBytes) {
        return digitSubtract(image, lhs.getByteStorage(), rhsBytes.getByteStorage(), image.isLargeNegativeInteger(lhs));
    }

    public static Object digitSubtract(final SqueakImageContext image, final byte[] lhsBytes, final byte[] rhsBytes, final boolean lhsNeg) {
        /* begin digitSubLarge:with: */
        int firstDigitLen = (lhsBytes.length + 3) / 4;
        int secondDigitLen = (rhsBytes.length + 3) / 4;
        if (firstDigitLen == secondDigitLen) {
            while ((firstDigitLen > 1) && cDigitOfAt(lhsBytes, firstDigitLen - 1) == cDigitOfAt(rhsBytes, firstDigitLen - 1)) {
                firstDigitLen--;
            }
            secondDigitLen = firstDigitLen;
        }
        final byte[] larger;
        final int largeDigitLen;
        final byte[] smaller;
        final int smallerDigitLen;
        final boolean neg;
        if ((firstDigitLen < secondDigitLen) || ((firstDigitLen == secondDigitLen) && cDigitOfAt(lhsBytes, firstDigitLen - 1) < cDigitOfAt(rhsBytes, firstDigitLen - 1))) {
            larger = rhsBytes;
            largeDigitLen = secondDigitLen;
            smaller = lhsBytes;
            smallerDigitLen = firstDigitLen;
            neg = !lhsNeg;
        } else {
            larger = lhsBytes;
            largeDigitLen = firstDigitLen;
            smaller = rhsBytes;
            smallerDigitLen = secondDigitLen;
            neg = lhsNeg;
        }
        final byte[] res = new byte[largeDigitLen * 4];
        /* begin cDigitSub:len:with:len:into: */
        long z = 0;
        for (int i = 0; i < smallerDigitLen; i++) {
            z = z + (cDigitOfAt(larger, i) - cDigitOfAt(smaller, i));
            cDigitOfAtPut(res, i, z & 0xFFFFFFFFL);
            z = (z >> 0x3F);
        }
        for (int i = smallerDigitLen; i < largeDigitLen; i++) {
            z += cDigitOfAt(larger, i);
            cDigitOfAtPut(res, i, z & 0xFFFFFFFFL);
            z = (z >> 0x3F);
        }
        return normalize(image, res, neg);
        /* end digitSubLarge:with: */
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
    private static Object subtractLarge(final SqueakImageContext image, final long lhs, final long rhs) {
        final byte[] lhsBytes = createLargeFromSmallInteger(lhs);
        final byte[] rhsBytes = createLargeFromSmallInteger(rhs);
        final boolean neg = lhs < 0;
        if (sameSign(lhs, rhs)) {
            return digitSubtract(image, lhsBytes, rhsBytes, neg);
        } else {
            return digitAdd(image, lhsBytes, rhsBytes, neg);
        }
    }

    public static Object digitMultiplyNegative(final SqueakImageContext image, final long lhs, final long rhs, final boolean neg) {
        /* Inlined version of Math.multiplyExact(x, y) with large integer fallback. */
        final long result = lhs * rhs;
        final long ax = Math.abs(lhs);
        final long ay = Math.abs(rhs);
        if ((ax | ay) >>> 31 != 0) {
            // Some bits greater than 2^31 that might cause overflow
            // Check the result using the divide operator
            // and check for the special case of Long.MIN_VALUE * -1
            if (rhs != 0 && result / rhs != lhs || lhs == Long.MIN_VALUE && rhs == -1) {
                return digitMultiplyNegativeLarge(image, lhs, rhs, neg);
            }
        }
        return result;
    }

    public static Object digitMultiplyNegativeLarge(final SqueakImageContext image, final long lhs, final long rhs, final boolean neg) {
        return digitMultiplyNegative(image, createLargeFromSmallInteger(lhs), createLargeFromSmallInteger(rhs), neg);
    }

    public static Object digitMultiplyNegative(final SqueakImageContext image, final NativeObject lhs, final long rhs, final boolean neg) {
        return digitMultiplyNegative(image, lhs.getByteStorage(), createLargeFromSmallInteger(rhs), neg);
    }

    public static Object digitMultiplyNegative(final SqueakImageContext image, final long lhs, final NativeObject rhs, final boolean neg) {
        return digitMultiplyNegative(image, createLargeFromSmallInteger(lhs), rhs.getByteStorage(), neg);
    }

    public static Object digitMultiplyNegative(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhsBytes, final boolean neg) {
        return digitMultiplyNegative(image, lhs.getByteStorage(), rhsBytes.getByteStorage(), neg);
    }

    public static Object digitMultiplyNegative(final SqueakImageContext image, final byte[] lhsBytes, final byte[] rhsBytes, final boolean neg) {
        /* begin digitMultiplyLarge:with:negative: */
        final int firstLen = lhsBytes.length;
        final int secondLen = rhsBytes.length;
        final byte[] shortInt;
        final int shortLen;
        final byte[] longInt;
        final int longLen;

        if (firstLen <= secondLen) {
            shortInt = lhsBytes;
            shortLen = firstLen;
            longInt = rhsBytes;
            longLen = secondLen;
        } else {
            shortInt = rhsBytes;
            shortLen = secondLen;
            longInt = lhsBytes;
            longLen = firstLen;
        }
        final byte[] prod = new byte[longLen + shortLen];

        /* begin cDigitMultiply:len:with:len:into:len: */
        if ((((shortLen + 3) / 4) == 1) && cDigitOfAt(shortInt, 0) == 0) {
            return 0L;
        }
        if ((((longLen + 3) / 4) == 1) && cDigitOfAt(longInt, 0) == 0) {
            return 0L;
        }

        /* prod starts out all zero */
        final int limitShort = ((shortLen + 3) / 4) - 1;
        final int limitLong = ((longLen + 3) / 4) - 1;
        for (int i = 0; i <= limitShort; i++) {
            final long digit = cDigitOfAt(shortInt, i);
            if (digit != 0) {
                int k = i;
                long carry = 0;
                /*
                 * Loop invariant: 0<=carry<=16rFFFFFFFF, k=i+j-1 (ST) -> Loop invariant:
                 * 0<=carry<=16rFFFFFFFF, k=i+j (C) (?)
                 */
                for (int j = 0; j <= limitLong; j++) {
                    long ab = cDigitOfAt(longInt, j);
                    ab = ((ab * digit) + carry) + cDigitOfAt(prod, k);
                    carry = (ab >> 32) & 0xFFFFFFFFL;
                    cDigitOfAtPut(prod, k, ab & 0xFFFFFFFFL);
                    k++;
                }
                if (k < (((longLen + shortLen) + 3) / 4)) {
                    cDigitOfAtPut(prod, k, carry);
                }
            }
        }
        /* end cDigitMultiply:len:with:len:into:len: */
        return normalize(image, prod, neg);
        /* end digitMultiplyLarge:with:negative: */
    }

    public static Object multiply(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return digitMultiplyNegative(image, lhs, rhs, !sameSign(lhs, rhs));
    }

    @TruffleBoundary
    public static Object multiply(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        if (rhs == 0) {
            return 0L;
        }
        return digitMultiplyNegative(image, lhs, rhs, !sameSign(image, lhs, rhs));
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
        return digitMultiplyNegative(image, lhs, rhs, !sameSign(lhs, rhs));
    }

    @TruffleBoundary
    public static Object divide(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return normalize(image, toBigInteger(image, lhs).divide(toBigInteger(image, rhs)));
    }

    @TruffleBoundary
    public static Object divide(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return normalize(image, toBigInteger(image, lhs).divide(BigInteger.valueOf(rhs)));
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
        return toBigInteger(image, lhs).remainder(BigInteger.valueOf(rhs)).longValue();
    }

    @TruffleBoundary
    public static Object remainder(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return normalize(image, toBigInteger(image, lhs).remainder(toBigInteger(image, rhs)));
    }

    /*
     * Comparison
     */

    public static int digitCompare(final NativeObject lhs, final NativeObject rhs) {
        return digitCompare(lhs.getByteStorage(), rhs.getByteStorage());
    }

    private static int digitCompare(final byte[] lhsBytes, final byte[] rhsBytes) {
        final int firstDigitLen = (lhsBytes.length + 3) / 4;
        final int secondDigitLen = (rhsBytes.length + 3) / 4;
        if (secondDigitLen != firstDigitLen) {
            if (secondDigitLen > firstDigitLen) {
                return -1;
            } else {
                return 1;
            }
        }
        return cDigitComparewithlen(lhsBytes, rhsBytes, firstDigitLen);
    }

    private static int cDigitComparewithlen(final byte[] first, final byte[] second, final int len) {
        long firstDigit;
        long secondDigit;
        int index = len - 1;
        while (index >= 0) {
            if ((secondDigit = cDigitOfAt(second, index)) != (firstDigit = cDigitOfAt(first, index))) {
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

    public static Object digitAnd(final SqueakImageContext image, final byte[] lhsBytes, final byte[] rhsBytes) {
        final int firstLen = lhsBytes.length;
        final int secondLen = rhsBytes.length;
        final int shortLen;
        final byte[] shortLarge;
        final int longLen;
        final byte[] longLarge;
        if (firstLen < secondLen) {
            shortLen = firstLen;
            shortLarge = lhsBytes;
            longLen = secondLen;
            longLarge = rhsBytes;
        } else {
            shortLen = secondLen;
            shortLarge = rhsBytes;
            longLen = firstLen;
            longLarge = lhsBytes;
        }
        final byte[] result = new byte[longLen];
        final int shortLimit = ((shortLen + 3) / 4) - 1;
        for (int i = 0; i <= shortLimit; i++) {
            cDigitOfAtPut(result, i, cDigitOfAt(shortLarge, i) & cDigitOfAt(longLarge, i));
        }
        /* array zero-initialized */
        return normalize(image, result, false);
    }

    public static Object digitOr(final SqueakImageContext image, final byte[] lhsBytes, final long rhs) {
        return digitOr(image, lhsBytes, createLargeFromSmallInteger(rhs));
    }

    public static Object digitOr(final SqueakImageContext image, final byte[] lhsBytes, final byte[] rhsBytes) {
        final int firstLen = lhsBytes.length;
        final int secondLen = rhsBytes.length;
        final int shortLen;
        final byte[] shortLarge;
        final int longLen;
        final byte[] longLarge;
        if (firstLen < secondLen) {
            shortLen = firstLen;
            shortLarge = lhsBytes;
            longLen = secondLen;
            longLarge = rhsBytes;
        } else {
            shortLen = secondLen;
            shortLarge = rhsBytes;
            longLen = firstLen;
            longLarge = lhsBytes;
        }
        final byte[] result = new byte[longLen];
        final int shortLimit = ((shortLen + 3) / 4) - 1;
        for (int i = 0; i <= shortLimit; i++) {
            cDigitOfAtPut(result, i, cDigitOfAt(shortLarge, i) | cDigitOfAt(longLarge, i));
        }
        final int longLimit = ((longLen + 3) / 4) - 1;
        for (int i = ((shortLen + 3) / 4); i <= longLimit; i++) {
            cDigitOfAtPut(result, i, cDigitOfAt(longLarge, i));
        }
        return normalize(image, result, false);
    }

    public static Object digitXor(final SqueakImageContext image, final byte[] lhsBytes, final long rhs) {
        return digitXor(image, lhsBytes, createLargeFromSmallInteger(rhs));
    }

    public static Object digitXor(final SqueakImageContext image, final byte[] lhsBytes, final byte[] rhsBytes) {
        final int firstLen = lhsBytes.length;
        final int secondLen = rhsBytes.length;
        final int shortLen;
        final byte[] shortLarge;
        final int longLen;
        final byte[] longLarge;
        if (firstLen < secondLen) {
            shortLen = firstLen;
            shortLarge = lhsBytes;
            longLen = secondLen;
            longLarge = rhsBytes;
        } else {
            shortLen = secondLen;
            shortLarge = rhsBytes;
            longLen = firstLen;
            longLarge = lhsBytes;
        }
        final byte[] result = new byte[longLen];
        final int shortLimit = ((shortLen + 3) / 4) - 1;
        for (int i = 0; i <= shortLimit; i++) {
            cDigitOfAtPut(result, i, cDigitOfAt(shortLarge, i) ^ cDigitOfAt(longLarge, i));
        }
        final int longLimit = ((longLen + 3) / 4) - 1;
        for (int i = ((shortLen + 3) / 4); i <= longLimit; i++) {
            cDigitOfAtPut(result, i, cDigitOfAt(longLarge, i));
        }
        return normalize(image, result, false);
    }

    public static Object digitBitShiftMagnitude(final SqueakImageContext image, final NativeObject value, final int shiftCount) {
        final byte[] valueBytes = value.getByteStorage();
        final boolean neg = image.isLargeNegativeInteger(value);
        /* convert it to a not normalized LargeInteger */
        if (shiftCount >= 0) {
            return NativeObject.newNativeBytes(image, neg ? image.largeNegativeIntegerClass : image.largePositiveIntegerClass, digitLshift(valueBytes, shiftCount));
        }
        final int rShift = -shiftCount;
        final byte[] aLargeInteger = digitRshiftlookfirst(valueBytes, rShift, (valueBytes.length + 3) / 4);
        return normalize(image, aLargeInteger, neg);
    }

    private static byte[] digitLshift(final byte[] valueBytes, final int shiftCount) {
        final int oldDigitLen = (valueBytes.length + 3) / 4;
        final int highBit = cDigitHighBitLen(valueBytes, oldDigitLen);
        if (highBit == 0) {
            return new byte[1];
        }
        final int newByteLen = ((highBit + shiftCount) + 7) / 8;
        final byte[] newBytes = new byte[newByteLen];
        final int newDigitLen = (newByteLen + 3) / 4;

        /* begin cDigitLshift:from:len:to:len: */
        final int digitShift = shiftCount / 32;
        final int bitShift = shiftCount % 32;
        // final int limit = digitShift - 1;

        /* Note: 0 is endian neutral, use direct access */
        /* array zero-initialized */
        if (bitShift == 0) {
            /* begin cDigitReplace:from:to:with:startingAt: */
            /* begin cDigitCopyFrom:to:len: */
            for (int i = 0; i < (((newDigitLen - 1) - digitShift) + 1); i++) {
                cDigitOfAtPut(newBytes, digitShift + i, cDigitOfAt(valueBytes, i));
            }
            return newBytes;
        }

        /*
         * Fast version for digit-aligned shifts C indexed! This implementation use at most 31 bits
         * of carry. bitAnd: 16rFFFFFFFF is only for simulator, useless in C
         */
        final int rshift = 32 - bitShift;
        long carry = 0;
        final int limitOld = oldDigitLen - 1;
        for (int i = 0; i <= limitOld; i++) {
            final long digit = cDigitOfAt(valueBytes, i);
            cDigitOfAtPut(newBytes, i + digitShift, (carry | (digit << bitShift)) & 0xFFFFFFFFL);
            carry = digit >> rshift;
        }
        if (carry != 0) {
            cDigitOfAtPut(newBytes, newDigitLen - 1, carry);
        }
        /* end cDigitLshift:from:len:to:len: */
        return newBytes;
    }

    private static byte[] digitRshiftlookfirst(final byte[] valueBytes, final int shiftCount, final int a) {
        final int oldBitLen = cDigitHighBitLen(valueBytes, a);
        final int oldDigitLen = (oldBitLen + 0x1F) / 32;
        final int newBitLen = oldBitLen - shiftCount;
        if (newBitLen <= 0) {
            return new byte[0];
        }
        /* All bits lost */
        final int newByteLen = (newBitLen + 7) / 8;
        final int newDigitLen = (newByteLen + 3) / 4;

        final byte[] newBytes = new byte[newByteLen];
        /* begin cDigitRshift:from:len:to:len: */
        final int digitShift = shiftCount / 32;
        final int bitShift = shiftCount % 32;
        if (bitShift == 0) {
            /* begin cDigitReplace:from:to:with:startingAt: */
            /* begin cDigitCopyFrom:to:len: */
            for (int i = 0; i < (((newDigitLen - 1)) + 1); i++) {
                cDigitOfAtPut(newBytes, i, cDigitOfAt(valueBytes, digitShift + i));
            }
            return newBytes;
        }

        /*
         * Fast version for digit-aligned shifts C indexed! This implementation use at most 31 bits
         * of carry. bitAnd: 16rFFFFFFFF is only for simulator, useless in C
         */
        final int leftShift = 32 - bitShift;
        long carry = cDigitOfAt(valueBytes, digitShift) >> bitShift;
        final int start = digitShift + 1;
        final int limit = oldDigitLen - 1;
        for (int j = start; j <= limit; j++) {
            final long digit = cDigitOfAt(valueBytes, j);
            cDigitOfAtPut(newBytes, j - start, (carry | (digit << leftShift)) & 0xFFFFFFFFL);
            carry = digit >> bitShift;
        }
        if (carry != 0) {
            cDigitOfAtPut(newBytes, newDigitLen - 1, carry);
        }
        /* end cDigitRshift:from:len:to:len: */
        return newBytes;
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

    public static boolean sameSign(final SqueakImageContext image, final NativeObject lhs, final long rhs) {
        return isPositive(image, lhs) == rhs >= 0;
    }

    public static boolean sameSign(final long lhs, final long rhs) {
        return (lhs ^ rhs) >= 0;
    }

    public static long rhsNegatedOnDifferentSign(final long lhs, final long rhs, final InlinedConditionProfile sameSignProfile, final Node node) {
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

    @TruffleBoundary
    public static boolean isIntegralWhenDividedBy(final SqueakImageContext image, final NativeObject lhs, final NativeObject rhs) {
        return toBigInteger(image, lhs).remainder(toBigInteger(image, rhs)).signum() == 0;
    }

    @TruffleBoundary
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
    public static NativeObject createLongMinOverflowResult(final SqueakImageContext image) {
        return NativeObject.newNativeBytes(image, image.largePositiveIntegerClass, LONG_MIN_OVERFLOW_RESULT_BYTES.clone());
    }

    /* Utilities */

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
     * BigInteger conversion
     */

    @TruffleBoundary
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
        return NativeObject.newNativeBytes(image, squeakClass, result);
    }

    /**
     * Reads a 32-bit word from the byte array in **little-endian** order. Word index 0 is the least
     * significant 4 bytes.
     */
    private static long cDigitOfAt(final byte[] bytes, final int wordIndex) {
        final int base = wordIndex * 4;
        final int length = bytes.length;
        final long b0 = (base + 0 < length) ? (bytes[base + 0] & 0xFFL) : 0;
        final long b1 = (base + 1 < length) ? (bytes[base + 1] & 0xFFL) : 0;
        final long b2 = (base + 2 < length) ? (bytes[base + 2] & 0xFFL) : 0;
        final long b3 = (base + 3 < length) ? (bytes[base + 3] & 0xFFL) : 0;
        return (b0 | (b1 << 8) | (b2 << 16) | (b3 << 24)) & 0xFFFFFFFFL;
    }

    static void cDigitOfAtPut(final byte[] bytes, final int wordIndex, final long value) {
        final int base = wordIndex * 4;
        final int length = bytes.length;
        bytes[base + 0] = (byte) value;
        if (base + 1 < length) {
            bytes[base + 1] = (byte) (value >> 8);
            if (base + 2 < length) {
                bytes[base + 2] = (byte) (value >> 16);
                if (base + 3 < length) {
                    bytes[base + 3] = (byte) (value >> 24);
                }
            }
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
        if (fitsIntoInt(value)) {
            return intValue(image, value);
        } else {
            throw LARGE_INTEGER_OUT_OF_INT_RANGE;
        }
    }

    public static long longValue(final NativeObject value) {
        final byte[] bytes = value.getByteStorage();
        assert bytes.length > 0 : "Fixme? 0L : -0L?";
        final int digitLen = (bytes.length + 3) / 4;
        long result = cDigitOfAt(bytes, 0);
        if (digitLen > 1) {
            result += (cDigitOfAt(bytes, 1) << 32);
        }
        return result;
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
        assert isPositive(SqueakImageContext.getSlow(), value) && bitLength <= Long.SIZE;
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
        return new BigInteger(image.largePositiveIntegerClass == value.getSqueakClass() ? 1 : -1, ArrayUtils.swapOrderCopy(value.getByteStorage()));
    }

    public static NativeObject toNativeObject(final SqueakImageContext image, final BigInteger result) {
        return NativeObject.newNativeBytes(image, result.signum() >= 0 ? image.largePositiveIntegerClass : image.largeNegativeIntegerClass, toByteArray(result));
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
