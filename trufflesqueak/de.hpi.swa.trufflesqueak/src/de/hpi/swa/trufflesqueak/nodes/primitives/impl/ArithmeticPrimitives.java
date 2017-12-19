package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;
import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public final class ArithmeticPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArithmeticPrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {3, 23, 43}, numArguments = 2)
    public static abstract class PrimLessThanNode extends AbstractPrimitiveNode {
        public PrimLessThanNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        boolean lt(int a, int b) {
            return a < b;
        }

        @Specialization
        boolean lt(long a, long b) {
            return a < b;
        }

        @Specialization
        boolean lt(BigInteger a, BigInteger b) {
            return a.compareTo(b) < 0;
        }

        @Specialization
        boolean lt(double a, double b) {
            return a < b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {4, 24, 44}, numArguments = 2)
    public static abstract class PrimGreaterThanNode extends AbstractPrimitiveNode {
        public PrimGreaterThanNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        boolean gt(int a, int b) {
            return a > b;
        }

        @Specialization
        boolean gt(long a, long b) {
            return a > b;
        }

        @Specialization
        boolean gt(BigInteger a, BigInteger b) {
            return a.compareTo(b) > 0;
        }

        @Specialization
        boolean gt(double a, double b) {
            return a > b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {5, 25, 45}, numArguments = 2)
    public static abstract class PrimLessOrEqualNode extends AbstractPrimitiveNode {
        protected PrimLessOrEqualNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        boolean le(int a, int b) {
            return a <= b;
        }

        @Specialization
        boolean le(long a, long b) {
            return a <= b;
        }

        @Specialization
        boolean le(BigInteger a, BigInteger b) {
            return a.compareTo(b) <= 0;
        }

        @Specialization
        boolean le(double a, double b) {
            return a <= b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {6, 26, 46}, numArguments = 2)
    public static abstract class PrimGreaterOrEqualNode extends AbstractPrimitiveNode {
        public PrimGreaterOrEqualNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        boolean ge(int a, int b) {
            return a >= b;
        }

        @Specialization
        boolean ge(long a, long b) {
            return a >= b;
        }

        @Specialization
        boolean ge(BigInteger a, BigInteger b) {
            return a.compareTo(b) >= 0;
        }

        @Specialization
        boolean ge(double a, double b) {
            return a >= b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {7, 27, 47}, numArguments = 2)
    public static abstract class PrimEqualNode extends AbstractPrimitiveNode {
        public PrimEqualNode(CompiledMethodObject method) {
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
        boolean eq(BigInteger a, BigInteger b) {
            return a.equals(b);
        }

        @Specialization
        boolean eq(double a, double b) {
            return a == b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {8, 28, 48}, numArguments = 2)
    public static abstract class PrimNotEqualNode extends AbstractPrimitiveNode {
        public PrimNotEqualNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        boolean neq(int a, int b) {
            return a != b;
        }

        @Specialization
        boolean neq(long a, long b) {
            return a != b;
        }

        @Specialization
        boolean neq(BigInteger a, BigInteger b) {
            return !a.equals(b);
        }

        @Specialization
        boolean neq(double a, double b) {
            return a != b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {10, 30, 50}, numArguments = 2)
    public static abstract class PrimDivideNode extends AbstractPrimitiveNode {
        public PrimDivideNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        int divide(int a, int b) {
            if (a % b != 0) {
                throw new PrimitiveFailed();
            }
            return a / b;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        long divideInt(long a, long b) {
            if (a % b != 0) {
                throw new PrimitiveFailed();
            }
            return Math.toIntExact(a / b);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        long divide(long a, long b) {
            if (a % b != 0) {
                throw new PrimitiveFailed();
            }
            return a / b;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        int divdideInt(BigInteger a, BigInteger b) {
            if (a.mod(b.abs()).compareTo(BigInteger.ZERO) != 0) {
                throw new PrimitiveFailed();
            }
            return a.divide(b).intValueExact();
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        long divide(BigInteger a, BigInteger b) {
            if (a.mod(b.abs()).compareTo(BigInteger.ZERO) != 0) {
                throw new PrimitiveFailed();
            }
            return a.divide(b).longValueExact();
        }

        @Specialization
        BigInteger divBig(BigInteger a, BigInteger b) {
            if (a.mod(b.abs()).compareTo(BigInteger.ZERO) != 0) {
                throw new PrimitiveFailed();
            }
            return a.divide(b);
        }

        @Specialization
        double div(double a, double b) {
            return a / b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {11, 31}, numArguments = 2)
    public static abstract class PrimModNode extends AbstractPrimitiveNode {
        public PrimModNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        int mod(int a, int b) {
            return a % b;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        int modInt(long a, long b) {
            return Math.toIntExact(a % b);
        }

        @Specialization
        long mod(long a, long b) {
            return a % b;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        int modInt(BigInteger a, BigInteger b) {
            return doBigModulo(a, b).intValueExact();
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        long mod(BigInteger a, BigInteger b) {
            return doBigModulo(a, b).longValueExact();
        }

        @Specialization
        BigInteger modBig(BigInteger a, BigInteger b) {
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
    public static abstract class PrimDivNode extends AbstractPrimitiveNode {
        public PrimDivNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        int div(int a, int b) {
            if (a == Integer.MIN_VALUE && b == -1) {
                throw new ArithmeticException();
            }
            return Math.floorDiv(a, b);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        int divInt(long a, long b) {
            if (a == Long.MIN_VALUE && b == -1) {
                throw new ArithmeticException();
            }
            return Math.toIntExact(Math.floorDiv(a, b));
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        long div(long a, long b) {
            if (a == Long.MIN_VALUE && b == -1) {
                throw new ArithmeticException();
            }
            return Math.floorDiv(a, b);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        int divInt(BigInteger a, BigInteger b) {
            return a.divide(b).intValueExact();
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        long div(BigInteger a, BigInteger b) {
            return a.divide(b).longValueExact();
        }

        @Specialization
        BigInteger divBig(BigInteger a, BigInteger b) {
            return a.divide(b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {13, 33}, numArguments = 2)
    public static abstract class PrimQuoNode extends AbstractPrimitiveNode {
        public PrimQuoNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        int quo(int a, int b) {
            return a / b;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        int quoInt(long a, long b) {
            return Math.toIntExact(a / b);
        }

        @Specialization
        long quo(long a, long b) {
            return a / b;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        int quoInt(BigInteger a, BigInteger b) {
            return a.divide(b).intValueExact();
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        long quo(BigInteger a, BigInteger b) {
            return a.divide(b).longValueExact();
        }

        @Specialization
        BigInteger quoBig(BigInteger a, BigInteger b) {
            return a.divide(b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {16, 36}, numArguments = 2)
    public static abstract class PrimBitXorNode extends AbstractPrimitiveNode {
        public PrimBitXorNode(CompiledMethodObject method) {
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
    public static abstract class PrimAsFloatNode extends AbstractPrimitiveNode {
        public PrimAsFloatNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        double asFloat(int v) {
            return v;
        }

        @Specialization
        double asFloat(long v) {
            return v;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 51)
    public static abstract class PrimFloatTruncatedNode extends AbstractPrimitiveNode {
        public PrimFloatTruncatedNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        int truncateToInt(double receiver) {
            return Math.toIntExact((long) Math.floor(receiver));
        }

        @Specialization
        long truncate(double receiver) {
            return (long) Math.floor(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 53)
    public static abstract class PrimFloatExponentNode extends AbstractPrimitiveNode {
        public PrimFloatExponentNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        int exponentAsInt(double receiver) {
            return Math.getExponent(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 54, numArguments = 2)
    public static abstract class PrimFloatTimesTwoPowerNode extends AbstractPrimitiveNode {
        public PrimFloatTimesTwoPowerNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        double calc(double receiver, long argument) {
            return receiver * Math.pow(2, argument);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 55)
    public static abstract class PrimSquareRootNode extends AbstractPrimitiveNode {
        public PrimSquareRootNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        double squareRoot(double a) {
            return Math.sqrt(a);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 56)
    public static abstract class PrimSinNode extends AbstractPrimitiveNode {
        public PrimSinNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        double sin(double a) {
            return Math.sin(a);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 57)
    public static abstract class PrimArcTanNode extends AbstractPrimitiveNode {
        public PrimArcTanNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        double arctan(double a) {
            return Math.atan(a);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 58)
    public static abstract class PrimLogNNode extends AbstractPrimitiveNode {
        public PrimLogNNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        double logn(double a) {
            return Math.log(a);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 59)
    public static abstract class PrimExpNode extends AbstractPrimitiveNode {
        public PrimExpNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        double exp(double a) {
            return Math.exp(a);
        }
    }
}
