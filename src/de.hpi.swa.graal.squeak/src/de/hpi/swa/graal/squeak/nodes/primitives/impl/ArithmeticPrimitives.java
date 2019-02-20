package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.nodes.SqueakArithmeticTypes;
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class ArithmeticPrimitives extends AbstractPrimitiveFactoryHolder {

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {1, 21, 41, 541})
    public abstract static class PrimAddNode extends AbstractArithmeticPrimitiveWithNumericResultNode implements BinaryPrimitive {
        public PrimAddNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization(rewriteOn = ArithmeticException.class)
        protected final long doLong(final long a, final long b) {
            return Math.addExact(a, b);
        }

        @Specialization
        protected final Object doLongWithOverflow(final long a, final long b) {
            return doLargeInteger(asLargeInteger(a), asLargeInteger(b));
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.add(b);
        }

        @Specialization(guards = "is64bit(a)")
        protected static final double doLongDouble64bit(final long a, final double b) {
            return doDouble64bit(a, b);
        }

        @Specialization(guards = "is64bit(a)")
        protected static final double doDoubleLong64bit(final double a, final long b) {
            return doDouble64bit(a, b);
        }

        @Specialization(guards = "is64bit(a)")
        protected static final double doDouble64bit(final double a, final double b) {
            return a + b;
        }

        @Specialization(guards = "!is64bit(a)")
        protected final FloatObject doLongDouble32bit(final long a, final double b) {
            return doDouble32bit(a, b);
        }

        @Specialization(guards = "!is64bit(a)")
        protected final FloatObject doDoubleLong32bit(final double a, final long b) {
            return doDouble32bit(a, b);
        }

        @Specialization(guards = "!is64bit(a)")
        protected final FloatObject doDouble32bit(final double a, final double b) {
            return asFloatObject(doDouble64bit(a, b));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {2, 22, 42, 542})
    public abstract static class PrimSubstractNode extends AbstractArithmeticPrimitiveWithNumericResultNode implements BinaryPrimitive {
        public PrimSubstractNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization(rewriteOn = ArithmeticException.class)
        protected final long doLong(final long a, final long b) {
            return Math.subtractExact(a, b);
        }

        @Specialization
        protected final Object doLongWithOverflow(final long a, final long b) {
            return doLargeInteger(asLargeInteger(a), asLargeInteger(b));
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.subtract(b);
        }

        @Specialization(guards = "is64bit(a)")
        protected static final double doLongDouble64bit(final long a, final double b) {
            return doDouble64bit(a, b);
        }

        @Specialization(guards = "is64bit(a)")
        protected static final double doDoubleLong64bit(final double a, final long b) {
            return doDouble64bit(a, b);
        }

        @Specialization(guards = "is64bit(a)")
        protected static final double doDouble64bit(final double a, final double b) {
            return a - b;
        }

        @Specialization(guards = "!is64bit(a)")
        protected final FloatObject doLongDouble32bit(final long a, final double b) {
            return doDouble32bit(a, b);
        }

        @Specialization(guards = "!is64bit(a)")
        protected final FloatObject doDoubleLong32bit(final double a, final long b) {
            return doDouble32bit(a, b);
        }

        @Specialization(guards = "!is64bit(a)")
        protected final FloatObject doDouble32bit(final double a, final double b) {
            return asFloatObject(doDouble64bit(a, b));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {3, 23, 43, 543})
    protected abstract static class PrimLessThanNode extends AbstractArithmeticPrimitiveWithBooleanResultNode implements BinaryPrimitive {
        protected PrimLessThanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final boolean doLong(final long a, final long b) {
            return a < b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Override
        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) < 0 ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doLongDouble(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final boolean doDoubleLong(final double a, final long b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final boolean doDouble(final double a, final double b) {
            return a < b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {4, 24, 44, 544})
    protected abstract static class PrimGreaterThanNode extends AbstractArithmeticPrimitiveWithBooleanResultNode implements BinaryPrimitive {
        protected PrimGreaterThanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final boolean doLong(final long a, final long b) {
            return a > b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Override
        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) > 0 ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doLongDouble(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final boolean doDoubleLong(final double a, final long b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final boolean doDouble(final double a, final double b) {
            return a > b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {5, 25, 45, 545})
    protected abstract static class PrimLessOrEqualNode extends AbstractArithmeticPrimitiveWithBooleanResultNode implements BinaryPrimitive {
        protected PrimLessOrEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final boolean doLong(final long a, final long b) {
            return a <= b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Override
        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) <= 0 ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doLongDouble(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final boolean doDoubleLong(final double a, final long b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final boolean doDouble(final double a, final double b) {
            return a <= b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {6, 26, 46, 546})
    protected abstract static class PrimGreaterOrEqualNode extends AbstractArithmeticPrimitiveWithBooleanResultNode implements BinaryPrimitive {
        protected PrimGreaterOrEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final boolean doLong(final long a, final long b) {
            return a >= b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Override
        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) >= 0 ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doLongDouble(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final boolean doDoubleLong(final double a, final long b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final boolean doDouble(final double a, final double b) {
            return a >= b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {7, 27, 47, 547})
    protected abstract static class PrimEqualNode extends AbstractArithmeticPrimitiveWithBooleanResultNode implements BinaryPrimitive {
        protected PrimEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final boolean doLong(final long a, final long b) {
            return a == b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Override
        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) == 0 ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "isAnExactFloat(a)")
        protected final boolean doLongExactDouble(final long a, final double b) {
            return doDouble(a, b);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isAnExactFloat(a)")
        protected final boolean doLongNotExactDouble(final long a, final double b) {
            return method.image.sqFalse;
        }

        @Specialization
        protected final boolean doDoubleLongExact(final double a, final long b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final boolean doDouble(final double a, final double b) {
            return a == b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {8, 28, 48, 548})
    protected abstract static class PrimNotEqualNode extends AbstractArithmeticPrimitiveWithBooleanResultNode implements BinaryPrimitive {
        protected PrimNotEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final boolean doLong(final long a, final long b) {
            return a != b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Override
        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) != 0 ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "isAnExactFloat(a)")
        protected final boolean doLongExactDouble(final long a, final double b) {
            return doDouble(a, b);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isAnExactFloat(a)")
        protected final boolean doLongNotExactDouble(final long a, final double b) {
            return method.image.sqTrue;
        }

        @Specialization
        protected final boolean doDoubleLongExact(final double a, final long b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final boolean doDouble(final double a, final double b) {
            return a != b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {9, 29, 49, 549})
    public abstract static class PrimMultiplyNode extends AbstractArithmeticPrimitiveWithNumericResultNode implements BinaryPrimitive {
        public PrimMultiplyNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization(rewriteOn = ArithmeticException.class)
        protected final long doLong(final long a, final long b) {
            return Math.multiplyExact(a, b);
        }

        @Specialization
        protected final Object doLongWithOverflow(final long a, final long b) {
            return doLargeInteger(asLargeInteger(a), asLargeInteger(b));
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.multiply(b);
        }

        @Specialization(guards = "is64bit(a)")
        protected static final double doLongDouble64bit(final long a, final double b) {
            return doDouble64bit(a, b);
        }

        @Specialization(guards = "is64bit(a)")
        protected static final double doDoubleLong64bit(final double a, final long b) {
            return doDouble64bit(a, b);
        }

        @Specialization(guards = "is64bit(a)")
        protected static final double doDouble64bit(final double a, final double b) {
            return a * b;
        }

        @Specialization(guards = "!is64bit(a)")
        protected final FloatObject doLongDouble32bit(final long a, final double b) {
            return doDouble32bit(a, b);
        }

        @Specialization(guards = "!is64bit(a)")
        protected final FloatObject doDoubleLong32bit(final double a, final long b) {
            return doDouble32bit(a, b);
        }

        @Specialization(guards = "!is64bit(a)")
        protected final FloatObject doDouble32bit(final double a, final double b) {
            return asFloatObject(doDouble64bit(a, b));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {10, 30, 50, 550})
    protected abstract static class PrimDivideNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimDivideNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0", "isIntegralWhenDividedBy(a, b)", "!isOverflowDivision(a, b)"})
        public static final long doLong(final long a, final long b) {
            return a / b;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isOverflowDivision(a, b)")
        protected final Object doLongWithOverflow(final long a, final long b) {
            return LargeIntegerObject.createLongMinOverflowResult(method.image);
        }

        @Specialization(guards = {"!b.isZero()", "a.isIntegralWhenDividedBy(b)"})
        protected static final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.divide(b);
        }

        @Specialization(guards = {"is64bit(a)", "!isZero(b)"})
        protected static final double doLongDouble64bit(final long a, final double b) {
            return doDouble64bit(a, b);
        }

        @Specialization(guards = {"is64bit(a)", "b != 0"})
        protected static final double doDoubleLong64bit(final double a, final long b) {
            return doDouble64bit(a, b);
        }

        @Specialization(guards = {"is64bit(a)", "!isZero(b)"})
        protected static final double doDouble64bit(final double a, final double b) {
            return a / b;
        }

        @Specialization(guards = {"!is64bit(a)", "!isZero(b)"})
        protected final FloatObject doLongDouble32bit(final long a, final double b) {
            return doDouble32bit(a, b);
        }

        @Specialization(guards = {"!is64bit(a)", "b != 0"})
        protected final FloatObject doDoubleLong32bit(final double a, final long b) {
            return doDouble32bit(a, b);
        }

        @Specialization(guards = {"!is64bit(a)", "!isZero(b)"})
        protected final FloatObject doDouble32bit(final double a, final double b) {
            return asFloatObject(doDouble64bit(a, b));
        }

        @Specialization(guards = {"!b.isZero()", "b.fitsIntoLong()", "isIntegralWhenDividedBy(a, b.longValue())", "!isOverflowDivision(a, b.longValue())"})
        protected static final Object doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValue());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"b.fitsIntoLong()", "isOverflowDivision(a, b.longValue())"})
        protected final LargeIntegerObject doLongLargeIntegerAsLongWithOverflow(final long a, final LargeIntegerObject b) {
            return LargeIntegerObject.createLongMinOverflowResult(method.image);
        }

        @Specialization(guards = {"!b.isZero()", "!b.fitsIntoLong()", "asLargeInteger(a).isIntegralWhenDividedBy(b)"})
        protected final Object doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization(guards = {"b != 0", "a.isIntegralWhenDividedBy(asLargeInteger(b))"})
        protected final Object doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 11)
    protected abstract static class PrimFloorModNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloorModNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0"})
        protected long doLong(final long a, final long b) {
            return Math.floorMod(a, b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 12)
    protected abstract static class PrimFloorDivideNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloorDivideNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0"})
        protected static final long doLong(final long a, final long b) {
            return Math.floorDiv(a, b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 13)
    protected abstract static class PrimQuoNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimQuoNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0"})
        protected static final long doLong(final long a, final long b) {
            return a / b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 14)
    public abstract static class PrimBitAndNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        public PrimBitAndNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doLong(final long receiver, final long arg) {
            return receiver & arg;
        }

        @Specialization(guards = {"arg.fitsIntoLong()"})
        protected static final Object doLongLargeAsLong(final long receiver, final LargeIntegerObject arg) {
            return doLong(receiver, arg.longValue());
        }

        @Specialization(guards = {"!arg.fitsIntoLong()"})
        protected final Object doLongLarge(final long receiver, final LargeIntegerObject arg) {
            return asLargeInteger(receiver).and(arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 15)
    public abstract static class PrimBitOrNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        public PrimBitOrNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doLong(final long receiver, final long arg) {
            return receiver | arg;
        }

        @Specialization(guards = {"arg.fitsIntoLong()"})
        protected static final Object doLongLargeAsLong(final long receiver, final LargeIntegerObject arg) {
            return doLong(receiver, arg.longValue());
        }

        @Specialization(guards = {"!arg.fitsIntoLong()"})
        protected final Object doLongLarge(final long receiver, final LargeIntegerObject arg) {
            return asLargeInteger(receiver).or(arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 16)
    protected abstract static class PrimBitXorNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimBitXorNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doLong(final long receiver, final long arg) {
            return receiver ^ arg;
        }

        @Specialization(guards = {"arg.fitsIntoLong()"})
        protected static final Object doLongLargeAsLong(final long receiver, final LargeIntegerObject arg) {
            return doLong(receiver, arg.longValue());
        }

        @Specialization(guards = {"!arg.fitsIntoLong()"})
        protected final Object doLongLarge(final long receiver, final LargeIntegerObject arg) {
            return asLargeInteger(receiver).xor(arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 17)
    public abstract static class PrimBitShiftNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {

        public PrimBitShiftNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"arg >= 0", "!isLShiftLongOverflow(receiver, arg)"})
        protected static final long doLong(final long receiver, final long arg) {
            return receiver << arg;
        }

        @Specialization(guards = {"arg < 0", "isArgInLongSizeRange(arg)"})
        protected static final long doLongNegativeLong(final long receiver, final long arg) {
            // The result of a right shift can only become smaller than the receiver and 0 or -1 at
            // minimum, so no BigInteger needed here
            return receiver >> -arg;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"arg < 0", "!isArgInLongSizeRange(arg)", "receiver >= 0"})
        protected static final long doLongTooBigPositive(final long receiver, final long arg) {
            return 0L;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"arg < 0", "!isArgInLongSizeRange(arg)", "receiver < 0"})
        protected static final long doLongTooBigNegative(final long receiver, final long arg) {
            return -1L;
        }

        protected static final boolean isLShiftLongOverflow(final long receiver, final long arg) {
            // -1 needed, because we do not want to shift a positive long into negative long (most
            // significant bit indicates positive/negative)
            return Long.numberOfLeadingZeros(receiver) - 1 < arg;
        }

        protected static final boolean isArgInLongSizeRange(final long negativeValue) {
            return -negativeValue < Long.SIZE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 20)
    protected abstract static class PrimRemLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimRemLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0"})
        protected static final long doLong(final long a, final long b) {
            return a % b;
        }

        @Specialization(guards = {"!b.isZero()"})
        protected static final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.remainder(b);
        }

        @Specialization(guards = {"!b.isZero()", "b.fitsIntoLong()"})
        protected static final long doLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValue());
        }

        @Specialization(guards = {"!b.isZero()", "!b.fitsIntoLong()"})
        protected final Object doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization(guards = {"b != 0"})
        protected final Object doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 31)
    protected abstract static class PrimFloorModLargeIntegerNode extends AbstractArithmeticPrimitiveWithNumericResultNode implements BinaryPrimitive {
        protected PrimFloorModLargeIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected long doLong(final long a, final long b) {
            return Math.floorMod(a, b);
        }

        @Override
        @Specialization
        protected Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.floorMod(b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 32)
    protected abstract static class PrimFloorDivideLargeIntegerNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloorDivideLargeIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0", "!isOverflowDivision(a, b)"})
        protected static final Object doLong(final long a, final long b) {
            return Math.floorDiv(a, b);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isOverflowDivision(a, b)")
        protected final LargeIntegerObject doLongWithOverflow(final long a, final long b) {
            return LargeIntegerObject.createLongMinOverflowResult(method.image);
        }

        @Specialization(guards = "!b.isZero()")
        protected static final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.floorDivide(b);
        }

        @Specialization(guards = {"!b.isZero()", "b.fitsIntoLong()", "!isOverflowDivision(a, b.longValue())"})
        protected static final Object doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValue());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"b.fitsIntoLong()", "isOverflowDivision(a, b.longValue())"})
        protected final LargeIntegerObject doLongLargeIntegerAsLongWithOverflow(final long a, final LargeIntegerObject b) {
            return LargeIntegerObject.createLongMinOverflowResult(method.image);
        }

        @Specialization(guards = {"!b.isZero()", "!b.fitsIntoLong()"})
        protected final Object doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization(guards = "b != 0")
        protected final Object doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 33)
    protected abstract static class PrimQuoLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimQuoLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0", "!isOverflowDivision(a, b)"})
        public static final long doLong(final long a, final long b) {
            return a / b;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isOverflowDivision(a, b)")
        protected final LargeIntegerObject doLongWithOverflow(final long a, final long b) {
            return LargeIntegerObject.createLongMinOverflowResult(method.image);
        }

        @Specialization(guards = {"!b.isZero()", "b.fitsIntoLong()", "!isOverflowDivision(a, b.longValue())"})
        protected static final long doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValue());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"b.fitsIntoLong()", "isOverflowDivision(a, b.longValue())"})
        protected final LargeIntegerObject doLongLargeIntegerWithOverflow(final long a, final LargeIntegerObject b) {
            return LargeIntegerObject.createLongMinOverflowResult(method.image);
        }

        @Specialization(guards = {"!b.isZero()", "!b.fitsIntoLong()"})
        protected final Object doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
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

    // Squeak/Smalltalk uses LargeIntegers plugin for bit operations instead of primitives 34 to 37.

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 38)
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
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 39)
    protected abstract static class PrimFloatAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitive {
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
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 40)
    protected abstract static class PrimAsFloatNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimAsFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "is64bit(receiver)")
        protected static final double doLong64bit(final long receiver) {
            return receiver;
        }

        @Specialization(guards = "!is64bit(receiver)")
        protected final FloatObject doLong32bit(final long receiver) {
            return asFloatObject(receiver);
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
            final long truncated = (long) (receiver >= 0 ? Math.floor(receiver) : Math.ceil(receiver));
            if (!isSmallInteger(truncated)) {
                throw new PrimitiveExceptions.PrimitiveFailed();
            }
            return truncated;
        }

        protected final boolean isSmallInteger(final double value) {
            return SqueakGuards.isSmallInteger(method.image, (long) value);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {52, 552})
    protected abstract static class PrimFloatFractionPartNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimFloatFractionPartNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "is64bit(receiver)")
        protected static final double doDouble64bit(final double receiver) {
            return receiver - (long) receiver;
        }

        @Specialization(guards = "!is64bit(receiver)")
        protected final FloatObject doDouble32bit(final double receiver) {
            return asFloatObject(doDouble64bit(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {53, 553})
    protected abstract static class PrimFloatExponentNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        private static final long BIAS = 1023;

        protected PrimFloatExponentNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "receiver == 0")
        protected static final long doLongZero(@SuppressWarnings("unused") final long receiver) {
            return 0L;
        }

        @Specialization(guards = "isZero(receiver)")
        protected static final long doDoubleZero(@SuppressWarnings("unused") final double receiver) {
            return 0L;
        }

        @Specialization(guards = "isZero(receiver.getValue())")
        protected static final long doFloatZero(@SuppressWarnings("unused") final FloatObject receiver) {
            return 0L;
        }

        @Specialization(guards = "receiver != 0")
        protected static final long doLong(final long receiver) {
            return doDouble(receiver);
        }

        @Specialization(guards = "!isZero(receiver)")
        protected static final long doDouble(final double receiver) {
            final long bits = Double.doubleToRawLongBits(receiver) >>> 52 & 0x7FF;
            if (bits == 0) { // we have a subnormal float (actual zero was handled above)
                // make it normal by multiplying a large number
                final double data = receiver * Math.pow(2, 64);
                // access its exponent bits, and subtract the large number's exponent and bias
                return (Double.doubleToRawLongBits(data) >>> 52 & 0x7FF) - 64 - BIAS;
            } else {
                return bits - BIAS; // apply bias
            }
        }

        @Specialization(guards = "!isZero(receiver.getValue())")
        protected static final long doFloat(final FloatObject receiver) {
            return doDouble(receiver.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {54, 554})
    protected abstract static class PrimFloatTimesTwoPowerNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloatTimesTwoPowerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"is64bit(matissa)", "!isZero(matissa)", "!isInfinite(matissa)"})
        protected static final double doDoubleLong64bit(final double matissa, final long exponent) {
            return doDouble64bit(matissa, exponent);
        }

        @Specialization(guards = {"!is64bit(matissa)", "!isZero(matissa)", "!isInfinite(matissa)"})
        protected final FloatObject doDoubleLong32bit(final double matissa, final long exponent) {
            return doDouble32bit(matissa, exponent);
        }

        @Specialization(guards = {"is64bit(matissa)", "!isZero(matissa)", "!isInfinite(matissa)"})
        protected static final double doDouble64bit(final double matissa, final double exponent) {
            final double steps = Math.min(3, Math.ceil(Math.abs(exponent) / 1023));
            double result = matissa;
            for (int i = 0; i < steps; i++) {
                final double pow = Math.pow(2, Math.floor((exponent + i) / steps));
                assert pow != Double.POSITIVE_INFINITY && pow != Double.NEGATIVE_INFINITY;
                result *= pow;
            }
            return result;
        }

        @Specialization(guards = {"!is64bit(matissa)", "!isZero(matissa)", "!isInfinite(matissa)"})
        protected final FloatObject doDouble32bit(final double matissa, final double exponent) {
            return asFloatObject(doDouble64bit(matissa, exponent));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"is64bit(matissa)", "isZero(matissa)"})
        protected static final double doDoubleZero64bit(final double matissa, final double exponent) {
            return 0D;
        }

        @Specialization(guards = {"!is64bit(matissa)", "isZero(matissa)"})
        protected final FloatObject doDoubleZero32bit(final double matissa, final double exponent) {
            return asFloatObject(doDoubleZero64bit(matissa, exponent));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"is64bit(matissa)", "isInfinite(matissa)"})
        protected static final double doDoubleNaN64bit(final double matissa, final double exponent) {
            return matissa;
        }

        @Specialization(guards = {"!is64bit(matissa)", "isInfinite(matissa)"})
        protected final FloatObject doDoubleNaN32bit(final double matissa, final double exponent) {
            return asFloatObject(doDoubleZero64bit(matissa, exponent));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {55, 555})
    protected abstract static class PrimSquareRootNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimSquareRootNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "is64bit(receiver)")
        protected static final double doLong64bit(final long receiver) {
            return Math.sqrt(receiver);
        }

        @Specialization(guards = "is64bit(receiver)")
        protected static final double doLargeInteger64bit(final LargeIntegerObject receiver) {
            return doDouble64bit(receiver.doubleValue());
        }

        @Specialization(guards = "is64bit(receiver)")
        protected static final double doDouble64bit(final double receiver) {
            return Math.sqrt(receiver);
        }

        @Specialization(guards = "!is64bit(receiver)")
        protected final FloatObject doLong32bit(final long receiver) {
            return asFloatObject(doLong64bit(receiver));
        }

        @Specialization(guards = "!is64bit(receiver)")
        protected final FloatObject doLargeInteger32bit(final LargeIntegerObject receiver) {
            return asFloatObject(doLargeInteger64bit(receiver));
        }

        @Specialization(guards = "!is64bit(receiver)")
        protected final FloatObject doDouble32bit(final double receiver) {
            return asFloatObject(doDouble64bit(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {56, 556})
    protected abstract static class PrimSinNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimSinNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "is64bit(receiver)")
        protected static final double doDouble64bit(final double receiver) {
            return Math.sin(receiver);
        }

        @Specialization(guards = "!is64bit(receiver)")
        protected final FloatObject doDouble32bit(final double receiver) {
            return asFloatObject(doDouble64bit(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {57, 557})
    protected abstract static class PrimArcTanNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimArcTanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "is64bit(receiver)")
        protected static final double doDouble64bit(final double receiver) {
            return Math.atan(receiver);
        }

        @Specialization(guards = "!is64bit(receiver)")
        protected final FloatObject doDouble32bit(final double receiver) {
            return asFloatObject(doDouble64bit(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {58, 558})
    protected abstract static class PrimLogNNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimLogNNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "is64bit(receiver)")
        protected static final double doDouble64bit(final double receiver) {
            return Math.log(receiver);
        }

        @Specialization(guards = "!is64bit(receiver)")
        protected final FloatObject doDouble32bit(final double receiver) {
            return asFloatObject(doDouble64bit(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {59, 559})
    protected abstract static class PrimExpNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimExpNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "is64bit(receiver)")
        protected static final double doDouble64bit(final double receiver) {
            return Math.exp(receiver);
        }

        @Specialization(guards = "!is64bit(receiver)")
        protected final FloatObject doDouble32bit(final double receiver) {
            return asFloatObject(doDouble64bit(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 159)
    protected abstract static class PrimHashMultiplyNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
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
            return receiver * HASH_MULTIPLY_CONSTANT & HASH_MULTIPLY_MASK;
        }
    }

    @TypeSystemReference(SqueakArithmeticTypes.class)
    public abstract static class AbstractArithmeticPrimitiveNode extends AbstractPrimitiveNode {
        public AbstractArithmeticPrimitiveNode(final CompiledMethodObject method) {
            super(method);
        }

        protected static final boolean isAnExactFloat(final long value) {
            final int h = Long.bitCount(value);
            if (h <= FloatObject.PRECISION) {
                return true;
            }
            return value - 1 <= FloatObject.EMAX && h - Math.log(Long.lowestOneBit(Math.abs(value))) / Math.log(2) < FloatObject.PRECISION;
        }
    }

    public abstract static class AbstractArithmeticPrimitiveWithNumericResultNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        public AbstractArithmeticPrimitiveWithNumericResultNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected long doLong(final long a, final long b) {
            throw SqueakException.create("Should have been overriden: ", a, "-", b);
        }

        @Specialization
        protected Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            throw SqueakException.create("Should have been overriden: ", a, "-", b);
        }

        @Specialization(guards = "b.fitsIntoLong()", rewriteOn = ArithmeticException.class)
        protected final long doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValue());
        }

        @Specialization(guards = "b.fitsIntoLong()")
        protected final Object doLongLargeIntegerOverflow(final long a, final LargeIntegerObject b) {
            return doLongLargeInteger(a, b);
        }

        @Specialization(guards = "!b.fitsIntoLong()")
        protected final Object doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization(guards = "a.fitsIntoLong()", rewriteOn = ArithmeticException.class)
        protected final long doLargeIntegerAsLongLong(final LargeIntegerObject a, final long b) {
            return doLong(a.longValue(), b);
        }

        @Specialization(guards = "a.fitsIntoLong()")
        protected final Object doLargeIntegerAsLongLongOverflow(final LargeIntegerObject a, final long b) {
            return doLargeIntegerLong(a, b);
        }

        @Specialization(guards = "!a.fitsIntoLong()")
        protected final Object doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }
    }

    public abstract static class AbstractArithmeticPrimitiveWithBooleanResultNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        public AbstractArithmeticPrimitiveWithBooleanResultNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected boolean doLong(final long a, final long b) {
            throw SqueakException.create("Should have been overriden: ", a, "-", b);
        }

        @Specialization
        protected boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            throw SqueakException.create("Should have been overriden: ", a, "-", b);
        }

        @Specialization(guards = "b.fitsIntoLong()")
        protected final boolean doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValue());
        }

        @Specialization(guards = "!b.fitsIntoLong()")
        protected final boolean doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization(guards = "a.fitsIntoLong()")
        protected final boolean doLargeIntegerAsLongLong(final LargeIntegerObject a, final long b) {
            return doLong(a.longValue(), b);
        }

        @Specialization(guards = "!a.fitsIntoLong()")
        protected final boolean doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArithmeticPrimitivesFactory.getFactories();
    }
}
