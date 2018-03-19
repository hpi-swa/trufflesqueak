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
import de.hpi.swa.trufflesqueak.model.NilObject;
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

        protected boolean isZero(final double value) {
            return value == 0;
        }

        protected boolean isIntegralWhenDividedBy(final long a, final long b) {
            return a % b == 0;
        }

        protected final static boolean isMinValueDividedByMinusOne(final long a, final long b) {
            return a == Long.MIN_VALUE && b == -1;
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
        protected final static boolean doLong(final long a, final double b) {
            return a == b;
        }

        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final static boolean doLargeInteger(final LargeIntegerObject a, final double b) {
            return false;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final static boolean doDouble(final double a, final LargeIntegerObject b) {
            return false;
        }

        @Specialization
        protected final boolean doLong(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization
        protected final static boolean doDouble(final double a, final long b) {
            return doDouble(a, (double) b);
        }

        /*
         * nil checks
         */
        @SuppressWarnings("unused")
        @Specialization
        protected final static boolean doNil(final long a, final NilObject b) {
            return false;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final static boolean doNil(final LargeIntegerObject a, final NilObject b) {
            return false;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final static boolean doNil(final double a, final NilObject b) {
            return false;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {8, 28, 48}, numArguments = 2)
    protected static abstract class PrimNotEqualNode extends AbstractArithmeticPrimitiveNode {
        protected PrimNotEqualNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static boolean doBoolean(final boolean a, final boolean b) {
            return a != b;
        }

        @Specialization
        protected final static boolean doLong(final long a, final long b) {
            return a != b;
        }

        @Specialization
        protected final static boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return !a.equals(b);
        }

        @Specialization
        protected final static boolean doDouble(final double a, final double b) {
            return a != b;
        }

        @Specialization
        protected final static boolean doChar(final char receiver, final char argument) {
            return receiver != argument;
        }

        @Specialization
        protected final static boolean doLong(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final static boolean doDouble(final double a, final long b) {
            return doDouble(a, (double) b);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final static boolean doDouble(final double a, final LargeIntegerObject b) {
            return true;
        }

        @Specialization
        protected final boolean doLong(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));

        }

        @SuppressWarnings("unused")
        @Specialization
        protected final static boolean doLargeInteger(final LargeIntegerObject a, final double b) {
            return true;
        }

        /*
         * nil checks
         */
        @SuppressWarnings("unused")
        @Specialization
        protected final static boolean doNil(final long a, final NilObject b) {
            return true;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final static boolean doNil(final LargeIntegerObject a, final NilObject b) {
            return true;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final static boolean doNil(final double a, final NilObject b) {
            return true;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 10, numArguments = 2)
    protected static abstract class PrimDivideSmallIntegerNode extends AbstractArithmeticPrimitiveNode {
        protected PrimDivideSmallIntegerNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {
                        "isSmallInteger(a)", "isSmallInteger(b)", // both values need to be SmallInteger
                        "b != 0",                                 // fail on division by zero
                        "isIntegralWhenDividedBy(a, b)"})         // fail if result is not integral
        public final static long doLong(final long a, final long b) {
            return a / b;
        }

        @SuppressWarnings("unused")
        @Fallback
        public final static long doZeroDivide(final Object a, final Object b) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 11, numArguments = 2)
    protected static abstract class PrimFloorModSmallIntegerNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloorModSmallIntegerNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isSmallInteger(a)", "isSmallInteger(a)"})
        protected long doLong(final long a, final long b) {
            return Math.floorMod(a, b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 12, numArguments = 2)
    protected static abstract class PrimFloorDivideSmallIntegerNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloorDivideSmallIntegerNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isSmallInteger(a)", "isSmallInteger(b)"})
        protected final static long doLong(final long a, final long b) {
            return Math.floorDiv(a, b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 13, numArguments = 2)
    protected static abstract class PrimQuoSmallIntegerNode extends AbstractArithmeticPrimitiveNode {
        protected PrimQuoSmallIntegerNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0"})
        protected final static long doLong(final long a, final long b) {
            return a / b;
        }

        @SuppressWarnings("unused")
        @Fallback
        public final static long doZeroDivide(final Object a, final Object b) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 20, numArguments = 2)
    protected static abstract class PrimRemLargeIntegersNode extends AbstractArithmeticPrimitiveNode {
        protected PrimRemLargeIntegersNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0"})
        protected final static long doLong(final long a, final long b) {
            return a / b;
        }

        @Specialization(guards = {"!b.isZero()"})
        protected final static Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.remainder(b);
        }

        @Specialization(guards = {"!b.isZero()"})
        protected final Object doLong(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization(guards = {"b != 0"})
        protected final Object doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @SuppressWarnings("unused")
        @Fallback
        public final static long doZeroDivide(final Object a, final Object b) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 30, numArguments = 2)
    protected static abstract class PrimDivideLargeIntegerNode extends AbstractArithmeticPrimitiveNode {
        protected PrimDivideLargeIntegerNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {
                        "b != 0",                                   // fail on division by zero
                        "isIntegralWhenDividedBy(a, b)",            // fail if result is not integral
                        "!isMinValueDividedByMinusOne(a, b)"})      // handle special case separately
        public final static long doLong(final long a, final long b) {
            return a / b;
        }

        @Specialization(guards = "isMinValueDividedByMinusOne(a, b)") // handle special case: Long.MIN_VALUE / -1
        protected final Object doLongOverflow(final long a, final long b) {
            return doLargeInteger(asLargeInteger(a), asLargeInteger(b));
        }

        @Specialization(guards = {"!b.isZero()", "a.isIntegralWhenDividedBy(b)"})
        protected final static Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.divide(b);
        }

        @Specialization(guards = {"b != 0", "a.isIntegralWhenDividedBy(asLargeInteger(b))"})
        protected final Object doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @Specialization(guards = {"!b.isZero()", "asLargeInteger(a).isIntegralWhenDividedBy(b)"})
        protected final Object doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @SuppressWarnings("unused")
        @Fallback
        public final static long doZeroDivide(final Object a, final Object b) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 31, numArguments = 2)
    protected static abstract class PrimFloorModLargeIntegerNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloorModLargeIntegerNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"!isSmallInteger(a)"})
        protected long doLong(final long a, final long b) {
            return Math.floorMod(a, b);
        }

        @Specialization
        protected Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.floorMod(b);
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
    @SqueakPrimitive(index = 32, numArguments = 2)
    protected static abstract class PrimFloorDivideLargeIntegerNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloorDivideLargeIntegerNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"!isSmallInteger(a)", "!isSmallInteger(b)"})
        protected final static long doLong(final long a, final long b) {
            return Math.floorDiv(a, b);
        }

        @Specialization
        protected final static Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.floorDivide(b);
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
    @SqueakPrimitive(index = 33, numArguments = 2)
    protected static abstract class PrimQuoLargeIntegerNode extends AbstractArithmeticPrimitiveNode {
        protected PrimQuoLargeIntegerNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {
                        "b != 0",                                 // fail on division by zero
                        "isMinValueDividedByMinusOne(a, b)"})     // handle special case separately
        public final static long doLong(final long a, final long b) {
            return a / b;
        }

        @Specialization(guards = "isMinValueDividedByMinusOne(a, b)") // handle special case: Long.MIN_VALUE / -1
        protected final Object doLongOverflow(final long a, final long b) {
            return doLargeInteger(asLargeInteger(a), asLargeInteger(b));
        }

        @Specialization(guards = "!b.isZero()")
        protected final static Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.divide(b);
        }

        @Specialization(guards = "b != 0")
        protected final Object doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @SuppressWarnings("unused")
        @Fallback
        public final static long doZeroDivide(final Object a, final Object b) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 38, numArguments = 2)
    protected static abstract class PrimFloatAtNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloatAtNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static long doDouble(final double receiver, final long index) {
            long doubleBits = Double.doubleToLongBits(receiver);
            if (index == 1) {
                if (receiver >= 0) {
                    return Math.abs(doubleBits >> 32);
                } else {
                    return ~Math.abs(doubleBits >> 32) + 1 & 0xffffffffL;
                }
            } else if (index == 2) {
                return Math.abs((int) doubleBits);
            } else {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 39, numArguments = 3)
    protected static abstract class PrimFloatAtPutNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloatAtPutNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static double doDouble(final double receiver, final long index, final long value) {
            long doubleBits = Double.doubleToLongBits(receiver);
            if (index == 1) {
                return Double.longBitsToDouble(value >> 32 | (int) doubleBits);
            } else if (index == 2) {
                return Double.longBitsToDouble(doubleBits & 0xffffffff00000000L | ((Long) value).intValue());
            } else {
                throw new PrimitiveFailed();
            }
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
    @SqueakPrimitive(index = 50, numArguments = 2)
    protected static abstract class PrimFloatDivideNode extends AbstractArithmeticPrimitiveNode {
        protected PrimFloatDivideNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "!isZero(b)")
        protected final static double doDouble(final double a, final double b) {
            return a / b;
        }

        @Specialization(guards = {"b != 0", "isSmallInteger(b)"})
        protected final static double doDoubleLong(final double a, final long b) {
            return doDouble(a, b);
        }

        @SuppressWarnings("unused")
        @Fallback
        public final static long doZeroDivide(final Object a, final Object b) {
            throw new PrimitiveFailed();
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
