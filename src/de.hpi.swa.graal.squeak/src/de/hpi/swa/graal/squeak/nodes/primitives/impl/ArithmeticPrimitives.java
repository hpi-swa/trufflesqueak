package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class ArithmeticPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArithmeticPrimitivesFactory.getFactories();
    }

    public abstract static class AbstractArithmeticPrimitiveNode extends AbstractPrimitiveNode {

        public AbstractArithmeticPrimitiveNode(final CompiledMethodObject method) {
            super(method);
        }

        protected static final boolean isZero(final double value) {
            return value == 0;
        }

        protected static final boolean isIntegralWhenDividedBy(final long a, final long b) {
            return a % b == 0;
        }

        protected static final boolean isMinValueDividedByMinusOne(final long a, final long b) {
            return a == Long.MIN_VALUE && b == -1;
        }
    }

    public abstract static class AbstractArithmeticBinaryPrimitiveNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        public AbstractArithmeticBinaryPrimitiveNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        protected Object doLong(final long a, final long b) {
            throw new PrimitiveFailed(); // SmallInteger + LargeInteger
        }

        @SuppressWarnings("unused")
        protected Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            throw new PrimitiveFailed(); // LargeInteger
        }

        @SuppressWarnings("unused")
        protected Object doDouble(final double a, final double b) {
            throw new PrimitiveFailed(); // SmallFloat64
        }

        @SuppressWarnings("unused")
        protected Object doFloat(final FloatObject a, final FloatObject b) {
            throw new PrimitiveFailed(); // BoxedFloat64
        }

        @Specialization
        protected final Object doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization
        protected final Object doLongDouble(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final Object doLongFloat(final long a, final FloatObject b) {
            return doFloat(asFloatObject(a), b);
        }

        @Specialization
        protected final Object doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @Specialization
        protected final Object doDoubleLong(final double a, final long b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final Object doDoubleFloat(final double a, final FloatObject b) {
            return doFloat(asFloatObject(a), b);
        }

        @Specialization
        protected final Object doFloatLong(final FloatObject a, final long b) {
            return doFloat(a, asFloatObject(b));
        }

        @Specialization
        protected final Object doFloatDouble(final FloatObject a, final double b) {
            return doFloat(a, asFloatObject(b));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {3, 23, 43, 543})
    protected abstract static class PrimLessThanNode extends AbstractArithmeticBinaryPrimitiveNode {
        protected PrimLessThanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final Object doLong(final long a, final long b) {
            return a < b ? code.image.sqTrue : code.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) < 0 ? code.image.sqTrue : code.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doDouble(final double a, final double b) {
            return a < b ? code.image.sqTrue : code.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doFloat(final FloatObject a, final FloatObject b) {
            return a.getValue() < b.getValue() ? code.image.sqTrue : code.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {4, 24, 44, 544})
    protected abstract static class PrimGreaterThanNode extends AbstractArithmeticBinaryPrimitiveNode {
        protected PrimGreaterThanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final Object doLong(final long a, final long b) {
            return a > b ? code.image.sqTrue : code.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) > 0 ? code.image.sqTrue : code.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doDouble(final double a, final double b) {
            return a > b ? code.image.sqTrue : code.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doFloat(final FloatObject a, final FloatObject b) {
            return a.getValue() > b.getValue() ? code.image.sqTrue : code.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {5, 25, 45, 545})
    protected abstract static class PrimLessOrEqualNode extends AbstractArithmeticBinaryPrimitiveNode {
        protected PrimLessOrEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final Object doLong(final long a, final long b) {
            return a <= b ? code.image.sqTrue : code.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) <= 0 ? code.image.sqTrue : code.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doDouble(final double a, final double b) {
            return a <= b ? code.image.sqTrue : code.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doFloat(final FloatObject a, final FloatObject b) {
            return a.getValue() <= b.getValue() ? code.image.sqTrue : code.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {6, 26, 46, 546})
    protected abstract static class PrimGreaterOrEqualNode extends AbstractArithmeticBinaryPrimitiveNode {
        protected PrimGreaterOrEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final Object doLong(final long a, final long b) {
            return a >= b ? code.image.sqTrue : code.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) >= 0 ? code.image.sqTrue : code.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doDouble(final double a, final double b) {
            return a >= b ? code.image.sqTrue : code.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doFloat(final FloatObject a, final FloatObject b) {
            return a.getValue() >= b.getValue() ? code.image.sqTrue : code.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {7, 27, 47, 547})
    protected abstract static class PrimEqualNode extends AbstractArithmeticBinaryPrimitiveNode {
        protected PrimEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final Object doLong(final long a, final long b) {
            return a == b ? code.image.sqTrue : code.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) == 0 ? code.image.sqTrue : code.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doDouble(final double a, final double b) {
            return a == b ? code.image.sqTrue : code.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doFloat(final FloatObject a, final FloatObject b) {
            return a.getValue() == b.getValue() ? code.image.sqTrue : code.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization // Specialization for quick nil checks.
        protected final boolean doNil(final Object a, final NilObject b) {
            return code.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {8, 28, 48, 548})
    protected abstract static class PrimNotEqualNode extends AbstractArithmeticBinaryPrimitiveNode {
        protected PrimNotEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final Object doLong(final long a, final long b) {
            return a != b ? code.image.sqTrue : code.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) != 0 ? code.image.sqTrue : code.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doDouble(final double a, final double b) {
            return a != b ? code.image.sqTrue : code.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doFloat(final FloatObject a, final FloatObject b) {
            return a.getValue() != b.getValue() ? code.image.sqTrue : code.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization // Specialization for quick nil checks.
        protected final boolean doNil(final Object a, final NilObject b) {
            return code.image.sqTrue;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 10)
    protected abstract static class PrimDivideSmallIntegerNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimDivideSmallIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {
                        "isSmallInteger(a)", "isSmallInteger(b)", // both values need to be
                                                                  // SmallInteger
                        "b != 0",                                 // fail on division by zero
                        "isIntegralWhenDividedBy(a, b)"})         // fail if result is not integral
        public static final long doLong(final long a, final long b) {
            return a / b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 11)
    protected abstract static class PrimFloorModSmallIntegerNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloorModSmallIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isSmallInteger(a)", "isSmallInteger(b)"})
        protected long doLong(final long a, final long b) {
            return Math.floorMod(a, b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 12)
    protected abstract static class PrimFloorDivideSmallIntegerNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloorDivideSmallIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        /*
         * The primitive normally fails if argument is not a SmallInteger. Supporting LargeIntegers
         * anyway as it does not change the behavior.
         */

        @Specialization(guards = {"isSmallInteger(a)"})
        protected static final long doLong(final long a, final long b) {
            return Math.floorDiv(a, b);
        }

        @Specialization(guards = {"isSmallInteger(a)"})
        protected final long doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return (long) asLargeInteger(a).floorDivide(b); // if a is SmallInteger, result must be
                                                            // SmallInteger
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 13)
    protected abstract static class PrimQuoSmallIntegerNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimQuoSmallIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0"})
        protected static final long doLong(final long a, final long b) {
            return a / b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 20)
    protected abstract static class PrimRemLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimRemLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0"})
        protected static final long doLong(final long a, final long b) {
            return a / b;
        }

        @Specialization(guards = {"!b.isZero()"})
        protected static final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
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
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 30)
    protected abstract static class PrimDivideLargeIntegerNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimDivideLargeIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {
                        "b != 0",                                   // fail on division by zero
                        "isIntegralWhenDividedBy(a, b)",            // fail if result is not
                                                                    // integral
                        "!isMinValueDividedByMinusOne(a, b)"})      // handle special case
                                                                    // separately
        public static final long doLong(final long a, final long b) {
            return a / b;
        }

        @Specialization(guards = "isMinValueDividedByMinusOne(a, b)") // handle special case:
                                                                      // Long.MIN_VALUE / -1
        protected final Object doLongOverflow(final long a, final long b) {
            return doLargeInteger(asLargeInteger(a), asLargeInteger(b));
        }

        @Specialization(guards = {"!b.isZero()", "a.isIntegralWhenDividedBy(b)"})
        protected static final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
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
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 31)
    protected abstract static class PrimFloorModLargeIntegerNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloorModLargeIntegerNode(final CompiledMethodObject method) {
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
    @SqueakPrimitive(index = 32)
    protected abstract static class PrimFloorDivideLargeIntegerNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloorDivideLargeIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doLong(final long a, final long b) {
            return Math.floorDiv(a, b);
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
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
    @SqueakPrimitive(index = 33)
    protected abstract static class PrimQuoLargeIntegerNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimQuoLargeIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {
                        "b != 0",                                 // fail on division by zero
                        "!isMinValueDividedByMinusOne(a, b)"})     // handle special case separately
        public static final long doLong(final long a, final long b) {
            return a / b;
        }

        @Specialization(guards = "isMinValueDividedByMinusOne(a, b)") // handle special case:
                                                                      // Long.MIN_VALUE / -1
        protected final Object doLongOverflow(final long a, final long b) {
            return doLargeInteger(asLargeInteger(a), asLargeInteger(b));
        }

        @Specialization(guards = "!b.isZero()")
        protected static final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.divide(b);
        }

        @Specialization(guards = "b != 0")
        protected final Object doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 38)
    protected abstract static class PrimFloatAtNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloatAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "index == 1")
        protected static final Object doDoubleHigh(final double receiver, @SuppressWarnings("unused") final long index) {
            final long bits = Double.doubleToRawLongBits(receiver);
            return Integer.toUnsignedLong((int) (bits >> 32));
        }

        @Specialization(guards = "index == 2")
        protected static final Object doDoubleLow(final double receiver, @SuppressWarnings("unused") final long index) {
            final long bits = Double.doubleToRawLongBits(receiver);
            return Integer.toUnsignedLong((int) bits);
        }

        @Specialization(guards = "index == 1")
        protected static final Object doFloatObjectHigh(final FloatObject receiver, @SuppressWarnings("unused") final long index) {
            return receiver.getHigh();
        }

        @Specialization(guards = "index == 2")
        protected static final Object doFloatObjectLow(final FloatObject receiver, @SuppressWarnings("unused") final long index) {
            return receiver.getLow();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 39)
    protected abstract static class PrimFloatAtPutNode extends AbstractArithmeticPrimitiveNode implements TernaryPrimitive {
        protected PrimFloatAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "index == 1")
        protected static final long doFloatHigh(final FloatObject receiver, @SuppressWarnings("unused") final long index, final long value) {
            receiver.setHigh(value);
            return value;
        }

        @Specialization(guards = "index == 2")
        protected static final long doFloatLow(final FloatObject receiver, @SuppressWarnings("unused") final long index, final long value) {
            receiver.setLow(value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected static final double doDouble(final double receiver, final long index, final long value) {
            throw new SqueakException("Cannot modify immediate double value");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 40)
    protected abstract static class PrimAsFloatNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimAsFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final FloatObject doLong(final long receiver) {
            return asFloatObject(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {50, 550})
    protected abstract static class PrimFloatDivideNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloatDivideNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "!isZero(b)")
        protected static final double doDouble(final double a, final double b) {
            return a / b;
        }

        @Specialization(guards = {"b != 0", "isSmallInteger(b)"})
        protected static final double doDoubleLong(final double a, final long b) {
            return doDouble(a, b);
        }

        @Specialization(guards = {"!isZero(b.getValue())"})
        protected final FloatObject doDoubleFloat(final double a, final FloatObject b) {
            return asFloatObject(doDouble(a, b.getValue()));
        }

        @Specialization(guards = "!isZero(b.getValue())")
        protected final FloatObject doFloat(final FloatObject a, final FloatObject b) {
            return asFloatObject(a.getValue() / b.getValue());
        }

        @Specialization(guards = {"b != 0", "isSmallInteger(b)"})
        protected final FloatObject doFloatLong(final FloatObject a, final long b) {
            return asFloatObject(doDouble(a.getValue(), b));
        }

        @Specialization(guards = {"!isZero(b)"})
        protected final FloatObject doFloatDouble(final FloatObject a, final double b) {
            return asFloatObject(doDouble(a.getValue(), b));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {51, 551})
    protected abstract static class PrimFloatTruncatedNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimFloatTruncatedNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final long doDouble(final double receiver) {
            final long truncatedValue = Double.valueOf(receiver).longValue();
            if (isSmallInteger(truncatedValue)) {
                return truncatedValue;
            } else {
                throw new PrimitiveFailed();
            }
        }

        @Specialization
        protected final long doFloatObject(final FloatObject receiver) {
            return doDouble(receiver.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {52, 552})
    protected abstract static class PrimFloatFractionPartNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimFloatFractionPartNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final FloatObject doDouble(final double receiver) {
            return asFloatObject(receiver - Double.valueOf(receiver).longValue());
        }

        @Specialization
        protected final FloatObject doFloatObject(final FloatObject receiver) {
            return doDouble(receiver.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {53, 553})
    protected abstract static class PrimFloatExponentNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimFloatExponentNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doLong(final long receiver) {
            return Math.getExponent(receiver);
        }

        @Specialization
        protected static final long doDouble(final double receiver) {
            return Math.getExponent(receiver);
        }

        @Specialization
        protected static final long doFloat(final FloatObject receiver) {
            return Math.getExponent(receiver.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {54, 554})
    protected abstract static class PrimFloatTimesTwoPowerNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        private static final int UNDERFLOW_LIMIT = FloatObject.EMIN - FloatObject.PRECISION + 1;

        protected PrimFloatTimesTwoPowerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doLong(final long receiver, final long argument) {
            return receiver * Math.pow(2, argument);
        }

        @Specialization
        protected static final double doLongDouble(final long receiver, final double argument) {
            return receiver * Math.pow(2, argument);
        }

        @Specialization
        protected final FloatObject doLongFloat(final long receiver, final FloatObject argument) {
            return asFloatObject(doubleSlow(receiver, argument.getValue()));
        }

        @Specialization(guards = "zeroOrInfinite(receiver)")
        protected static final double doDoubleZeroOrInfinite(final double receiver, @SuppressWarnings("unused") final double argument) {
            return receiver; // See Float>>timesTwoPower:.
        }

        @Specialization(guards = {"!zeroOrInfinite(receiver)", "greaterThanEMAX(argument)"})
        protected static final double doDoubleArgumentsGreaterThanEMAX(final double receiver, @SuppressWarnings("unused") final double argument) {
            // See Float>>timesTwoPower:.
            return receiver * Math.pow(2.0, FloatObject.EMAX) * Math.pow(2, argument - FloatObject.EMAX);
        }

        @Specialization(guards = {"!zeroOrInfinite(receiver)", "!greaterThanEMAX(argument)", "lessThanUNDERFLOWLIMIT(argument)"})
        protected static final double doDoubleArgumentsLessThanUNDERFLOWLIMIT(final double receiver, @SuppressWarnings("unused") final double argument) {
            // See Float>>timesTwoPower:.
            int deltaToUnderflow = Math.max(FloatObject.EMIN - Math.getExponent(argument), UNDERFLOW_LIMIT);
            if (deltaToUnderflow >= 0) {
                deltaToUnderflow = FloatObject.EMIN;
            }
            return receiver * Math.pow(2.0, deltaToUnderflow) * Math.pow(2, argument - deltaToUnderflow);
        }

        @Specialization(guards = {"!zeroOrInfinite(receiver)", "!greaterThanEMAX(argument)", "!lessThanUNDERFLOWLIMIT(argument)"})
        protected static final double doDoubleOtherwise(final double receiver, @SuppressWarnings("unused") final double argument) {
            return receiver * Math.pow(2.0, argument); // See Float>>timesTwoPower:.
        }

        @Specialization(guards = "zeroOrInfinite(receiver)")
        protected static final double doDoubleLongZeroOrInfinite(final double receiver, @SuppressWarnings("unused") final long argument) {
            return receiver; // See Float>>timesTwoPower:.
        }

        @Specialization(guards = {"!zeroOrInfinite(receiver)", "greaterThanEMAX(argument)"})
        protected static final double doDoubleLongArgumentsGreaterThanEMAX(final double receiver, @SuppressWarnings("unused") final long argument) {
            // See Float>>timesTwoPower:.
            return receiver * Math.pow(2.0, FloatObject.EMAX) * Math.pow(2, argument - FloatObject.EMAX);
        }

        @Specialization(guards = {"!zeroOrInfinite(receiver)", "!greaterThanEMAX(argument)", "lessThanUNDERFLOWLIMIT(argument)"})
        protected static final double doDoubleLongArgumentsLessThanUNDERFLOWLIMIT(final double receiver, @SuppressWarnings("unused") final long argument) {
            // See Float>>timesTwoPower:.
            int deltaToUnderflow = Math.max(FloatObject.EMIN - Math.getExponent(argument), UNDERFLOW_LIMIT);
            if (deltaToUnderflow >= 0) {
                deltaToUnderflow = FloatObject.EMIN;
            }
            return receiver * Math.pow(2.0, deltaToUnderflow) * Math.pow(2, argument - deltaToUnderflow);
        }

        @Specialization(guards = {"!zeroOrInfinite(receiver)", "!greaterThanEMAX(argument)", "!lessThanUNDERFLOWLIMIT(argument)"})
        protected static final double doDoubleLongOtherwise(final double receiver, @SuppressWarnings("unused") final long argument) {
            return receiver * Math.pow(2.0, argument); // See Float>>timesTwoPower:.
        }

        @Specialization
        protected final FloatObject doDoubleFloat(final double receiver, final FloatObject argument) {
            return asFloatObject(doubleSlow(receiver, argument.getValue()));
        }

        @Specialization
        protected final FloatObject doFloat(final FloatObject receiver, final FloatObject argument) {
            return asFloatObject(doubleSlow(receiver.getValue(), argument.getValue()));
        }

        @Specialization
        protected final FloatObject doFloatLong(final FloatObject receiver, final long argument) {
            return asFloatObject(doubleSlow(receiver.getValue(), argument));
        }

        @Specialization
        protected final FloatObject doFloatDouble(final FloatObject receiver, final double argument) {
            return asFloatObject(doubleSlow(receiver.getValue(), argument));
        }

        private static double doubleSlow(final double receiver, final double argument) {
            // see Float>>timesTwoPower:
            if (receiver == 0.0 || Double.isInfinite(receiver)) {
                return receiver;
            } else if (argument > FloatObject.EMAX) {
                return receiver * Math.pow(2.0, FloatObject.EMAX) * Math.pow(2, argument - FloatObject.EMAX);
            } else if (argument < UNDERFLOW_LIMIT) {
                int deltaToUnderflow = Math.max(FloatObject.EMIN - Math.getExponent(argument), UNDERFLOW_LIMIT);
                if (deltaToUnderflow >= 0) {
                    deltaToUnderflow = FloatObject.EMIN;
                }
                return receiver * Math.pow(2.0, deltaToUnderflow) * Math.pow(2, argument - deltaToUnderflow);
            } else {
                return receiver * Math.pow(2.0, argument);
            }
        }

        protected static final boolean zeroOrInfinite(final double receiver) {
            return receiver == 0.0 || Double.isInfinite(receiver);
        }

        protected static final boolean greaterThanEMAX(final double argument) {
            return argument > FloatObject.EMAX;
        }

        protected static final boolean lessThanUNDERFLOWLIMIT(final double argument) {
            return argument < UNDERFLOW_LIMIT;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {55, 555})
    protected abstract static class PrimSquareRootNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimSquareRootNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doLong(final long receiver) {
            return Math.sqrt(receiver);
        }

        @Specialization
        protected static final double doLargeInteger(final LargeIntegerObject receiver) {
            return Math.sqrt(receiver.doubleValue());
        }

        @Specialization
        protected static final double doDouble(final double receiver) {
            return Math.sqrt(receiver);
        }

        @Specialization
        protected final FloatObject doFloat(final FloatObject a) {
            return asFloatObject(Math.sqrt(a.getValue()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {56, 556})
    protected abstract static class PrimSinNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimSinNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double rcvr) {
            return Math.sin(rcvr);
        }

        @Specialization
        protected final FloatObject doFloat(final FloatObject a) {
            return asFloatObject(Math.sin(a.getValue()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {57, 557})
    protected abstract static class PrimArcTanNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimArcTanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double a) {
            return Math.atan(a);
        }

        @Specialization
        protected final FloatObject doFloat(final FloatObject a) {
            return asFloatObject(Math.atan(a.getValue()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {58, 558})
    protected abstract static class PrimLogNNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimLogNNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double a) {
            return Math.log(a);
        }

        @Specialization
        protected final FloatObject doFloat(final FloatObject a) {
            return asFloatObject(Math.log(a.getValue()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {59, 559})
    protected abstract static class PrimExpNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimExpNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double receiver) {
            return Math.exp(receiver);
        }

        @Specialization
        protected final FloatObject doFloat(final FloatObject receiver) {
            return asFloatObject(Math.exp(receiver.getValue()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 159)
    protected abstract static class PrimHashMultiplyNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        private static final int HASH_MULTIPLY_CONSTANT = 1664525;
        private static final long HASH_MULTIPLY_MASK = 0xFFFFFFF;

        protected PrimHashMultiplyNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doLargeInteger(final LargeIntegerObject receiver) {
            return doLong(receiver.longValue());
        }

        @Specialization
        protected static final long doLong(final long receiver) {
            return (receiver * HASH_MULTIPLY_CONSTANT) & HASH_MULTIPLY_MASK;
        }
    }
}
