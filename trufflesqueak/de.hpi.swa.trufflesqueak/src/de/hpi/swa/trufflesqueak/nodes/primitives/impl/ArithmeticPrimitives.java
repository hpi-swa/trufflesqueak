package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

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
            if (value.bitLength() < Integer.SIZE - 1) {
                return value.intValue();
            } else if (value.bitLength() < Long.SIZE - 1) {
                return value.longValue();
            } else {
                return value;
            }
        }

        @TruffleBoundary
        protected static final Number reduceIfPossible(long value) {
            if (Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE) {
                return (int) value;
            } else {
                return value;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {3, 23, 43}, numArguments = 2)
    protected static abstract class PrimLessThanNode extends AbstractArithmeticPrimitiveNode {
        protected PrimLessThanNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected boolean lt(int a, int b) {
            return a < b;
        }

        @Specialization
        protected boolean lt(long a, long b) {
            return a < b;
        }

        @Specialization
        protected boolean lt(BigInteger a, BigInteger b) {
            return a.compareTo(b) < 0;
        }

        @Specialization
        protected boolean lt(double a, double b) {
            return a < b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {4, 24, 44}, numArguments = 2)
    protected static abstract class PrimGreaterThanNode extends AbstractArithmeticPrimitiveNode {
        protected PrimGreaterThanNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected boolean gt(int a, int b) {
            return a > b;
        }

        @Specialization
        protected boolean gt(long a, long b) {
            return a > b;
        }

        @Specialization
        protected boolean gt(BigInteger a, BigInteger b) {
            return a.compareTo(b) > 0;
        }

        @Specialization
        protected boolean gt(double a, double b) {
            return a > b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {5, 25, 45}, numArguments = 2)
    protected static abstract class PrimLessOrEqualNode extends AbstractArithmeticPrimitiveNode {
        protected PrimLessOrEqualNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected boolean le(int a, int b) {
            return a <= b;
        }

        @Specialization
        protected boolean le(long a, long b) {
            return a <= b;
        }

        @Specialization
        protected boolean le(BigInteger a, BigInteger b) {
            return a.compareTo(b) <= 0;
        }

        @Specialization
        protected boolean le(double a, double b) {
            return a <= b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {6, 26, 46}, numArguments = 2)
    protected static abstract class PrimGreaterOrEqualNode extends AbstractArithmeticPrimitiveNode {
        protected PrimGreaterOrEqualNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected boolean ge(int a, int b) {
            return a >= b;
        }

        @Specialization
        protected boolean ge(long a, long b) {
            return a >= b;
        }

        @Specialization
        protected boolean ge(BigInteger a, BigInteger b) {
            return a.compareTo(b) >= 0;
        }

        @Specialization
        protected boolean ge(double a, double b) {
            return a >= b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {7, 27, 47}, numArguments = 2)
    protected static abstract class PrimEqualNode extends AbstractArithmeticPrimitiveNode {
        protected PrimEqualNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected boolean eq(int receiver, int argument) {
            return receiver == argument;
        }

        @Specialization
        protected boolean eq(long receiver, long argument) {
            return receiver == argument;
        }

        @Specialization
        protected boolean eq(BigInteger a, BigInteger b) {
            return a.equals(b);
        }

        @Specialization
        protected boolean eq(double a, double b) {
            return a == b;
        }

        @Specialization
        protected boolean eq(char receiver, char argument) {
            return receiver == argument;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {8, 28, 48}, numArguments = 2)
    protected static abstract class PrimNotEqualNode extends AbstractArithmeticPrimitiveNode {
        protected PrimNotEqualNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected boolean neq(int a, int b) {
            return a != b;
        }

        @Specialization
        protected boolean neq(long a, long b) {
            return a != b;
        }

        @Specialization
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

        @Specialization(rewriteOn = ArithmeticException.class)
        protected int divide(int a, int b) {
            return a / b;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected long divideInt(long a, long b) {
            return Math.toIntExact(a / b);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected long divide(long a, long b) {
            return a / b;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected int divdideInt(BigInteger a, BigInteger b) {
            return a.divide(b).intValueExact();
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected long divide(BigInteger a, BigInteger b) {
            return a.divide(b).longValueExact();
        }

        @Specialization
        protected BigInteger divBig(BigInteger a, BigInteger b) {
            return a.divide(b);
        }

        @Specialization
        protected double div(double a, double b) {
            return a / b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {11, 31}, numArguments = 2)
    protected static abstract class PrimModNode extends AbstractArithmeticPrimitiveNode {
        protected PrimModNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected int mod(int a, int b) {
            return a % b;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected int modInt(long a, long b) {
            return Math.toIntExact(a % b);
        }

        @Specialization
        protected long mod(long a, long b) {
            return a % b;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected int modInt(BigInteger a, BigInteger b) {
            return doBigModulo(a, b).intValueExact();
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected long mod(BigInteger a, BigInteger b) {
            return doBigModulo(a, b).longValueExact();
        }

        @Specialization
        protected BigInteger modBig(BigInteger a, BigInteger b) {
            return doBigModulo(a, b);
        }

        private static BigInteger doBigModulo(BigInteger a, BigInteger b) {
            BigInteger mod = a.mod(b.abs());
            if (a.signum() + b.signum() <= 0) {
                return mod.negate();
            } else {
                return mod;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {12, 32}, numArguments = 2)
    protected static abstract class PrimFloorDivideNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloorDivideNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected int div(int a, int b) {
            return Math.floorDiv(a, b);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected int divInt(long a, long b) {
            return Math.toIntExact(Math.floorDiv(a, b));
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected long div(long a, long b) {
            return Math.floorDiv(a, b);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected int divInt(BigInteger a, BigInteger b) {
            return a.divide(b).intValueExact();
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected long div(BigInteger a, BigInteger b) {
            return a.divide(b).longValueExact();
        }

        @Specialization
        protected BigInteger divBig(BigInteger a, BigInteger b) {
            return a.divide(b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {13, 33}, numArguments = 2)
    protected static abstract class PrimQuoNode extends AbstractArithmeticPrimitiveNode {
        protected PrimQuoNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected int quo(int a, int b) {
            return a / b;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected int quoInt(long a, long b) {
            return Math.toIntExact(a / b);
        }

        @Specialization
        protected long quo(long a, long b) {
            return a / b;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected int quoInt(BigInteger a, BigInteger b) {
            return a.divide(b).intValueExact();
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected long quo(BigInteger a, BigInteger b) {
            return a.divide(b).longValueExact();
        }

        @Specialization
        protected BigInteger quoBig(BigInteger a, BigInteger b) {
            return a.divide(b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {16, 36}, numArguments = 2)
    protected static abstract class PrimBitXorNode extends AbstractArithmeticPrimitiveNode {
        protected PrimBitXorNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected int bitOr(int receiver, int arg) {
            return receiver ^ arg;
        }

        @Specialization
        protected long bitOr(long receiver, long arg) {
            return receiver ^ arg;
        }

        @Specialization
        protected BigInteger bitAnd(BigInteger receiver, BigInteger arg) {
            return receiver.xor(arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 40, numArguments = 2)
    protected static abstract class PrimAsFloatNode extends AbstractArithmeticPrimitiveNode {
        protected PrimAsFloatNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected double asFloat(int v) {
            return v;
        }

        @Specialization
        protected double asFloat(long v) {
            return v;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 51)
    protected static abstract class PrimFloatTruncatedNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloatTruncatedNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected int truncateToInt(double receiver) {
            return Math.toIntExact((long) Math.floor(receiver));
        }

        @Specialization
        protected long truncate(double receiver) {
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
        protected int exponentAsInt(double receiver) {
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
        protected double calc(double receiver, long argument) {
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
        protected double squareRoot(double a) {
            return Math.sqrt(a);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 56)
    protected static abstract class PrimSinNode extends AbstractArithmeticPrimitiveNode {
        protected PrimSinNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected double sin(double a) {
            return Math.sin(a);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 57)
    protected static abstract class PrimArcTanNode extends AbstractArithmeticPrimitiveNode {
        protected PrimArcTanNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected double arctan(double a) {
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
        protected double logn(double a) {
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
        protected double exp(double a) {
            return Math.exp(a);
        }
    }
}
