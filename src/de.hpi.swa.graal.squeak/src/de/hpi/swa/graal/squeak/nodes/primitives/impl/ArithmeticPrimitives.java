package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.nodes.SqueakArithmeticTypes;
import de.hpi.swa.graal.squeak.nodes.plugins.LargeIntegers.PrimDigitBitShiftMagnitudeNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class ArithmeticPrimitives extends AbstractPrimitiveFactoryHolder {

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {1, 21, 41, 541})
    public abstract static class PrimAddNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        public PrimAddNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long a, final long b) {
            return Math.addExact(a, b);
        }

        @Specialization
        protected final Object doLongWithOverflow(final long a, final long b) {
            return LargeIntegerObject.add(method.image, a, b);
        }

        @Specialization(guards = {"a == 0"})
        protected static final LargeIntegerObject doLongLargeIntegerWithZero(@SuppressWarnings("unused") final long a, final LargeIntegerObject b) {
            return b;
        }

        @Specialization(guards = {"a != 0", "b.fitsIntoLong()"}, rewriteOn = ArithmeticException.class)
        protected static final long doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValue());
        }

        @Specialization(guards = {"a != 0", "b.fitsIntoLong()"})
        protected static final Object doLongLargeIntegerOverflow(final long a, final LargeIntegerObject b) {
            return b.add(a);
        }

        @Specialization(guards = {"a != 0", "!b.fitsIntoLong()"})
        protected static final Object doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return b.add(a);
        }

        @Specialization(guards = {"b == 0"})
        protected static final LargeIntegerObject doLongLargeIntegerWithZero(final LargeIntegerObject a, @SuppressWarnings("unused") final long b) {
            return a;
        }

        @Specialization(guards = {"b != 0", "a.fitsIntoLong()"}, rewriteOn = ArithmeticException.class)
        protected static final long doLargeIntegerAsLongLong(final LargeIntegerObject a, final long b) {
            return doLong(a.longValue(), b);
        }

        @Specialization(guards = {"b != 0", "a.fitsIntoLong()"})
        protected static final Object doLargeIntegerAsLongLongOverflow(final LargeIntegerObject a, final long b) {
            return a.add(b);
        }

        @Specialization(guards = {"b != 0", "!a.fitsIntoLong()"})
        protected static final Object doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return a.add(b);
        }

        @Specialization(guards = {"a.sameSign(b)"})
        protected static final LargeIntegerObject doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.addNoReduce(b); // Value can only grow, no need to try to reduce.
        }

        @Specialization(guards = "!a.sameSign(b)")
        protected static final Object doLargeIntegerNegative(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.add(b);
        }

        @Specialization
        protected static final double doLongDouble(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        protected static final double doDoubleLong(final double a, final long b) {
            return doDouble(a, b);
        }

        @Specialization
        protected static final double doDouble(final double a, final double b) {
            return a + b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {2, 22, 42, 542})
    public abstract static class PrimSubstractNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        public PrimSubstractNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long a, final long b) {
            return Math.subtractExact(a, b);
        }

        @Specialization
        protected final Object doLongWithOverflow(final long a, final long b) {
            return LargeIntegerObject.subtract(method.image, a, b);
        }

        @Specialization(guards = {"a == 0"})
        protected static final LargeIntegerObject doLongLargeIntegerWithZero(@SuppressWarnings("unused") final long a, final LargeIntegerObject b) {
            return b.negate();
        }

        @Specialization(guards = {"a != 0", "b.fitsIntoLong()"}, rewriteOn = ArithmeticException.class)
        protected static final long doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValue());
        }

        @Specialization(guards = {"a != 0", "b.fitsIntoLong()"})
        protected static final Object doLongLargeIntegerOverflow(final long a, final LargeIntegerObject b) {
            return LargeIntegerObject.subtract(a, b);
        }

        @Specialization(guards = {"a != 0", "!b.fitsIntoLong()"})
        protected static final Object doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return LargeIntegerObject.subtract(a, b);
        }

        @Specialization(guards = {"b == 0"})
        protected static final LargeIntegerObject doLongLargeIntegerWithZero(final LargeIntegerObject a, @SuppressWarnings("unused") final long b) {
            return a;
        }

        @Specialization(guards = {"b != 0", "a.fitsIntoLong()"}, rewriteOn = ArithmeticException.class)
        protected static final long doLargeIntegerAsLongLong(final LargeIntegerObject a, final long b) {
            return doLong(a.longValue(), b);
        }

        @Specialization(guards = {"b != 0", "a.fitsIntoLong()"})
        protected static final Object doLargeIntegerAsLongLongOverflow(final LargeIntegerObject a, final long b) {
            return a.subtract(b);
        }

        @Specialization(guards = {"b != 0", "!a.fitsIntoLong()"})
        protected static final Object doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return a.subtract(b);
        }

        @Specialization(guards = {"a.sameSign(b)"})
        protected static final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.subtract(b);
        }

        @Specialization(guards = "!a.sameSign(b)")
        protected static final Object doLargeIntegerNegative(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.subtractNoReduce(b); // Value can only grow, no need to try to reduce.
        }

        @Specialization
        protected static final double doLongDouble(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        protected static final double doDoubleLong(final double a, final long b) {
            return doDouble(a, b);
        }

        @Specialization
        protected static final double doDouble(final double a, final double b) {
            return a - b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {3, 23, 43, 543})
    protected abstract static class PrimLessThanNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimLessThanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doLong(final long a, final long b) {
            return a < b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) < 0 ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "b.fitsIntoLong()")
        protected final boolean doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValue());
        }

        @Specialization(guards = "!b.fitsIntoLong()")
        protected final boolean doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return b.compareTo(a) >= 0 ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "a.fitsIntoLong()")
        protected final boolean doLargeIntegerAsLongLong(final LargeIntegerObject a, final long b) {
            return doLong(a.longValue(), b);
        }

        @Specialization(guards = "!a.fitsIntoLong()")
        protected final boolean doLargeIntegerLong(final LargeIntegerObject a, final long b) {
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
    protected abstract static class PrimGreaterThanNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimGreaterThanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doLong(final long a, final long b) {
            return a > b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) > 0 ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "b.fitsIntoLong()")
        protected final boolean doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValue());
        }

        @Specialization(guards = "!b.fitsIntoLong()")
        protected final boolean doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return b.compareTo(a) <= 0 ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "a.fitsIntoLong()")
        protected final boolean doLargeIntegerAsLongLong(final LargeIntegerObject a, final long b) {
            return doLong(a.longValue(), b);
        }

        @Specialization(guards = "!a.fitsIntoLong()")
        protected final boolean doLargeIntegerLong(final LargeIntegerObject a, final long b) {
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
    protected abstract static class PrimLessOrEqualNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimLessOrEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doLong(final long a, final long b) {
            return a <= b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) <= 0 ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "b.fitsIntoLong()")
        protected final boolean doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValue());
        }

        @Specialization(guards = "!b.fitsIntoLong()")
        protected final boolean doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return b.compareTo(a) > 0 ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "a.fitsIntoLong()")
        protected final boolean doLargeIntegerAsLongLong(final LargeIntegerObject a, final long b) {
            return doLong(a.longValue(), b);
        }

        @Specialization(guards = "!a.fitsIntoLong()")
        protected final boolean doLargeIntegerLong(final LargeIntegerObject a, final long b) {
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
    protected abstract static class PrimGreaterOrEqualNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimGreaterOrEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doLong(final long a, final long b) {
            return a >= b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) >= 0 ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "b.fitsIntoLong()")
        protected final boolean doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValue());
        }

        @Specialization(guards = "!b.fitsIntoLong()")
        protected final boolean doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return b.compareTo(a) < 0 ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "a.fitsIntoLong()")
        protected final boolean doLargeIntegerAsLongLong(final LargeIntegerObject a, final long b) {
            return doLong(a.longValue(), b);
        }

        @Specialization(guards = "!a.fitsIntoLong()")
        protected final boolean doLargeIntegerLong(final LargeIntegerObject a, final long b) {
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
    protected abstract static class PrimEqualNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doLong(final long a, final long b) {
            return a == b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) == 0 ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "b.fitsIntoLong()")
        protected final boolean doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValue());
        }

        @Specialization(guards = "!b.fitsIntoLong()")
        protected final boolean doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return b.compareTo(a) == 0 ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "a.fitsIntoLong()")
        protected final boolean doLargeIntegerAsLongLong(final LargeIntegerObject a, final long b) {
            return doLong(a.longValue(), b);
        }

        @Specialization(guards = "!a.fitsIntoLong()")
        protected final boolean doLargeIntegerLong(final LargeIntegerObject a, final long b) {
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

        /*
         * Quick return false if b is not a Number or Complex.
         */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(b)", "!isLargeIntegerObject(b)", "!isPointersObject(b)"})
        protected final boolean doFail(final Object a, final AbstractSqueakObject b) {
            return method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {8, 28, 48, 548})
    protected abstract static class PrimNotEqualNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimNotEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doLong(final long a, final long b) {
            return a != b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b) != 0 ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "b.fitsIntoLong()")
        protected final boolean doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValue());
        }

        @Specialization(guards = "!b.fitsIntoLong()")
        protected final boolean doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return b.compareTo(a) != 0 ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "a.fitsIntoLong()")
        protected final boolean doLargeIntegerAsLongLong(final LargeIntegerObject a, final long b) {
            return doLong(a.longValue(), b);
        }

        @Specialization(guards = "!a.fitsIntoLong()")
        protected final boolean doLargeIntegerLong(final LargeIntegerObject a, final long b) {
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
    public abstract static class PrimMultiplyNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        public PrimMultiplyNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"a != 0", "b != 0"}, rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long a, final long b) {
            return Math.multiplyExact(a, b);
        }

        @Specialization(guards = {"a != 0", "b != 0"})
        protected final Object doLongWithOverflow(final long a, final long b) {
            return LargeIntegerObject.multiply(method.image, a, b);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"a == 0 || b == 0"})
        protected static final Object doLongWithZero(final long a, final long b) {
            return 0L;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"a == 0 || b.isZero()"})
        protected static final long doLongLargeIntegerWithZero(final long a, final LargeIntegerObject b) {
            return 0L;
        }

        @Specialization(guards = {"a != 0 ", "b.fitsIntoLong()"}, rewriteOn = ArithmeticException.class)
        protected static final long doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValue());
        }

        @Specialization(guards = {"a != 0", "b.fitsIntoLong()"})
        protected static final Object doLongLargeIntegerOverflow(final long a, final LargeIntegerObject b) {
            return b.multiply(a);
        }

        @Specialization(guards = {"isPowerOfTwo(a)", "!b.fitsIntoLong()"})
        protected static final LargeIntegerObject doLargeIntegerLongShift(final long a, final LargeIntegerObject b) {
            final long shiftBy = Long.numberOfTrailingZeros(a);
            assert 0 < shiftBy && shiftBy <= Integer.MAX_VALUE && a == Long.highestOneBit(a);
            return b.shiftLeftNoReduce((int) shiftBy);
        }

        @Specialization(guards = {"a != 0", "!isPowerOfTwo(a)", "!b.fitsIntoLong()"})
        protected final LargeIntegerObject doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return doLargeIntegerNoReduce(asLargeInteger(a), b);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"a.isZero() || b == 0"})
        protected static final long doLongLargeIntegerWithZero(final LargeIntegerObject a, final long b) {
            return 0L;
        }

        @Specialization(guards = {"b != 0", "a.fitsIntoLong()"}, rewriteOn = ArithmeticException.class)
        protected static final long doLargeIntegerAsLongLong(final LargeIntegerObject a, final long b) {
            return doLong(a.longValue(), b);
        }

        @Specialization(guards = {"b != 0", "a.fitsIntoLong()"})
        protected static final Object doLargeIntegerAsLongLongOverflow(final LargeIntegerObject a, final long b) {
            return a.multiply(b);
        }

        @Specialization(guards = {"isPowerOfTwo(b)", "!a.fitsIntoLong()"})
        protected static final LargeIntegerObject doLargeIntegerLongShift(final LargeIntegerObject a, @SuppressWarnings("unused") final long b) {
            final long shiftBy = Long.numberOfTrailingZeros(b);
            assert 0 < shiftBy && shiftBy <= Integer.MAX_VALUE && b == Long.highestOneBit(b);
            return a.shiftLeftNoReduce((int) shiftBy);
        }

        @Specialization(guards = {"b != 0", "!isPowerOfTwo(b)", "!a.fitsIntoLong()"})
        protected final LargeIntegerObject doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return doLargeIntegerNoReduce(a, asLargeInteger(b));
        }

        @Specialization(guards = {"!a.fitsIntoLong() || !b.fitsIntoLong()", "!a.isZero()", "!b.isZero()"})
        protected static final LargeIntegerObject doLargeIntegerNoReduce(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.multiplyNoReduce(b);
        }

        @Specialization(guards = {"a.fitsIntoLong()", "b.fitsIntoLong()"})
        protected static final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.multiply(b);
        }

        @Specialization
        protected static final double doLongDouble(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        protected static final double doDoubleLong(final double a, final long b) {
            return doDouble(a, b);
        }

        @Specialization
        protected static final double doDouble(final double a, final double b) {
            return a * b;
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
        protected final LargeIntegerObject doLongWithOverflow(final long a, final long b) {
            return LargeIntegerObject.createLongMinOverflowResult(method.image);
        }

        @Specialization(guards = {"!b.isZero()", "a.isIntegralWhenDividedBy(b)"})
        protected static final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.divide(b);
        }

        @Specialization(guards = {"b != 0", "a.isIntegralWhenDividedBy(b)"})
        protected static final Object doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return a.divide(b);
        }

        @Specialization(guards = {"!b.isZero()", "b.fitsIntoLong()", "isIntegralWhenDividedBy(a, b.longValue())", "!isOverflowDivision(a, b.longValue())"})
        protected static final long doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValue());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!b.isZero()", "b.fitsIntoLong()", "isOverflowDivision(a, b.longValue())"})
        protected final LargeIntegerObject doLongLargeIntegerAsLongWithOverflow(final long a, final LargeIntegerObject b) {
            return LargeIntegerObject.createLongMinOverflowResult(method.image);
        }

        /**
         * Fail if `a` is long and `b` is !b.isZero()" and "!b.fitsIntoLong()"
         * (isIntegralWhenDividedBy is always `false`).
         */

        @Specialization(guards = {"!isZero(b)"})
        protected static final double doLongDouble(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization(guards = {"b != 0"})
        protected static final double doDoubleLong(final double a, final long b) {
            return doDouble(a, b);
        }

        @Specialization(guards = {"!isZero(b)"})
        protected static final double doDouble(final double a, final double b) {
            return a / b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {11, 31})
    protected abstract static class PrimFloorModNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloorModNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0"})
        protected long doLong(final long a, final long b) {
            return Math.floorMod(a, b);
        }

        @Specialization
        protected Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.floorMod(b);
        }

        @Specialization(guards = "b.fitsIntoLong()")
        protected final long doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValue());
        }

        @Specialization(guards = "!b.fitsIntoLong()")
        protected static final Object doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return LargeIntegerObject.floorMod(a, b);
        }

        @Specialization(guards = "a.fitsIntoLong()")
        protected final long doLargeIntegerAsLongLong(final LargeIntegerObject a, final long b) {
            return doLong(a.longValue(), b);
        }

        @Specialization(guards = "!a.fitsIntoLong()")
        protected static final Object doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return a.floorMod(b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {12, 32})
    protected abstract static class PrimFloorDivideNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloorDivideNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"b != 0", "!isOverflowDivision(a, b)"})
        protected static final long doLong(final long a, final long b) {
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
        protected static final long doLongLargeIntegerAsLong(final long a, final LargeIntegerObject b) {
            return doLong(a, b.longValue());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"b.fitsIntoLong()", "isOverflowDivision(a, b.longValue())"})
        protected final LargeIntegerObject doLongLargeIntegerAsLongWithOverflow(final long a, final LargeIntegerObject b) {
            return LargeIntegerObject.createLongMinOverflowResult(method.image);
        }

        @Specialization(guards = {"!b.isZero()", "!b.fitsIntoLong()"})
        protected static final Object doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return LargeIntegerObject.floorDivide(a, b);
        }

        @Specialization(guards = "b != 0")
        protected static final Object doLargeIntegerLong(final LargeIntegerObject a, final long b) {
            return a.floorDivide(b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {13, 33})
    protected abstract static class PrimQuoNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimQuoNode(final CompiledMethodObject method) {
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
        protected static final Object doLongLargeInteger(final long a, final LargeIntegerObject b) {
            return LargeIntegerObject.divide(a, b);
        }

        @Specialization(guards = "!b.isZero()")
        protected static final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.divide(b);
        }

        @Specialization(guards = "b != 0")
        protected static final Object doLargeInteger(final LargeIntegerObject a, final long b) {
            return a.divide(b);
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
        protected static final long doLongLargeAsLong(final long receiver, final LargeIntegerObject arg) {
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
        protected static final long doLongLargeAsLong(final long receiver, final LargeIntegerObject arg) {
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
        protected static final long doLongLargeAsLong(final long receiver, final LargeIntegerObject arg) {
            return doLong(receiver, arg.longValue());
        }

        @Specialization(guards = {"!arg.fitsIntoLong()"})
        protected final Object doLongLarge(final long receiver, final LargeIntegerObject arg) {
            return asLargeInteger(receiver).xor(arg);
        }
    }

    /** Primitive 17 implemented via {@link PrimDigitBitShiftMagnitudeNode}. */

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

    // Squeak/Smalltalk uses LargeIntegers plugin for bit operations instead of primitives 34 to 37.

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 38)
    protected abstract static class PrimFloatAtNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloatAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "index == 1")
        protected static final long doDoubleHigh(final double receiver, @SuppressWarnings("unused") final long index) {
            final long bits = Double.doubleToRawLongBits(receiver);
            return Integer.toUnsignedLong((int) (bits >> 32));
        }

        @Specialization(guards = "index == 2")
        protected static final long doDoubleLow(final double receiver, @SuppressWarnings("unused") final long index) {
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

        @Specialization
        protected static final double doLong(final long receiver) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {51, 551})
    protected abstract static class PrimFloatTruncatedNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimFloatTruncatedNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doDouble(final double receiver) {
            final double rounded = receiver >= 0 ? Math.floor(receiver) : Math.ceil(receiver);
            final long value = (long) rounded;
            if (value == rounded) {
                return value;
            } else {
                throw new PrimitiveExceptions.PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {52, 552})
    protected abstract static class PrimFloatFractionPartNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimFloatFractionPartNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double receiver) {
            return receiver - (long) receiver;
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

        @Specialization(guards = {"!isZero(matissa)", "!isInfinite(matissa)", "exponent != 0"})
        protected static final double doDoubleLong(final double matissa, final long exponent) {
            return doDouble(matissa, exponent);
        }

        @Specialization(guards = {"!isZero(matissa)", "!isInfinite(matissa)", "!isZero(exponent)"})
        protected static final double doDouble(final double matissa, final double exponent) {
            final double steps = Math.min(3, Math.ceil(Math.abs(exponent) / 1023));
            double result = matissa;
            for (int i = 0; i < steps; i++) {
                final double pow = Math.pow(2, Math.floor((exponent + i) / steps));
                assert pow != Double.POSITIVE_INFINITY && pow != Double.NEGATIVE_INFINITY;
                result *= pow;
            }
            return result;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isZero(matissa)"})
        protected static final double doDoubleMatissaZero(final double matissa, final Object exponent) {
            return 0D;
        }

        @Specialization(guards = {"exponent == 0"})
        protected static final double doDoubleExponentZero(final double matissa, @SuppressWarnings("unused") final long exponent) {
            return matissa;
        }

        @Specialization(guards = {"isZero(exponent)"})
        protected static final double doDoubleExponentZero(final double matissa, @SuppressWarnings("unused") final double exponent) {
            return matissa;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isInfinite(matissa)"})
        protected static final double doDoubleNaN(final double matissa, final Object exponent) {
            return matissa;
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
            return doDouble(receiver.doubleValue());
        }

        @Specialization
        protected static final double doDouble(final double receiver) {
            return Math.sqrt(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {56, 556})
    protected abstract static class PrimSinNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimSinNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double receiver) {
            return Math.sin(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {57, 557})
    protected abstract static class PrimArcTanNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimArcTanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double receiver) {
            return Math.atan(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {58, 558})
    protected abstract static class PrimLogNNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimLogNNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double receiver) {
            return Math.log(receiver);
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
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 159)
    protected abstract static class PrimHashMultiplyNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        private static final int HASH_MULTIPLY_CONSTANT = 1664525;
        private static final long HASH_MULTIPLY_MASK = 0xFFFFFFFL;

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

        protected static final boolean sameSign(final long a, final long b) {
            return a >= 0 && b >= 0 || a < 0 && b < 0;
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArithmeticPrimitivesFactory.getFactories();
    }
}
