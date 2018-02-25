package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
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

        @Override
        public final Object executeWithArguments(VirtualFrame frame, Object... arguments) {
            try {
                return executeWithArgumentsSpecialized(frame, arguments);
            } catch (ArithmeticException e) {
                throw new PrimitiveFailed();
            }
        }

        @Override
        public final Object executePrimitive(VirtualFrame frame) {
            try {
                return executeArithmeticPrimitive(frame);
            } catch (ArithmeticException e) {
                throw new PrimitiveFailed();
            }
        }

        protected final LargeIntegerObject asLargeInteger(long value) {
            return LargeIntegerObject.valueOf(code, value);
        }

        public abstract Object executeArithmeticPrimitive(VirtualFrame frame);
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {3, 23, 43}, numArguments = 2)
    protected static abstract class PrimLessThanNode extends AbstractArithmeticPrimitiveNode {
        protected PrimLessThanNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static boolean doLong(final long a, final long b) {
            return a < b;
        }

        @Specialization
        protected final static boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) < 0;
        }

        @Specialization
        protected final static boolean doDouble(final double a, final double b) {
            return a < b;
        }

        @Specialization
        protected final boolean doLong(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization
        protected final static boolean doLong(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @Specialization
        protected final static boolean doDouble(final double a, final long b) {
            return doDouble(a, (double) b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {4, 24, 44}, numArguments = 2)
    protected static abstract class PrimGreaterThanNode extends AbstractArithmeticPrimitiveNode {
        protected PrimGreaterThanNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static boolean doLong(final long a, final long b) {
            return a > b;
        }

        @Specialization
        protected final static boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) > 0;
        }

        @Specialization
        protected final static boolean doDouble(final double a, final double b) {
            return a > b;
        }

        @Specialization
        protected final boolean doLong(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization
        protected final static boolean doLong(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @Specialization
        protected final static boolean doDouble(final double a, final long b) {
            return doDouble(a, (double) b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {5, 25, 45}, numArguments = 2)
    protected static abstract class PrimLessOrEqualNode extends AbstractArithmeticPrimitiveNode {
        protected PrimLessOrEqualNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static boolean doLong(final long a, final long b) {
            return a <= b;
        }

        @Specialization
        protected final static boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) <= 0;
        }

        @Specialization
        protected final static boolean doDouble(final double a, final double b) {
            return a <= b;
        }

        @Specialization
        protected final boolean doLong(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization
        protected final static boolean doLong(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @Specialization
        protected final static boolean doDouble(final double a, final long b) {
            return doDouble(a, (double) b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {6, 26, 46}, numArguments = 2)
    protected static abstract class PrimGreaterOrEqualNode extends AbstractArithmeticPrimitiveNode {
        protected PrimGreaterOrEqualNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static boolean doLong(final long a, final long b) {
            return a >= b;
        }

        @Specialization
        protected final static boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) >= 0;
        }

        @Specialization
        protected final static boolean doDouble(final double a, final double b) {
            return a >= b;
        }

        @Specialization
        protected final boolean doLong(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization
        protected final static boolean doLong(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @Specialization
        protected final static boolean doDouble(final double a, final long b) {
            return doDouble(a, (double) b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {7, 27, 47}, numArguments = 2)
    protected static abstract class PrimEqualNode extends AbstractArithmeticPrimitiveNode {
        protected PrimEqualNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static boolean doBoolean(final boolean a, final boolean b) {
            return a == b;
        }

        @Specialization
        protected final static boolean doLong(final long a, final long b) {
            return a == b;
        }

        @Specialization
        protected final static boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) == 0;
        }

        @Specialization
        protected final static boolean doDouble(final double a, final double b) {
            return a == b;
        }

        @Specialization
        protected boolean doChar(char receiver, char argument) {
            return receiver == argument;
        }

        @Specialization
        protected final static boolean doLong(final long a, final double b) {
            return a == b;
        }

        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final double b) {
            return doLargeInteger(a, asLargeInteger((long) b));
        }

        @Specialization
        protected final boolean doDouble(final double a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger((long) a), b);
        }

        @Specialization
        protected final boolean doLong(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization
        protected final static boolean doDouble(final double a, final long b) {
            return doDouble(a, (double) b);
        }

        // Additional specialization to speed up eager sends
        @Specialization
        protected final static boolean doObject(final Object a, final Object b) {
            if (a == b) { // must be equal if identical
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
        protected boolean neq(LargeIntegerObject a, LargeIntegerObject b) {
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

        protected boolean isZero(double value) {
            return value == 0;
        }

        @Specialization(guards = "b != 0")
        public final static long doLong(final long a, final long b) {
            return a / b;
        }

        @Specialization(guards = "!b.isZero()")
        protected final static Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.divide(b);
        }

        @Specialization(guards = "!isZero(b)")
        protected final static double doDouble(final double a, final double b) {
            return a / b;
        }

        @Specialization(guards = "b != 0")
        protected final Object doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @Specialization(guards = "!isZero(b)")
        protected final static double doLargeIntegerDouble(final LargeIntegerObject a, final double b) {
            return doDouble(a.doubleValue(), b);
        }

        @Specialization(guards = "!b.isZero()")
        protected final Object doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization(guards = "!isZero(b)")
        protected final static double doLongDouble(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization(guards = "b != 0")
        protected final static double doDoubleLong(final double a, final long b) {
            return doDouble(a, b);
        }

        @Specialization(guards = "!b.isZero()")
        protected final static double doDoubleLargeInteger(final double a, final LargeIntegerObject b) {
            return doDouble(a, b.doubleValue());
        }

        @SuppressWarnings("unused")
        @Fallback
        public final static long doZeroDivide(final Object a, final Object b) {
            throw new PrimitiveFailed();
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
        protected Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.mod(b);
        }

        @Specialization
        protected Object doLong(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization
        protected Object doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
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
        protected final static Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.divide(b);
        }

        @Specialization
        protected final Object doLong(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {13, 33}, numArguments = 2) // used by SmallInteger and LargePositiveInteger
    protected static abstract class PrimQuoNode extends AbstractArithmeticPrimitiveNode {
        protected PrimQuoNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static long doLong(final long a, final long b) {
            return a / b;
        }

        @Specialization
        protected final static Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.divide(b);
        }

        @Specialization
        protected final Object doLong(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
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
        protected final static long doDouble(final double receiver) {
            return Double.valueOf(receiver).longValue();
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
        protected final static double doLargeInteger(final LargeIntegerObject receiver) {
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
