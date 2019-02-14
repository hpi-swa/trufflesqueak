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
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class ArithmeticPrimitives extends AbstractPrimitiveFactoryHolder {

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 1)
    public abstract static class PrimAddNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        public PrimAddNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isSmallInteger(a)", "isSmallInteger(b)"}, rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long a, final long b) {
            return Math.addExact(a, b);
        }

        @Specialization(guards = {"isSmallInteger(a)", "isSmallInteger(b)"})
        protected static final long doLongWithOverflow(final long a, final long b) {
            try {
                return Math.addExact(a, b);
            } catch (final ArithmeticException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 2)
    public abstract static class PrimSubstractNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        public PrimSubstractNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isSmallInteger(a)", "isSmallInteger(b)"}, rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long a, final long b) {
            return Math.subtractExact(a, b);
        }

        @Specialization(guards = {"isSmallInteger(a)", "isSmallInteger(b)"})
        protected static final long doLongWithOverflow(final long a, final long b) {
            try {
                return Math.subtractExact(a, b);
            } catch (final ArithmeticException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 3)
    protected abstract static class PrimLessThanNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimLessThanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isSmallInteger(a)", "isSmallInteger(b)"})
        protected final boolean doLong(final long a, final long b) {
            return a < b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 4)
    protected abstract static class PrimGreaterThanNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimGreaterThanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isSmallInteger(a)", "isSmallInteger(b)"})
        protected final boolean doLong(final long a, final long b) {
            return a > b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 5)
    protected abstract static class PrimLessOrEqualNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimLessOrEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isSmallInteger(a)", "isSmallInteger(b)"})
        protected final boolean doLong(final long a, final long b) {
            return a <= b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 6)
    protected abstract static class PrimGreaterOrEqualNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimGreaterOrEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isSmallInteger(a)", "isSmallInteger(b)"})
        protected final boolean doLong(final long a, final long b) {
            return a >= b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 7)
    protected abstract static class PrimEqualNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isSmallInteger(a)", "isSmallInteger(b)"})
        protected final boolean doLong(final long a, final long b) {
            return a == b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 8)
    protected abstract static class PrimNotEqualNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimNotEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isSmallInteger(a)", "isSmallInteger(b)"})
        protected final boolean doLong(final long a, final long b) {
            return a != b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 9)
    public abstract static class PrimMultiplyNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        public PrimMultiplyNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isSmallInteger(a)", "isSmallInteger(b)"}, rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long a, final long b) {
            return Math.multiplyExact(a, b);
        }

        @Specialization(guards = {"isSmallInteger(a)", "isSmallInteger(b)"})
        protected static final long doLongWithOverflow(final long a, final long b) {
            try {
                return Math.multiplyExact(a, b);
            } catch (final ArithmeticException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 10)
    protected abstract static class PrimDivideSmallIntegerNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimDivideSmallIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0", "isSmallInteger(a)", "isSmallInteger(b)", "isIntegralWhenDividedBy(a, b)"})
        public static final long doLong(final long a, final long b) {
            return a / b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 11)
    protected abstract static class PrimFloorModNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimFloorModNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0", "isSmallInteger(a)", "isSmallInteger(b)"})
        protected long doLong(final long a, final long b) {
            return Math.floorMod(a, b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 12)
    protected abstract static class PrimFloorDivideNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimFloorDivideNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0", "isSmallInteger(a)", "isSmallInteger(b)"})
        protected static final long doLong(final long a, final long b) {
            return Math.floorDiv(a, b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 13)
    protected abstract static class PrimQuoNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimQuoNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0", "isSmallInteger(a)", "isSmallInteger(b)"})
        protected static final long doLong(final long a, final long b) {
            return a / b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 14)
    public abstract static class PrimBitAndNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        public PrimBitAndNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isSmallIntegerPositive(receiver)", "isSmallIntegerPositive(arg)"})
        protected static final long doLong(final long receiver, final long arg) {
            return receiver & arg;
        }

        @Specialization(guards = {"isSmallIntegerPositive(receiver)", "arg.fitsIntoLong()"})
        protected static final Object doLongLargeAsLong(final long receiver, final LargeIntegerObject arg) {
            return doLong(receiver, arg.longValueExact());
        }

        @Specialization(guards = {"isSmallInteger(receiver)", "!arg.fitsIntoLong()", "arg.sizeLessThanWordSize()"})
        protected final Object doLongLarge(final long receiver, final LargeIntegerObject arg) {
            return asLargeInteger(receiver).and(arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 15)
    public abstract static class PrimBitOrNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        public PrimBitOrNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isSmallInteger(receiver)", "isSmallInteger(arg)"})
        protected static final long doLong(final long receiver, final long arg) {
            return receiver | arg;
        }

        @Specialization(guards = {"isSmallInteger(receiver)", "arg.fitsIntoLong()"})
        protected static final Object doLongLargeAsLong(final long receiver, final LargeIntegerObject arg) {
            return doLong(receiver, arg.longValueExact());
        }

        @Specialization(guards = {"isSmallInteger(receiver)", "!arg.fitsIntoLong()", "arg.sizeLessThanWordSize()"})
        protected final Object doLongLarge(final long receiver, final LargeIntegerObject arg) {
            return asLargeInteger(receiver).or(arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 16)
    protected abstract static class PrimBitXorNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimBitXorNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isSmallInteger(receiver)", "isSmallInteger(arg)"})
        protected static final long doLong(final long receiver, final long arg) {
            return receiver ^ arg;
        }

        @Specialization(guards = {"isSmallInteger(receiver)", "arg.fitsIntoLong()"})
        protected static final Object doLongLargeAsLong(final long receiver, final LargeIntegerObject arg) {
            return doLong(receiver, arg.longValueExact());
        }

        @Specialization(guards = {"isSmallInteger(receiver)", "!arg.fitsIntoLong()", "arg.sizeLessThanWordSize()"})
        protected final Object doLongLarge(final long receiver, final LargeIntegerObject arg) {
            return asLargeInteger(receiver).xor(arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 17)
    public abstract static class PrimBitShiftNode extends AbstractPrimitiveNode implements BinaryPrimitive {

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
    protected abstract static class PrimRemLargeIntegersNode extends AbstractPrimitiveNode implements BinaryPrimitive {
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
            return doLong(a, b.longValueExact());
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
    @SqueakPrimitive(indices = 21)
    public abstract static class PrimAddLargeIntegersNode extends AbstractLargeIntegerPrimitiveNode {
        public PrimAddLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization(rewriteOn = ArithmeticException.class)
        protected final Object doLong(final long a, final long b) {
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
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 22)
    public abstract static class PrimSubtractLargeIntegersNode extends AbstractLargeIntegerPrimitiveNode {
        public PrimSubtractLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization(rewriteOn = ArithmeticException.class)
        protected final Object doLong(final long a, final long b) {
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
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 23)
    protected abstract static class PrimLessThanLargeIntegersNode extends AbstractLargeIntegerPrimitiveNode {
        protected PrimLessThanLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final Object doLong(final long a, final long b) {
            return a < b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) < 0 ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 24)
    protected abstract static class PrimGreaterThanLargeIntegersNode extends AbstractLargeIntegerPrimitiveNode {
        protected PrimGreaterThanLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final Object doLong(final long a, final long b) {
            return a > b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) > 0 ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 25)
    protected abstract static class PrimLessOrEqualLargeIntegersNode extends AbstractLargeIntegerPrimitiveNode {
        protected PrimLessOrEqualLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final Object doLong(final long a, final long b) {
            return a <= b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) <= 0 ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 26)
    protected abstract static class PrimGreaterOrEqualLargeIntegersNode extends AbstractLargeIntegerPrimitiveNode {
        protected PrimGreaterOrEqualLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final Object doLong(final long a, final long b) {
            return a >= b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) >= 0 ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 27)
    protected abstract static class PrimEqualLargeIntegersNode extends AbstractLargeIntegerPrimitiveNode {
        protected PrimEqualLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final Object doLong(final long a, final long b) {
            return a == b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) == 0 ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 28)
    protected abstract static class PrimNotEqualLargeIntegersNode extends AbstractLargeIntegerPrimitiveNode {
        protected PrimNotEqualLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final Object doLong(final long a, final long b) {
            return a != b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) != 0 ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 29)
    public abstract static class PrimMultiplyLargeIntegersNode extends AbstractLargeIntegerPrimitiveNode {
        public PrimMultiplyLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization(rewriteOn = ArithmeticException.class)
        protected final Object doLong(final long a, final long b) {
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
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 30)
    protected abstract static class PrimDivideLargeIntegerNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimDivideLargeIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0", "isIntegralWhenDividedBy(a, b)", "!isOverflowDivision(a, b)"})
        public static final long doLong(final long a, final long b) {
            return a / b;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isOverflowDivision(a, b)")
        protected final Object doLongWithOverflow(final long a, final long b) {
            return LargeIntegerObject.createOverflowDivisionResult(method.image);
        }

        @Specialization(guards = {"!b.isZero()", "a.isIntegralWhenDividedBy(b)"})
        protected static final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.divide(b);
        }

        @Specialization(guards = {"b != 0", "a.isIntegralWhenDividedBy(asLargeInteger(b))"})
        protected final Object doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @Specialization(guards = {"!b.isZero()", "b.fitsIntoLong()", "isIntegralWhenDividedBy(a, b.longValueExact())", "!isOverflowDivision(a, b.longValueExact())"})
        protected static final Object doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValueExact());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"b.fitsIntoLong()", "isOverflowDivision(a, b.longValueExact())"})
        protected final LargeIntegerObject doLongLargeIntegerAsLongWithOverflow(final long a, final LargeIntegerObject b) {
            return LargeIntegerObject.createOverflowDivisionResult(method.image);
        }

        @Specialization(guards = {"!b.isZero()", "!b.fitsIntoLong()", "asLargeInteger(a).isIntegralWhenDividedBy(b)"})
        protected final Object doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 31)
    protected abstract static class PrimFloorModLargeIntegerNode extends AbstractLargeIntegerPrimitiveNode {
        protected PrimFloorModLargeIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization(guards = {"!isSmallInteger(a)"})
        protected Object doLong(final long a, final long b) {
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
    protected abstract static class PrimFloorDivideLargeIntegerNode extends AbstractPrimitiveNode implements BinaryPrimitive {
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
            return LargeIntegerObject.createOverflowDivisionResult(method.image);
        }

        @Specialization(guards = "!b.isZero()")
        protected static final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.floorDivide(b);
        }

        @Specialization(guards = {"!b.isZero()", "b.fitsIntoLong()", "!isOverflowDivision(a, b.longValueExact())"})
        protected static final Object doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValueExact());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"b.fitsIntoLong()", "isOverflowDivision(a, b.longValueExact())"})
        protected final LargeIntegerObject doLongLargeIntegerAsLongWithOverflow(final long a, final LargeIntegerObject b) {
            return LargeIntegerObject.createOverflowDivisionResult(method.image);
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
    protected abstract static class PrimQuoLargeIntegersNode extends AbstractPrimitiveNode implements BinaryPrimitive {
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
            return LargeIntegerObject.createOverflowDivisionResult(method.image);
        }

        @Specialization(guards = {"!b.isZero()", "b.fitsIntoLong()", "!isOverflowDivision(a, b.longValueExact())"})
        protected static final long doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValueExact());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"b.fitsIntoLong()", "isOverflowDivision(a, b.longValueExact())"})
        protected final LargeIntegerObject doLongLargeIntegerWithOverflow(final long a, final LargeIntegerObject b) {
            return LargeIntegerObject.createOverflowDivisionResult(method.image);
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
    protected abstract static class PrimFloatAtNode extends AbstractPrimitiveNode implements BinaryPrimitive {
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
    protected abstract static class PrimAsFloatNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimAsFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "is64bit(receiver)")
        protected final double doLong64bit(final long receiver) {
            assert isSmallInteger(receiver);
            return receiver;
        }

        @Specialization(guards = "!is64bit(receiver)")
        protected final FloatObject doLong32bit(final long receiver) {
            assert isSmallInteger(receiver);
            return asFloatObject(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {41, 541})
    public abstract static class PrimFloatAddNode extends AbstractFloatPrimitiveWithFloatResultNode {
        public PrimFloatAddNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final double doDouble(final double a, final double b) {
            return a + b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {42, 542})
    public abstract static class PrimFloatSubtractNode extends AbstractFloatPrimitiveWithFloatResultNode {
        public PrimFloatSubtractNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final double doDouble(final double a, final double b) {
            return a - b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {43, 543})
    protected abstract static class PrimFloatLessThanNode extends AbstractFloatPrimitiveWithBooleanResultNode {
        protected PrimFloatLessThanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final boolean doDouble(final double a, final double b) {
            return a < b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {44, 544})
    protected abstract static class PrimFloatGreaterThanNode extends AbstractFloatPrimitiveWithBooleanResultNode {
        protected PrimFloatGreaterThanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final boolean doDouble(final double a, final double b) {
            return a > b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {45, 545})
    protected abstract static class PrimFloatLessOrEqualNode extends AbstractFloatPrimitiveWithBooleanResultNode {
        protected PrimFloatLessOrEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final boolean doDouble(final double a, final double b) {
            return a <= b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {46, 546})
    protected abstract static class PrimFloatGreaterOrEqualNode extends AbstractFloatPrimitiveWithBooleanResultNode {
        protected PrimFloatGreaterOrEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final boolean doDouble(final double a, final double b) {
            return a >= b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {47, 547})
    protected abstract static class PrimFloatEqualNode extends AbstractFloatPrimitiveWithBooleanResultNode {
        protected PrimFloatEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final boolean doDouble(final double a, final double b) {
            return a == b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {48, 548})
    protected abstract static class PrimFloatNotEqualNode extends AbstractFloatPrimitiveWithBooleanResultNode {
        protected PrimFloatNotEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final boolean doDouble(final double a, final double b) {
            return a != b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {49, 549})
    public abstract static class PrimFloatMultiplyNode extends AbstractFloatPrimitiveWithFloatResultNode {
        public PrimFloatMultiplyNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final double doDouble(final double a, final double b) {
            return a * b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {50, 550})
    protected abstract static class PrimFloatDivideNode extends AbstractPrimitiveNode implements BinaryPrimitive {
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
    protected abstract static class PrimFloatTruncatedNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimFloatTruncatedNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "isSmallInteger(receiver)")
        protected static final long doDouble(final double receiver) {
            return (long) receiver;
        }

        @Specialization(guards = "isSmallInteger(receiver.getValue())")
        protected static final long doFloatObject(final FloatObject receiver) {
            return doDouble(receiver.getValue());
        }

        protected final boolean isSmallInteger(final double value) {
            if (method.image.flags.is64bit()) {
                return LargeIntegerObject.SMALLINTEGER64_MIN <= value && value <= LargeIntegerObject.SMALLINTEGER64_MAX;
            } else {
                return LargeIntegerObject.SMALLINTEGER32_MIN <= value && value <= LargeIntegerObject.SMALLINTEGER32_MAX;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {52, 552})
    protected abstract static class PrimFloatFractionPartNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimFloatFractionPartNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double receiver) {
            return receiver - (long) receiver;
        }

        @Specialization
        protected final FloatObject doFloatObject(final FloatObject receiver) {
            return asFloatObject(doDouble(receiver.getValue()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {53, 553})
    protected abstract static class PrimFloatExponentNode extends AbstractPrimitiveNode implements UnaryPrimitive {
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
    protected abstract static class PrimFloatTimesTwoPowerNode extends AbstractFloatPrimitiveWithFloatResultNode {
        protected PrimFloatTimesTwoPowerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected final double doDouble(final double matissa, final double exponent) {
            final double steps = Math.min(3, Math.ceil(Math.abs(exponent) / 1023));
            double result = matissa;
            for (int i = 0; i < steps; i++) {
                final double pow = Math.pow(2, Math.floor((exponent + i) / steps));
                assert pow != Double.POSITIVE_INFINITY && pow != Double.NEGATIVE_INFINITY;
                result *= pow;
            }
            return result;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {55, 555})
    protected abstract static class PrimSquareRootNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimSquareRootNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doLong(final long receiver) {
            return Math.sqrt(receiver);
        }

        @Specialization
        protected static final double doLargeInteger(final LargeIntegerObject receiver) {
            return doDouble(receiver.doubleValue());
        }

        @Specialization
        protected static final double doDouble(final double receiver) {
            return Math.sqrt(receiver);
        }

        @Specialization
        protected final FloatObject doFloat(final FloatObject a) {
            return asFloatObject(doDouble(a.getValue()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {56, 556})
    protected abstract static class PrimSinNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimSinNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double rcvr) {
            return Math.sin(rcvr);
        }

        @Specialization
        protected final FloatObject doFloat(final FloatObject a) {
            return asFloatObject(doDouble(a.getValue()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {57, 557})
    protected abstract static class PrimArcTanNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimArcTanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double a) {
            return Math.atan(a);
        }

        @Specialization
        protected final FloatObject doFloat(final FloatObject a) {
            return asFloatObject(doDouble(a.getValue()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {58, 558})
    protected abstract static class PrimLogNNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimLogNNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double a) {
            return Math.log(a);
        }

        @Specialization
        protected final FloatObject doFloat(final FloatObject a) {
            return asFloatObject(doDouble(a.getValue()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {59, 559})
    protected abstract static class PrimExpNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimExpNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double receiver) {
            return Math.exp(receiver);
        }

        @Specialization
        protected final FloatObject doFloat(final FloatObject receiver) {
            return asFloatObject(doDouble(receiver.getValue()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 159)
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
            return receiver * HASH_MULTIPLY_CONSTANT & HASH_MULTIPLY_MASK;
        }
    }

    protected abstract static class AbstractLargeIntegerPrimitiveNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected AbstractLargeIntegerPrimitiveNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object doLong(final long a, final long b) {
            throw SqueakException.create("Should have been overriden: ", a, "-", b);
        }

        @Specialization
        protected Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            throw SqueakException.create("Should have been overriden: ", a, "-", b);
        }

        @Specialization
        protected final Object doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @Specialization(guards = "b.fitsIntoLong()", rewriteOn = ArithmeticException.class)
        protected final Object doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValueExact());
        }

        @Specialization(guards = "!b.fitsIntoLong()")
        protected final Object doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }
    }

    protected abstract static class AbstractFloatPrimitiveWithFloatResultNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected AbstractFloatPrimitiveWithFloatResultNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected double doDouble(final double a, final double b) {
            throw SqueakException.create("Should have been overriden: ", a, "-", b);
        }

        @Specialization(guards = "is64bit(a)")
        protected final double doFloat64bit(final FloatObject a, final FloatObject b) {
            return doDouble(a.getValue(), b.getValue());
        }

        @Specialization(guards = "!is64bit(a)")
        protected final FloatObject doFloat32bit(final FloatObject a, final FloatObject b) {
            return asFloatObject(doDouble(a.getValue(), b.getValue()));
        }

        @Specialization
        protected final double doDoubleFloat(final double a, final FloatObject b) {
            return doDouble(a, b.getValue());
        }

        @Specialization(guards = "is64bit(a)")
        protected final double doFloatDouble64bit(final FloatObject a, final double b) {
            return doDouble(a.getValue(), b);
        }

        @Specialization(guards = "!is64bit(a)")
        protected final FloatObject doFloatDouble32bit(final FloatObject a, final double b) {
            return asFloatObject(doDouble(a.getValue(), b));
        }

        /*
         * `Float` primitives accept `SmallInteger`s (see #loadFloatOrIntFrom:).
         */

        @Specialization(guards = "isSmallInteger(b)") // Needs to fail on LargeIntegerObjects.
        protected final double doDoubleLong(final double a, final long b) {
            return doDouble(a, b);
        }

        @Specialization(guards = {"is64bit(a)", "isSmallInteger(b)"})
        protected final double doFloatLong64bit(final FloatObject a, final long b) {
            return doFloatDouble64bit(a, b);
        }

        @Specialization(guards = {"!is64bit(a)", "isSmallInteger(b)"})
        protected final FloatObject doFloatLong32bit(final FloatObject a, final long b) {
            return doFloatDouble32bit(a, b);
        }
    }

    protected abstract static class AbstractFloatPrimitiveWithBooleanResultNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected AbstractFloatPrimitiveWithBooleanResultNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected boolean doDouble(final double a, final double b) {
            throw SqueakException.create("Should have been overriden: ", a, "-", b);
        }

        @Specialization
        protected final boolean doFloat(final FloatObject a, final FloatObject b) {
            return doDouble(a.getValue(), b.getValue());
        }

        @Specialization
        protected final boolean doDoubleFloat(final double a, final FloatObject b) {
            return doDouble(a, b.getValue());
        }

        @Specialization
        protected final boolean doFloatDouble(final FloatObject a, final double b) {
            return doDouble(a.getValue(), b);
        }

        /*
         * `Float` primitives accept `SmallInteger`s (see #loadFloatOrIntFrom:).
         */

        @Specialization(guards = "isSmallInteger(b)")
        protected final boolean doDoubleLong(final double a, final long b) {
            return doDouble(a, b);
        }

        @Specialization(guards = "isSmallInteger(b)")
        protected final boolean doFloatLong(final FloatObject a, final long b) {
            return doFloatDouble(a, b);
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArithmeticPrimitivesFactory.getFactories();
    }
}
