/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins;

import java.math.BigInteger;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BooleanObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ArithmeticPrimitives.AbstractArithmeticPrimitiveNode;
import de.hpi.swa.graal.squeak.util.ArrayConversionUtils;

public final class LargeIntegers extends AbstractPrimitiveFactoryHolder {
    private static final String MODULE_NAME = "LargeIntegers v2.0 (GraalSqueak)";

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primAnyBitFromTo")
    protected abstract static class PrimAnyBitFromToNode extends AbstractArithmeticPrimitiveNode implements TernaryPrimitive {
        private final BranchProfile startLargerThanStopProfile = BranchProfile.create();
        private final BranchProfile firstAndLastDigitIndexIdenticalProfile = BranchProfile.create();

        protected PrimAnyBitFromToNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"start >= 1", "stopArg >= 1"})
        protected final boolean doLong(final long receiver, final long start, final long stopArg) {
            final long stop = Math.min(stopArg, Long.highestOneBit(receiver));
            if (start > stop) {
                startLargerThanStopProfile.enter();
                return BooleanObject.FALSE;
            }
            final long firstDigitIndex = Math.floorDiv(start - 1, 32);
            final long lastDigitIndex = Math.floorDiv(stop - 1, 32);
            final long firstMask = 0xFFFFFFFFL << (start - 1 & 31);
            final long lastMask = 0xFFFFFFFFL >> 31 - (stop - 1 & 31);
            if (firstDigitIndex == lastDigitIndex) {
                firstAndLastDigitIndexIdenticalProfile.enter();
                final byte digit = digitOf(receiver, firstDigitIndex);
                if ((digit & firstMask & lastMask) != 0) {
                    return BooleanObject.TRUE;
                } else {
                    return BooleanObject.FALSE;
                }
            }
            if ((digitOf(receiver, firstDigitIndex) & firstMask) != 0) {
                return BooleanObject.TRUE;
            }
            for (long i = firstDigitIndex + 1; i < lastDigitIndex - 1; i++) {
                if (digitOf(receiver, i) != 0) {
                    return BooleanObject.TRUE;
                }
            }
            if ((digitOf(receiver, lastDigitIndex) & lastMask) != 0) {
                return BooleanObject.TRUE;
            } else {
                return BooleanObject.FALSE;
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
            final long firstDigitIndex = Math.floorDiv(start - 1, 32);
            final long lastDigitIndex = Math.floorDiv(stop - 1, 32);
            final long firstMask = 0xFFFFFFFFL << (start - 1 & 31);
            final long lastMask = 0xFFFFFFFFL >> 31 - (stop - 1 & 31);
            if (firstDigitIndex == lastDigitIndex) {
                firstAndLastDigitIndexIdenticalProfile.enter();
                final long digit = receiver.getNativeAt0(firstDigitIndex);
                if ((digit & firstMask & lastMask) != 0) {
                    return BooleanObject.TRUE;
                } else {
                    return BooleanObject.FALSE;
                }
            }
            if ((receiver.getNativeAt0(firstDigitIndex) & firstMask) != 0) {
                return BooleanObject.TRUE;
            }
            for (long i = firstDigitIndex + 1; i < lastDigitIndex - 1; i++) {
                if (receiver.getNativeAt0(i) != 0) {
                    return BooleanObject.TRUE;
                }
            }
            if ((receiver.getNativeAt0(lastDigitIndex) & lastMask) != 0) {
                return BooleanObject.TRUE;
            } else {
                return BooleanObject.FALSE;
            }
        }

        private static byte digitOf(final long value, final long index) {
            return (byte) (value >> Byte.SIZE * index);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitAdd")
    protected abstract static class PrimDigitAddNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimDigitAddNode(final CompiledMethodObject method) {
            super(method);
        }

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
        protected final Object doLongWithOverflow(final long lhs, final long rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile differentSignProfile) {
            if (differentSignProfile.profile(differentSign(lhs, rhs))) {
                return LargeIntegerObject.subtract(method.image, lhs, rhs);
            } else {
                return LargeIntegerObject.add(method.image, lhs, rhs);
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
        protected PrimDigitSubtractNode(final CompiledMethodObject method) {
            super(method);
        }

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
        protected final Object doLongWithOverflow(final long lhs, final long rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile differentSignProfile) {
            if (differentSignProfile.profile(differentSign(lhs, rhs))) {
                return LargeIntegerObject.add(method.image, lhs, rhs);
            } else {
                return LargeIntegerObject.subtract(method.image, lhs, rhs);
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
        protected PrimDigitMultiplyNegativeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs) {
            return Math.multiplyExact(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        protected final Object doLongWithOverflow(final long lhs, final long rhs) {
            return LargeIntegerObject.multiply(method.image, lhs, rhs);
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
        protected PrimDigitBitAndNode(final CompiledMethodObject method) {
            super(method);
        }

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
        protected PrimDigitBitOrNode(final CompiledMethodObject method) {
            super(method);
        }

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
        protected PrimDigitBitShiftMagnitudeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final long arg) {
            return receiver.shiftLeft((int) arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitBitXor")
    protected abstract static class PrimBitXorNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimBitXorNode(final CompiledMethodObject method) {
            super(method);
        }

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
        protected PrimDigitCompareNode(final CompiledMethodObject method) {
            super(method);
        }

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
        protected PrimDigitDivNegativeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final ArrayObject doLong(final long rcvr, final long arg, final boolean negative,
                        @Cached final BranchProfile signProfile) {
            long divide = rcvr / arg;
            if (negative && divide >= 0 || !negative && divide < 0) {
                signProfile.enter();
                if (divide == Long.MIN_VALUE) {
                    return createArrayWithLongMinOverflowResult(rcvr, arg);
                }
                divide = -divide;
            }
            return method.image.asArrayOfLongs(divide, rcvr % arg);
        }

        @TruffleBoundary
        private ArrayObject createArrayWithLongMinOverflowResult(final long rcvr, final long arg) {
            return method.image.asArrayOfObjects(LargeIntegerObject.createLongMinOverflowResult(method.image), rcvr % arg);
        }

        @Specialization
        @TruffleBoundary
        protected final ArrayObject doLargeInteger(final LargeIntegerObject rcvr, final LargeIntegerObject arg, final boolean negative) {
            final BigInteger[] divide = rcvr.getBigInteger().divideAndRemainder(arg.getBigInteger());
            final Object[] result = new Object[2];
            if (negative != divide[0].signum() < 0) {
                if (divide[0].bitLength() < Long.SIZE) {
                    final long lresult = divide[0].longValue();
                    if (lresult == Long.MIN_VALUE) {
                        result[0] = LargeIntegerObject.createLongMinOverflowResult(method.image);
                    } else {
                        result[0] = -lresult;
                    }
                } else {
                    result[0] = new LargeIntegerObject(method.image, divide[0].negate());
                }
            } else {
                if (divide[0].bitLength() < Long.SIZE) {
                    result[0] = divide[0].longValue();
                } else {
                    result[0] = new LargeIntegerObject(method.image, divide[0]);
                }
            }
            if (divide[1].bitLength() < Long.SIZE) {
                result[1] = divide[1].longValue();
            } else {
                result[1] = new LargeIntegerObject(method.image, divide[1]);
            }
            return method.image.asArrayOfObjects(result);
        }

        @Specialization
        protected final ArrayObject doLongLargeInteger(final long rcvr, final LargeIntegerObject arg, @SuppressWarnings("unused") final boolean negative) {
            assert !arg.fitsIntoLong() : "non-reduced large integer!";
            return method.image.asArrayOfLongs(0L, rcvr);
        }

        @Specialization
        @TruffleBoundary
        protected final ArrayObject doLargeIntegerLong(final LargeIntegerObject rcvr, final long arg, final boolean negative) {
            final BigInteger[] divide = rcvr.getBigInteger().divideAndRemainder(BigInteger.valueOf(arg));
            final Object[] result = new Object[2];
            if (negative != divide[0].signum() < 0) {
                if (divide[0].bitLength() < Long.SIZE) {
                    final long lresult = divide[0].longValue();
                    if (lresult == Long.MIN_VALUE) {
                        result[0] = LargeIntegerObject.createLongMinOverflowResult(method.image);
                    } else {
                        result[0] = -lresult;
                    }
                } else {
                    result[0] = new LargeIntegerObject(method.image, divide[0].negate());
                }
            } else {
                if (divide[0].bitLength() < Long.SIZE) {
                    result[0] = divide[0].longValue();
                } else {
                    result[0] = new LargeIntegerObject(method.image, divide[0]);
                }
            }
            result[1] = divide[1].longValue();
            return method.image.asArrayOfObjects(result);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primGetModuleName")
    protected abstract static class PrimGetModuleNameNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimGetModuleNameNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doGet(@SuppressWarnings("unused") final Object rcvr) {
            return method.image.asByteString(MODULE_NAME);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primMontgomeryDigitLength")
    protected abstract static class PrimMontgomeryDigitLengthNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimMontgomeryDigitLengthNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doDigitLength(@SuppressWarnings("unused") final Object receiver) {
            return 32L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primMontgomeryTimesModulo")
    protected abstract static class PrimMontgomeryTimesModuloNode extends AbstractArithmeticPrimitiveNode implements QuaternaryPrimitive {
        protected PrimMontgomeryTimesModuloNode(final CompiledMethodObject method) {
            super(method);
        }

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
        protected final Object doLong(final long receiver, final long a, final long m, final long mInv) {
            // TODO: avoid falling back to LargeIntegerObject
            return doLargeInteger(asLargeInteger(receiver), asLargeInteger(a), asLargeInteger(m), mInv);
        }

        @Specialization
        protected final Object doLong(final long receiver, final LargeIntegerObject a, final long m, final long mInv) {
            return doLargeInteger(asLargeInteger(receiver), a, asLargeInteger(m), mInv);
        }

        @Specialization
        protected final Object doLong(final long receiver, final long a, final LargeIntegerObject m, final long mInv) {
            return doLargeInteger(asLargeInteger(receiver), asLargeInteger(a), m, mInv);
        }

        @Specialization
        protected final Object doLong(final long receiver, final LargeIntegerObject a, final LargeIntegerObject m, final long mInv) {
            return doLargeInteger(asLargeInteger(receiver), a, m, mInv);
        }

        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject receiver, final long a, final long m, final long mInv) {
            return doLargeInteger(receiver, asLargeInteger(a), asLargeInteger(m), mInv);
        }

        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject receiver, final LargeIntegerObject a, final long m, final long mInv) {
            return doLargeInteger(receiver, a, asLargeInteger(m), mInv);
        }

        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject receiver, final long a, final LargeIntegerObject m, final long mInv) {
            return doLargeInteger(receiver, asLargeInteger(a), m, mInv);
        }

        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject receiver, final LargeIntegerObject a, final LargeIntegerObject m, final LargeIntegerObject mInv) {
            return doLargeInteger(receiver, a, m, mInv.longValueExact());
        }

        @Specialization
        @TruffleBoundary
        protected final Object doLargeInteger(final LargeIntegerObject receiver, final LargeIntegerObject a, final LargeIntegerObject m, final long mInv) {
            final int[] firstInts = ArrayConversionUtils.intsFromBytesExact(receiver.getBytes());
            final int[] secondInts = ArrayConversionUtils.intsFromBytesExact(a.getBytes());
            final int[] thirdInts = ArrayConversionUtils.intsFromBytesExact(m.getBytes());

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
            final byte[] resultBytes = ArrayConversionUtils.bytesFromInts(result);
            return new LargeIntegerObject(method.image, method.image.largePositiveIntegerClass, resultBytes).reduceIfPossible(); // normalize
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

        protected PrimNormalizeNode(final CompiledMethodObject method) {
            super(method);
        }

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
        protected static final Object doNativeObject(final NativeObject receiver) {
            return new LargeIntegerObject(receiver.image, receiver.getSqueakClass(), receiver.getByteStorage().clone());
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return LargeIntegersFactory.getFactories();
    }
}
