package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.math.BigInteger;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnBytecodes.ReturnReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.AbstractArithmeticPrimitiveNode;

public final class LargeIntegers extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return LargeIntegersFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {1, 21, 41}, name = "primDigitAdd", numArguments = 2)
    public static abstract class PrimAddNode extends AbstractArithmeticPrimitiveNode {

        public PrimAddNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected final static long doLong(final long a, final long b) {
            return Math.addExact(a, b);
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doLongWithOverflow(final long a, final long argument) {
            return doBigInteger(BigInteger.valueOf(a), BigInteger.valueOf(argument));
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doBigInteger(final BigInteger a, final BigInteger b) {
            return reduceIfPossible(a.add(b));
        }

        @Specialization
        protected final static double doDouble(final double a, final double b) {
            return a + b;
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doLong(final long a, final BigInteger b) {
            return doBigInteger(BigInteger.valueOf(a), b);
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doBigInteger(final BigInteger a, final long b) {
            return doBigInteger(a, BigInteger.valueOf(b));
        }

        @Specialization
        protected final static double doLong(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final static double doDouble(final double a, final long b) {
            return doDouble(a, (double) b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {2, 22, 42}, name = "primDigitSubtract", numArguments = 2)
    public static abstract class PrimSubstractNode extends AbstractArithmeticPrimitiveNode {
        public PrimSubstractNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected final static long doLong(final long a, final long b) {
            return Math.subtractExact(a, b);
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doLongWithOverflow(final long a, final long b) {
            return doBigInteger(BigInteger.valueOf(a), BigInteger.valueOf(b));
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doBigInteger(final BigInteger a, final BigInteger b) {
            return reduceIfPossible(a.subtract(b));
        }

        @Specialization
        protected final static double doDouble(final double a, final double b) {
            return a - b;
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doLong(final long a, final BigInteger b) {
            return doBigInteger(BigInteger.valueOf(a), b);
        }

        @Specialization
        protected final static double doLong(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doBigInteger(final BigInteger a, final long b) {
            return doBigInteger(a, BigInteger.valueOf(b));
        }

        @Specialization
        protected final static double doDouble(final double a, final long b) {
            return doDouble(a, (double) b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {9, 29, 49}, name = "primDigitMultiplyNegative", numArguments = 2)
    public static abstract class PrimMultiplyNode extends AbstractArithmeticPrimitiveNode {
        public PrimMultiplyNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected final static long doLong(final long a, final long b) {
            return Math.multiplyExact(a, b);
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doLongWithOverflow(final long a, final long b) {
            return doBigInteger(BigInteger.valueOf(a), BigInteger.valueOf(b));
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doBigInteger(final BigInteger a, final BigInteger b) {
            return reduceIfPossible(a.multiply(b));
        }

        @Specialization
        protected final static double doDouble(final double a, final double b) {
            return a * b;
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doLong(final long a, final BigInteger b) {
            return doBigInteger(BigInteger.valueOf(a), b);
        }

        @Specialization
        protected final static double doLong(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doBigInteger(final BigInteger a, final long b) {
            return doBigInteger(a, BigInteger.valueOf(b));
        }

        @Specialization
        protected final static double doDouble(final double a, final long b) {
            return doDouble(a, (double) b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primDigitDivNegative", numArguments = 3)
    public static abstract class PrimDigitDivNegativeNode extends AbstractArithmeticPrimitiveNode {
        public PrimDigitDivNegativeNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected final ListObject doLong(final long rcvr, final long arg, final boolean negative) {
            long divide = rcvr / arg;
            if ((negative && divide >= 0) || (!negative && divide < 0)) {
                divide = Math.negateExact(divide);
            }
            long remainder = rcvr % arg;
            return code.image.wrap(new Object[]{divide, remainder});
        }

        @Specialization
        @TruffleBoundary
        protected final ListObject doLongWithOverflow(final long rcvr, final long arg, final boolean negative) {
            return doBigInteger(BigInteger.valueOf(rcvr), BigInteger.valueOf(arg), negative);
        }

        @Specialization
        @TruffleBoundary
        protected final ListObject doBigInteger(final BigInteger rcvr, final BigInteger arg, final boolean negative) {
            BigInteger divide = rcvr.divide(arg);
            if ((negative && divide.signum() >= 0) || (!negative && divide.signum() < 0)) {
                divide = divide.negate();
            }
            BigInteger remainder = rcvr.remainder(arg);
            return code.image.wrap(new Object[]{reduceIfPossible(divide), reduceIfPossible(remainder)});
        }

        @Specialization
        @TruffleBoundary
        protected final ListObject doLong(final long rcvr, final BigInteger arg, final boolean negative) {
            return doBigInteger(BigInteger.valueOf(rcvr), arg, negative);
        }

        @Specialization
        @TruffleBoundary
        protected final ListObject doBigInteger(final BigInteger rcvr, final long arg, final boolean negative) {
            return doBigInteger(rcvr, BigInteger.valueOf(arg), negative);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {14, 34}, name = "primDigitBitAnd", numArguments = 2)
    public static abstract class PrimBitAndNode extends AbstractArithmeticPrimitiveNode {
        public PrimBitAndNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static long doLong(final long receiver, final long arg) {
            return receiver & arg;
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doBigInteger(final BigInteger receiver, final BigInteger arg) {
            return reduceIfPossible(receiver.and(arg));
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doLong(final long receiver, final BigInteger arg) {
            return doBigInteger(BigInteger.valueOf(receiver), arg);
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doBigInteger(final BigInteger receiver, final long arg) {
            return doBigInteger(receiver, BigInteger.valueOf(arg));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {15, 35}, name = "primDigitBitOr", numArguments = 2)
    public static abstract class PrimBitOrNode extends AbstractArithmeticPrimitiveNode {
        public PrimBitOrNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static long bitOr(final long receiver, final long arg) {
            return receiver | arg;
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doBigInteger(final BigInteger receiver, final BigInteger arg) {
            return reduceIfPossible(receiver.or(arg));
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doLong(final long receiver, final BigInteger arg) {
            return doBigInteger(BigInteger.valueOf(receiver), arg);
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doBigInteger(final BigInteger receiver, final long arg) {
            return doBigInteger(receiver, BigInteger.valueOf(arg));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {17, 37}, name = "primDigitBitShiftMagnitude", numArguments = 2)
    public static abstract class PrimBitShiftNode extends AbstractArithmeticPrimitiveNode {

        public PrimBitShiftNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"arg >= 0"})
        protected final static long doLong(final long receiver, final long arg) {
            return receiver << arg;
        }

        @Specialization(guards = {"arg < 0"})
        protected final static long doLongNegative(final long receiver, final long arg) {
            return receiver >> -arg;
        }

        @Specialization(guards = {"arg >= 0"})
        @TruffleBoundary
        protected final static Number doBigInteger(final BigInteger receiver, final long arg) {
            return reduceIfPossible(receiver.shiftLeft((int) arg));
        }

        @Specialization(guards = {"arg < 0"})
        @TruffleBoundary
        protected final static Number doBigIntegerNegative(final BigInteger receiver, final long arg) {
            return reduceIfPossible(receiver.shiftRight((int) -arg));
        }

        @Specialization(guards = {"arg >= 0"})
        @TruffleBoundary
        protected final static Number doNativeObject(final NativeObject receiver, final long arg) {
            return doBigInteger(receiver.normalize(), arg);
        }

        @Specialization(guards = {"arg < 0"})
        @TruffleBoundary
        protected final static Number doNativeObjectNegative(final NativeObject receiver, final long arg) {
            return doBigIntegerNegative(receiver.normalize(), arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = {"primNormalizePositive", "primNormalizeNegative"})
    public static abstract class PrimNormalizeNode extends AbstractArithmeticPrimitiveNode {
        @Child private ReturnReceiverNode receiverNode;

        public PrimNormalizeNode(CompiledMethodObject method) {
            super(method);
            receiverNode = ReturnReceiverNode.create(method, -1);
        }

        @Specialization
        protected long doLong(long value) {
            return value;
        }

        @Specialization
        @TruffleBoundary
        public Number doBigInteger(BigInteger value) {
            return reduceIfPossible(value);
        }

        @Specialization
        protected Number doNativeObject(NativeObject value) {
            return reduceIfPossible(value.normalize());
        }
    }

}
