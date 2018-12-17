package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodes.ReturnReceiverNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.util.ArrayConversionUtils;

public final class LargeIntegers extends AbstractPrimitiveFactoryHolder {
    private static final String MODULE_NAME = "LargeIntegers v2.0 (GraalSqueak)";

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return LargeIntegersFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primAnyBitFromTo")
    public abstract static class PrimAnyBitFromToNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        public PrimAnyBitFromToNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"start >= 1", "stopArg >= 1"})
        protected final boolean doLong(final long receiver, final long start, final long stopArg) {
            final long stop = Math.min(stopArg, Long.highestOneBit(receiver));
            if (start > stop) {
                return code.image.sqFalse;
            }
            final long firstDigitIndex = Math.floorDiv(start - 1, 32);
            final long lastDigitIndex = Math.floorDiv(stop - 1, 32);
            final long firstMask = 0xFFFFFFFFL << ((start - 1) & 31);
            final long lastMask = 0xFFFFFFFFL >> (31 - ((stop - 1) & 31));
            if (firstDigitIndex == lastDigitIndex) {
                final byte digit = digitOf(receiver, firstDigitIndex);
                if ((digit & (firstMask & lastMask)) != 0) {
                    return code.image.sqTrue;
                } else {
                    return code.image.sqFalse;
                }
            }
            if (((digitOf(receiver, firstDigitIndex)) & firstMask) != 0) {
                return code.image.sqTrue;
            }
            for (long i = firstDigitIndex + 1; i < lastDigitIndex - 1; i++) {
                if (digitOf(receiver, i) != 0) {
                    return code.image.sqTrue;
                }
            }
            if ((digitOf(receiver, lastDigitIndex) & lastMask) != 0) {
                return code.image.sqTrue;
            }
            return code.image.sqFalse;
        }

        @Specialization(guards = {"start >= 1", "stopArg >= 1"}, rewriteOn = {ArithmeticException.class})
        protected final boolean doLargeIntegerAsLong(final LargeIntegerObject receiver, final long start, final long stopArg) {
            return doLong(receiver.longValueExact(), start, stopArg);
        }

        @Specialization(guards = {"start >= 1", "stopArg >= 1"})
        protected final boolean doLargeInteger(final LargeIntegerObject receiver, final long start, final long stopArg) {
            final long stop = Math.min(stopArg, receiver.bitLength());
            if (start > stop) {
                return code.image.sqFalse;
            }
            final long firstDigitIndex = Math.floorDiv(start - 1, 32);
            final long lastDigitIndex = Math.floorDiv(stop - 1, 32);
            final long firstMask = 0xFFFFFFFFL << ((start - 1) & 31);
            final long lastMask = 0xFFFFFFFFL >> (31 - ((stop - 1) & 31));
            if (firstDigitIndex == lastDigitIndex) {
                final long digit = receiver.getNativeAt0(firstDigitIndex);
                if ((digit & (firstMask & lastMask)) != 0) {
                    return code.image.sqTrue;
                } else {
                    return code.image.sqFalse;
                }
            }
            if ((receiver.getNativeAt0(firstDigitIndex) & firstMask) != 0) {
                return code.image.sqTrue;
            }
            for (long i = firstDigitIndex + 1; i < lastDigitIndex - 1; i++) {
                if (receiver.getNativeAt0(i) != 0) {
                    return code.image.sqTrue;
                }
            }
            if ((receiver.getNativeAt0(lastDigitIndex) & lastMask) != 0) {
                return code.image.sqTrue;
            }
            return code.image.sqFalse;
        }

        private static byte digitOf(final long value, final long index) {
            return (byte) (value >> (Byte.SIZE * index));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitAdd")
    public abstract static class PrimDigitAddNode extends AbstractDigitPrimitiveNode {
        public PrimDigitAddNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "sameSign(a, b)", rewriteOn = ArithmeticException.class)
        protected static final Object doLongPositive(final long a, final long b) {
            return Math.addExact(a, b);
        }

        @Specialization(guards = "sameSign(a, b)")
        protected final Object doLongPositiveWithOverflow(final long a, final long b) {
            return doLargeIntegerPositive(asLargeInteger(a), asLargeInteger(b));
        }

        @Specialization(guards = "!sameSign(a, b)", rewriteOn = ArithmeticException.class)
        protected static final Object doLongNegative(final long a, final long b) {
            return Math.subtractExact(a, b);
        }

        @Specialization(guards = "!sameSign(a, b)")
        protected final Object doLongNegativeWithOverflow(final long a, final long b) {
            return doLargeIntegerNegative(asLargeInteger(a), asLargeInteger(b));
        }

        @Specialization(guards = "sameSign(a, b)")
        protected static final Object doLargeIntegerPositive(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.add(b);
        }

        @Specialization(guards = "!sameSign(a, b)")
        protected static final Object doLargeIntegerNegative(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.subtract(b);
        }

        @Specialization(guards = "sameSign(b, a)")
        protected final Object doLongLargeIntegerPositive(final long a, final LargeIntegerObject b) {
            return doLargeIntegerPositive(asLargeInteger(a), b);
        }

        @Specialization(guards = "!sameSign(b, a)")
        protected final Object doLongLargeIntegerNegative(final long a, final LargeIntegerObject b) {
            return doLargeIntegerNegative(asLargeInteger(a), b);
        }

        @Specialization(guards = "sameSign(a, b)")
        protected final Object doLargeIntegerLongPositive(final LargeIntegerObject a, final long b) {
            return doLargeIntegerPositive(a, asLargeInteger(b));
        }

        @Specialization(guards = "!sameSign(a, b)")
        protected final Object doLargeIntegerLongNegative(final LargeIntegerObject a, final long b) {
            return doLargeIntegerNegative(a, asLargeInteger(b));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitSubtract")
    public abstract static class PrimDigitSubtractNode extends AbstractDigitPrimitiveNode {
        public PrimDigitSubtractNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "sameSign(a, b)", rewriteOn = ArithmeticException.class)
        protected static final Object doLongPositive(final long a, final long b) {
            return Math.subtractExact(a, b);
        }

        @Specialization(guards = "sameSign(a, b)")
        protected final Object doLongPositiveWithOverflow(final long a, final long b) {
            return doLargeIntegerPositive(asLargeInteger(a), asLargeInteger(b));
        }

        @Specialization(guards = "!sameSign(a, b)", rewriteOn = ArithmeticException.class)
        protected static final Object doLongNegative(final long a, final long b) {
            return Math.addExact(a, b);
        }

        @Specialization(guards = "!sameSign(a, b)")
        protected final Object doLongNegativeWithOverflow(final long a, final long b) {
            return doLargeIntegerNegative(asLargeInteger(a), asLargeInteger(b));
        }

        @Specialization(guards = "sameSign(a, b)")
        protected static final Object doLargeIntegerPositive(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.subtract(b);
        }

        @Specialization(guards = "!sameSign(a, b)")
        protected static final Object doLargeIntegerNegative(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.add(b);
        }

        @Specialization(guards = "sameSign(b, a)")
        protected final Object doLongLargeIntegerPositive(final long a, final LargeIntegerObject b) {
            return doLargeIntegerPositive(asLargeInteger(a), b);
        }

        @Specialization(guards = "!sameSign(b, a)")
        protected final Object doLongLargeIntegerNegative(final long a, final LargeIntegerObject b) {
            return doLargeIntegerNegative(asLargeInteger(a), b);
        }

        @Specialization(guards = "sameSign(a, b)")
        protected final Object doLargeIntegerLongPositive(final LargeIntegerObject a, final long b) {
            return doLargeIntegerPositive(a, asLargeInteger(b));
        }

        @Specialization(guards = "!sameSign(a, b)")
        protected final Object doLargeIntegerLongNegative(final LargeIntegerObject a, final long b) {
            return doLargeIntegerNegative(a, asLargeInteger(b));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitMultiplyNegative")
    public abstract static class PrimDigitMultiplyNegativeNode extends AbstractDigitPrimitiveNode {
        public PrimDigitMultiplyNegativeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final Object doLong(final long a, final long b) {
            return Math.multiplyExact(a, b);
        }

        @Specialization
        protected final Object doLongWithOverflow(final long a, final long b) {
            return doLargeInteger(asLargeInteger(a), asLargeInteger(b));
        }

        @Specialization
        protected final Object doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization
        protected final Object doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.multiply(b);
        }

        @Specialization
        protected static final Object doDouble(final double a, final double b) {
            return a * b;
        }

        @Specialization
        protected final Object doFloat(final FloatObject a, final FloatObject b) {
            return asFloatObject(a.getValue() * b.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitBitAnd")
    public abstract static class PrimDigitBitAndNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        public PrimDigitBitAndNode(final CompiledMethodObject method) {
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
        protected final Object doLong(final long receiver, final LargeIntegerObject arg) {
            return doLargeInteger(asLargeInteger(receiver), arg);
        }

        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject receiver, final long arg) {
            return doLargeInteger(receiver, asLargeInteger(arg));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitBitOr")
    public abstract static class PrimDigitBitOrNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        public PrimDigitBitOrNode(final CompiledMethodObject method) {
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
        protected final Object doLong(final long receiver, final LargeIntegerObject arg) {
            return doLargeInteger(asLargeInteger(receiver), arg);
        }

        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject receiver, final long arg) {
            return doLargeInteger(receiver, asLargeInteger(arg));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {17, 37}, names = "primDigitBitShiftMagnitude")
    public abstract static class PrimBitShiftNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        public PrimBitShiftNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"arg >= 0", "!isLShiftLongOverflow(receiver, arg)"})
        protected static final long doLong(final long receiver, final long arg) {
            return receiver << arg;
        }

        @Specialization(guards = {"arg >= 0", "isLShiftLongOverflow(receiver, arg)"})
        protected final Object doLongLargeInteger(final long receiver, final long arg) {
            return doLargeInteger(asLargeInteger(receiver), arg);
        }

        @Specialization(guards = {"arg < 0", "isArgInLongSizeRange(arg)"})
        protected static final long doLongNegativeLong(final long receiver, final long arg) {
            // The result of a right shift can only become smaller than the receiver and 0 or -1 at
            // minimum, so no BigInteger needed here
            return receiver >> -arg;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"arg < 0", "!isArgInLongSizeRange(arg)"})
        protected static final long doLongNegative(final long receiver, final long arg) {
            return receiver >= 0 ? 0L : -1L;
        }

        @Specialization(guards = {"arg >= 0"})
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final long arg) {
            return receiver.shiftLeft((int) arg);
        }

        @Specialization(guards = {"arg < 0"})
        protected static final Object doLargeIntegerNegative(final LargeIntegerObject receiver, final long arg) {
            return receiver.shiftRight((int) -arg);
        }

        @Specialization(guards = {"arg >= 0", "receiver.isByteType()"})
        protected static final Object doNativeObject(final NativeObject receiver, final long arg) {
            return doLargeInteger(receiver.normalize(), arg);
        }

        @Specialization(guards = {"arg < 0", "receiver.isByteType()"})
        protected static final Object doNativeObjectNegative(final NativeObject receiver, final long arg) {
            return doLargeIntegerNegative(receiver.normalize(), arg);
        }

        protected static final boolean isLShiftLongOverflow(final long receiver, final long arg) {
            // -1 needed, because we do not want to shift a positive long into negative long (most
            // significant bit indicates positive/negative)
            return Long.numberOfLeadingZeros(receiver) - 1 < arg;
        }

        protected static final boolean isArgInLongSizeRange(final long value) {
            return -Long.SIZE < value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {16, 36}, names = "primDigitBitXor")
    protected abstract static class PrimBitXorNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimBitXorNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doLong(final long receiver, final long b) {
            return receiver ^ b;
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final LargeIntegerObject arg) {
            return receiver.xor(arg);
        }

        @Specialization
        protected final Object doLong(final long receiver, final LargeIntegerObject arg) {
            return doLargeInteger(asLargeInteger(receiver), arg);
        }

        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject receiver, final long arg) {
            return doLargeInteger(receiver, asLargeInteger(arg));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitCompare")
    public abstract static class PrimDigitCompareNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        public PrimDigitCompareNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "a == b")
        protected long doLongEqual(final long a, final long b) {
            return 0L;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "a != b")
        protected long doLongNotEqual(final long a, final long b) {
            return digitCompareLargewith(asLargeInteger(a), asLargeInteger(b));
        }

        @Specialization
        protected long doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return digitCompareLargewith(asLargeInteger(a), b);
        }

        @Specialization
        protected long doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return digitCompareLargewith(a, b);
        }

        @Specialization
        protected long doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return digitCompareLargewith(a, asLargeInteger(b));
        }

        /*
         * Compare the magnitude of firstInteger with that of secondInteger. Return a code of 1, 0,
         * -1 for firstInteger >, = , < secondInteger
         */
        private static long digitCompareLargewith(final LargeIntegerObject firstInteger, final LargeIntegerObject secondInteger) {
            final byte[] firstBytes = firstInteger.getBytes();
            final byte[] secondBytes = secondInteger.getBytes();

            final int firstLen = firstBytes.length;
            final int secondLen = secondBytes.length;
            if (secondLen != firstLen) {
                if (secondLen > firstLen) {
                    return -1;
                } else {
                    return 1;
                }
            }
            return cDigitComparewithlen(firstBytes, secondBytes, firstLen);
        }

        /* Precondition: pFirst len = pSecond len. */
        private static long cDigitComparewithlen(final byte[] pFirst, final byte[] pSecond, final int len) {
            int firstDigit;
            int secondDigit;
            int ix = len - 1;
            while (ix >= 0) {
                if (((secondDigit = Byte.toUnsignedInt(pSecond[ix]))) != ((firstDigit = Byte.toUnsignedInt(pFirst[ix])))) {
                    if (secondDigit < firstDigit) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
                --ix;
            }
            return 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDigitDivNegative")
    public abstract static class PrimDigitDivNegativeNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        public PrimDigitDivNegativeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected final ArrayObject doLong(final long rcvr, final long arg, final boolean negative) {
            long divide = rcvr / arg;
            if ((negative && divide >= 0) || (!negative && divide < 0)) {
                divide = Math.negateExact(divide);
            }
            return code.image.newList(new long[]{divide, rcvr % arg});
        }

        @Specialization
        protected final ArrayObject doLongWithOverflow(final long rcvr, final long arg, final boolean negative) {
            return doLargeInteger(asLargeInteger(rcvr), asLargeInteger(arg), negative);
        }

        @Specialization
        protected final ArrayObject doLargeInteger(final LargeIntegerObject rcvr, final LargeIntegerObject arg, final boolean negative) {
            LargeIntegerObject divide = rcvr.divideNoReduce(arg);
            if (negative != divide.isNegative()) {
                divide = divide.negateNoReduce();
            }
            final Object remainder = rcvr.remainder(arg);
            return code.image.newListWith(divide.reduceIfPossible(), remainder);
        }

        @Specialization
        protected final ArrayObject doLong(final long rcvr, final LargeIntegerObject arg, final boolean negative) {
            return doLargeInteger(asLargeInteger(rcvr), arg, negative);
        }

        @Specialization
        protected final ArrayObject doLargeInteger(final LargeIntegerObject rcvr, final long arg, final boolean negative) {
            return doLargeInteger(rcvr, asLargeInteger(arg), negative);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primGetModuleName")
    public abstract static class PrimGetModuleNameNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        public PrimGetModuleNameNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doGet(@SuppressWarnings("unused") final AbstractSqueakObject rcvr) {
            return code.image.wrap(MODULE_NAME);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primMontgomeryDigitLength")
    public abstract static class PrimMontgomeryDigitLengthNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        public PrimMontgomeryDigitLengthNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doDigitLength(@SuppressWarnings("unused") final Object receiver) {
            return 32L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primMontgomeryTimesModulo")
    public abstract static class PrimMontgomeryTimesModuloNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {

        public PrimMontgomeryTimesModuloNode(final CompiledMethodObject method) {
            super(method);
        }

        /*
         * Optimized version of montgomeryTimesModulo for integer-sized arguments.
         */
        @Specialization(guards = {"fitsInOneWord(receiver)", "fitsInOneWord(a)", "fitsInOneWord(m)"})
        protected static final long doLongQuick(final long receiver, final long a, final long m, final long mInv) {
            final long accum3 = receiver * a;
            final long u = (accum3 * mInv) & 0xFFFFFFFFL;
            final long accum2 = u * m;
            long accum = (accum2 & 0xFFFFFFFFL) + (accum3 & 0xFFFFFFFFL);
            accum = (accum >> 32) + (accum2 >> 32) + (accum3 >> 32);
            long result = accum & 0xFFFFFFFFL;
            if (!((accum >> 32) == 0 && result < m)) {
                result = (result - m) & 0xFFFFFFFFL;
            }
            return result;
        }

        protected static final boolean fitsInOneWord(final long value) {
            return value <= NativeObject.INTEGER_MAX;
        }

        @Specialization(guards = {"(!fitsInOneWord(receiver) || !fitsInOneWord(a)) || !fitsInOneWord(m)"})
        protected final Object doLong(final long receiver, final long a, final long m, final long mInv) {
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
        protected final Object doLargeInteger(final LargeIntegerObject receiver, final LargeIntegerObject a, final LargeIntegerObject m, final long mInv) {
            final int[] firstInts = ArrayConversionUtils.intsFromBytesReversedExact(receiver.getBytes());
            final int[] secondInts = ArrayConversionUtils.intsFromBytesReversedExact(a.getBytes());
            final int[] thirdInts = ArrayConversionUtils.intsFromBytesReversedExact(m.getBytes());
            return montgomeryTimesModulo(firstInts, secondInts, thirdInts, mInv);
        }

        private Object montgomeryTimesModulo(final int[] firstInts, final int[] secondInts, final int[] thirdInts, final long mInv) {
            final int firstLen = firstInts.length;
            final int secondLen = secondInts.length;
            final int thirdLen = thirdInts.length;
            if (firstLen > thirdLen || secondLen > thirdLen) {
                throw new PrimitiveFailed();
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
                accum3 = (accum3 * (secondInts[0] & 0xFFFFFFFFL)) + (result[0] & 0xFFFFFFFFL);
                final long u = (accum3 * mInv) & 0xFFFFFFFFL;
                accum2 = u * (thirdInts[0] & 0xFFFFFFFFL);
                accum = (accum2 & 0xFFFFFFFFL) + (accum3 & 0xFFFFFFFFL);
                accum = (accum >> 32) + (accum2 >> 32) + ((accum3 >> 32) & 0xFFFFFFFFL);
                for (int k = 1; k <= limit2; k++) {
                    accum3 = firstInts[i] & 0xFFFFFFFFL;
                    accum3 = (accum3 * (secondInts[k] & 0xFFFFFFFFL)) + (result[k] & 0xFFFFFFFFL);
                    accum2 = u * (thirdInts[k] & 0xFFFFFFFFL);
                    accum = accum + (accum2 & 0xFFFFFFFFL) + (accum3 & 0xFFFFFFFFL);
                    result[k - 1] = (int) (accum & 0xFFFFFFFFL);
                    accum = (accum >> 32) + (accum2 >> 32) + ((accum3 >> 32) & 0xFFFFFFFFL);
                }
                for (int k = secondLen; k <= limit3; k++) {
                    accum2 = u * (thirdInts[k] & 0xFFFFFFFFL);
                    accum = accum + (result[k] & 0xFFFFFFFFL) + (accum2 & 0xFFFFFFFFL);
                    result[k - 1] = (int) (accum & 0xFFFFFFFFL);
                    accum = ((accum >> 32) + (accum2 >> 32)) & 0xFFFFFFFFL;
                }
                accum += lastDigit;
                result[limit3] = (int) (accum & 0xFFFFFFFFL);
                lastDigit = (int) (accum >> 32);
            }
            for (int i = firstLen; i <= limit3; i++) {
                accum = result[0] & 0xFFFFFFFFL;
                final long u = (accum * mInv) & 0xFFFFFFFFL;
                accum += u * (thirdInts[0] & 0xFFFFFFFFL);
                accum = accum >> 32;
                for (int k = 1; k <= limit3; k++) {
                    accum2 = u * (thirdInts[k] & 0xFFFFFFFFL);
                    accum = accum + (result[k] & 0xFFFFFFFFL) + (accum2 & 0xFFFFFFFFL);
                    result[k - 1] = (int) (accum & 0xFFFFFFFFL);
                    accum = ((accum >> 32) + (accum2 >> 32)) & 0xFFFFFFFFL;
                }
                accum += lastDigit;
                result[limit3] = (int) (accum & 0xFFFFFFFFL);
                lastDigit = (int) (accum >> 32);
            }
            if (!((lastDigit == 0) && (cDigitComparewithlen(thirdInts, result, thirdLen) == 1))) {
                accum = 0;
                for (int i = 0; i <= limit3; i++) {
                    accum = accum + result[i] - (thirdInts[i] & 0xFFFFFFFFL);
                    result[i] = (int) (accum & 0xFFFFFFFFL);
                    accum = 0 - (accum >> 63);
                }
            }
            final byte[] resultBytes = ArrayConversionUtils.bytesFromIntsReversed(result);
            return new LargeIntegerObject(code.image, code.image.largePositiveIntegerClass, resultBytes).reduceIfPossible(); // normalize
        }

        private static int cDigitComparewithlen(final int[] first, final int[] second, final int len) {
            int firstDigit;
            int secondDigit;
            int index = len - 1;
            while (index >= 0) {
                if (((secondDigit = second[index])) != ((firstDigit = first[index]))) {
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
    public abstract static class PrimNormalizeNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private ReturnReceiverNode receiverNode;

        public PrimNormalizeNode(final CompiledMethodObject method) {
            super(method);
            receiverNode = ReturnReceiverNode.create(method, -1);
        }

        @Specialization
        protected static final long doLong(final long value) {
            return value;
        }

        @Specialization
        public static final Object doLargeInteger(final LargeIntegerObject value) {
            return value.reduceIfPossible();
        }

        @Specialization(guards = "receiver.isByteType()")
        protected static final Object doNativeObject(final NativeObject receiver) {
            return receiver.normalize();
        }
    }

    protected abstract static class AbstractDigitPrimitiveNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        public AbstractDigitPrimitiveNode(final CompiledMethodObject method) {
            super(method);
        }

        protected static final boolean sameSign(final long a, final long b) {
            return (a >= 0 && b >= 0) || (a < 0 && b < 0);
        }

        protected static final boolean sameSign(final LargeIntegerObject a, final long b) {
            return (a.signum() >= 0 && b >= 0) || (a.signum() < 0 && b < 0);
        }

        protected static final boolean sameSign(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.signum() == b.signum();
        }
    }
}
