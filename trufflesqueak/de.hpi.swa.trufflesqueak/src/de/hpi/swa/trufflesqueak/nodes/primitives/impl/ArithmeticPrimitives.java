package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public final class ArithmeticPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArithmeticPrimitivesFactory.getFactories();
    }

    public static abstract class AbstractArithmeticPrimitiveNode extends AbstractPrimitiveNode {

        public AbstractArithmeticPrimitiveNode(CompiledMethodObject method) {
            super(method);
        }

        @TruffleBoundary
        protected static final Number reduceIfPossible(BigInteger value) {
            if (value.bitLength() > Long.SIZE - 1) {
                return value;
            } else {
                return value.longValue();
            }
        }

        @Override
        public final Object executeWithArguments(VirtualFrame frame, Object... arguments) {
            try {
                return executeWithArgumentsSpecialized(frame, arguments);
            } catch (ArithmeticException e) {
                throw new PrimitiveFailed();
            }
        }

        @Override
        public final Object executeRead(VirtualFrame frame) {
            try {
                return executePrimitive(frame);
            } catch (ArithmeticException e) {
                throw new PrimitiveFailed();
            }
        }

        public abstract Object executePrimitive(VirtualFrame frame);
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {3, 23, 43}, numArguments = 2)
    protected static abstract class PrimLessThanNode extends AbstractArithmeticPrimitiveNode {
        protected PrimLessThanNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static boolean doLong(final long left, final long right) {
            return left < right;
        }

        @Specialization
        @TruffleBoundary
        protected final static boolean doBigInteger(final BigInteger left, final BigInteger right) {
            return left.compareTo(right) < 0;
        }

        @Specialization
        protected final static boolean doDouble(final double left, final double right) {
            return left < right;
        }

        @Specialization
        @TruffleBoundary
        protected final static boolean doLong(final long left, final BigInteger right) {
            return doBigInteger(BigInteger.valueOf(left), right);
        }

        @Specialization
        protected final static boolean doLong(final long left, final double right) {
            return doDouble(left, right);
        }

        @Specialization
        @TruffleBoundary
        protected final static boolean doBigInteger(final BigInteger left, final long right) {
            return doBigInteger(left, BigInteger.valueOf(right));
        }

        @Specialization
        protected final static boolean doDouble(final double left, final long right) {
            return doDouble(left, (double) right);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {4, 24, 44}, numArguments = 2)
    protected static abstract class PrimGreaterThanNode extends AbstractArithmeticPrimitiveNode {
        protected PrimGreaterThanNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static boolean doLong(final long left, final long right) {
            return left > right;
        }

        @Specialization
        @TruffleBoundary
        protected final static boolean doBigInteger(final BigInteger left, final BigInteger right) {
            return left.compareTo(right) > 0;
        }

        @Specialization
        protected final static boolean doDouble(final double left, final double right) {
            return left > right;
        }

        @Specialization
        @TruffleBoundary
        protected final static boolean doLong(final long left, final BigInteger right) {
            return doBigInteger(BigInteger.valueOf(left), right);
        }

        @Specialization
        protected final static boolean doLong(final long left, final double right) {
            return doDouble(left, right);
        }

        @Specialization
        @TruffleBoundary
        protected final static boolean doBigInteger(final BigInteger left, final long right) {
            return doBigInteger(left, BigInteger.valueOf(right));
        }

        @Specialization
        protected final static boolean doDouble(final double left, final long right) {
            return doDouble(left, (double) right);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {5, 25, 45}, numArguments = 2)
    protected static abstract class PrimLessOrEqualNode extends AbstractArithmeticPrimitiveNode {
        protected PrimLessOrEqualNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static boolean doLong(final long left, final long right) {
            return left <= right;
        }

        @Specialization
        @TruffleBoundary
        protected final static boolean doBigInteger(final BigInteger left, final BigInteger right) {
            return left.compareTo(right) <= 0;
        }

        @Specialization
        protected final static boolean doDouble(final double left, final double right) {
            return left <= right;
        }

        @Specialization
        @TruffleBoundary
        protected final static boolean doLong(final long left, final BigInteger right) {
            return doBigInteger(BigInteger.valueOf(left), right);
        }

        @Specialization
        protected final static boolean doLong(final long left, final double right) {
            return doDouble(left, right);
        }

        @Specialization
        @TruffleBoundary
        protected final static boolean doBigInteger(final BigInteger left, final long right) {
            return doBigInteger(left, BigInteger.valueOf(right));
        }

        @Specialization
        protected final static boolean doDouble(final double left, final long right) {
            return doDouble(left, (double) right);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {6, 26, 46}, numArguments = 2)
    protected static abstract class PrimGreaterOrEqualNode extends AbstractArithmeticPrimitiveNode {
        protected PrimGreaterOrEqualNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static boolean doLong(final long left, final long right) {
            return left >= right;
        }

        @Specialization
        @TruffleBoundary
        protected final static boolean doBigInteger(final BigInteger left, final BigInteger right) {
            return left.compareTo(right) >= 0;
        }

        @Specialization
        protected final static boolean doDouble(final double left, final double right) {
            return left >= right;
        }

        @Specialization
        @TruffleBoundary
        protected final static boolean doLong(final long left, final BigInteger right) {
            return doBigInteger(BigInteger.valueOf(left), right);
        }

        @Specialization
        protected final static boolean doLong(final long left, final double right) {
            return doDouble(left, right);
        }

        @Specialization
        @TruffleBoundary
        protected final static boolean doBigInteger(final BigInteger left, final long right) {
            return doBigInteger(left, BigInteger.valueOf(right));
        }

        @Specialization
        protected final static boolean doDouble(final double left, final long right) {
            return doDouble(left, (double) right);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {7, 27, 47}, numArguments = 2)
    protected static abstract class PrimEqualNode extends AbstractArithmeticPrimitiveNode {
        protected PrimEqualNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static boolean doBoolean(final boolean left, final boolean right) {
            return left == right;
        }

        @Specialization
        protected final static boolean doLong(final long left, final long right) {
            return left == right;
        }

        @Specialization
        @TruffleBoundary
        protected final static boolean doBigInteger(final BigInteger left, final BigInteger right) {
            return left.compareTo(right) == 0;
        }

        @Specialization
        protected final static boolean doDouble(final double left, final double right) {
            return left == right;
        }

        @Specialization
        protected boolean doChar(char receiver, char argument) {
            return receiver == argument;
        }

        @Specialization
        protected final static boolean doLong(final long left, final double right) {
            return left == right;
        }

        @Specialization
        @TruffleBoundary
        protected final static boolean doBigInteger(final BigInteger left, final long right) {
            return doBigInteger(left, BigInteger.valueOf(right));
        }

        @Specialization
        @TruffleBoundary
        protected final static boolean doBigInteger(final BigInteger left, final double right) {
            return doBigInteger(left, BigInteger.valueOf((long) right));
        }

        @Specialization
        @TruffleBoundary
        protected final static boolean doDouble(final double left, final BigInteger right) {
            return doBigInteger(BigInteger.valueOf((long) left), right);
        }

        @Specialization
        @TruffleBoundary
        protected final static boolean doLong(final long left, final BigInteger right) {
            return doBigInteger(BigInteger.valueOf(left), right);
        }

        @Specialization
        protected final static boolean doDouble(final double left, final long right) {
            return doDouble(left, (double) right);
        }

        // Additional specialization to speed up eager sends
        @Specialization
        protected final static boolean doObject(final Object left, final Object right) {
            if (left == right) { // must be equal if identical
                return true;
            } else {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {8, 28, 48}, numArguments = 2)
    protected static abstract class PrimNotEqualNode extends AbstractArithmeticPrimitiveNode {
        protected PrimNotEqualNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected boolean neq(long a, long b) {
            return a != b;
        }

        @Specialization
        @TruffleBoundary
        protected boolean neq(BigInteger a, BigInteger b) {
            return !a.equals(b);
        }

        @Specialization
        protected boolean neq(double a, double b) {
            return a != b;
        }

        @Specialization
        protected boolean eq(char receiver, char argument) {
            return receiver != argument;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {10, 30, 50}, numArguments = 2)
    protected static abstract class PrimDivideNode extends AbstractArithmeticPrimitiveNode {
        protected PrimDivideNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        public final static long doLong(final long left, final long right) {
            return left / right;
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doBigInteger(final BigInteger left, final BigInteger right) {
            return reduceIfPossible(left.divide(right));
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doBigInteger(final BigInteger left, final long right) {
            return doBigInteger(left, BigInteger.valueOf(right));
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doLong(final long left, final BigInteger right) {
            return doBigInteger(BigInteger.valueOf(left), right);
        }

        @Specialization
        protected final static long doLong(final long left, final double right) {
            return (long) (left / right);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {11, 31}, numArguments = 2)
    protected static abstract class PrimModNode extends AbstractArithmeticPrimitiveNode {
        protected PrimModNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected long doLong(final long a, final long b) {
            return Math.floorMod(a, b);
        }

        @Specialization
        @TruffleBoundary
        protected Number doBigInteger(final BigInteger a, final BigInteger b) {
            return reduceIfPossible(a.mod(b.abs()));
        }

        @Specialization
        @TruffleBoundary
        protected Number doLong(final long a, final BigInteger b) {
            return doBigInteger(BigInteger.valueOf(a), b);
        }

        @Specialization
        @TruffleBoundary
        protected Number doBigInteger(final BigInteger a, final long b) {
            return doBigInteger(a, BigInteger.valueOf(b));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {12, 32}, numArguments = 2)
    protected static abstract class PrimFloorDivideNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloorDivideNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected final static long doLong(final long a, final long b) {
            return Math.floorDiv(a, b);
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doBigInteger(final BigInteger a, final BigInteger b) {
            return reduceIfPossible(a.divide(b));
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
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {13, 33}, numArguments = 2)
    protected static abstract class PrimQuoNode extends AbstractArithmeticPrimitiveNode {
        protected PrimQuoNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static long doLong(final long a, final long b) {
            return a / b;
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doBigInteger(final BigInteger a, final BigInteger b) {
            return reduceIfPossible(a.divide(b));
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
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {16, 36}, numArguments = 2)
    protected static abstract class PrimBitXorNode extends AbstractArithmeticPrimitiveNode {
        protected PrimBitXorNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static long doLong(final long receiver, final long right) {
            return receiver ^ right;
        }

        @Specialization
        @TruffleBoundary
        protected final static Number doBigInteger(final BigInteger receiver, final BigInteger arg) {
            return reduceIfPossible(receiver.xor(arg));
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
    @SqueakPrimitive(index = 40, numArguments = 2)
    protected static abstract class PrimAsFloatNode extends AbstractArithmeticPrimitiveNode {
        protected PrimAsFloatNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static double doLong(final long receiver) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 51)
    protected static abstract class PrimFloatTruncatedNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloatTruncatedNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static long doLong(final long receiver) {
            return (long) Math.floor(receiver);
        }

        @Specialization
        protected final static long doDouble(final double receiver) {
            return (long) Math.floor(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 53)
    protected static abstract class PrimFloatExponentNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloatExponentNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static long doLong(final long receiver) {
            return Math.getExponent(receiver);
        }

        @Specialization
        protected final static long doDouble(final double receiver) {
            return Math.getExponent(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 54, numArguments = 2)
    protected static abstract class PrimFloatTimesTwoPowerNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloatTimesTwoPowerNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static double doLong(final long receiver, final long argument) {
            return receiver * Math.pow(2, argument);
        }

        @Specialization
        protected final static double doLong(final long receiver, final double argument) {
            return receiver * Math.pow(2, argument);
        }

        @Specialization
        protected final static double doDouble(final double receiver, final double argument) {
            return receiver * Math.pow(2, argument);
        }

        @Specialization
        protected final static double doDouble(final double receiver, final long argument) {
            return receiver * Math.pow(2, argument);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 55)
    protected static abstract class PrimSquareRootNode extends AbstractArithmeticPrimitiveNode {
        protected PrimSquareRootNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static double doLong(final long receiver) {
            return Math.sqrt(receiver);
        }

        @Specialization
        @TruffleBoundary
        protected final static double doBigInteger(final BigInteger receiver) {
            return Math.sqrt(receiver.doubleValue());
        }

        @Specialization
        protected final static double doDouble(final double receiver) {
            return Math.sqrt(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 56)
    protected static abstract class PrimSinNode extends AbstractArithmeticPrimitiveNode {
        protected PrimSinNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static double doDouble(final double rcvr) {
            return Math.sin(rcvr);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 57)
    protected static abstract class PrimArcTanNode extends AbstractArithmeticPrimitiveNode {
        protected PrimArcTanNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static double doDouble(final double a) {
            return Math.atan(a);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 58)
    protected static abstract class PrimLogNNode extends AbstractArithmeticPrimitiveNode {
        protected PrimLogNNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static double doDouble(final double a) {
            return Math.log(a);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 59)
    protected static abstract class PrimExpNode extends AbstractArithmeticPrimitiveNode {
        protected PrimExpNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static double doDouble(final double rcvr) {
            return Math.exp(rcvr);
        }
    }
}
