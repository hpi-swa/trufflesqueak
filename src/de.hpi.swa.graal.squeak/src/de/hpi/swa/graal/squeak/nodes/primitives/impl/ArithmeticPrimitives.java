package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.BooleanObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.nodes.ArithmeticBaseTypeSystem;
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
        protected static final long doLong(final long lhs, final long rhs) {
            return Math.addExact(lhs, rhs);
        }

        @Specialization
        protected final Object doLongWithOverflow(final long lhs, final long rhs) {
            return LargeIntegerObject.add(method.image, lhs, rhs);
        }

        @Specialization(guards = {"lhs == 0"})
        protected static final LargeIntegerObject doLongLargeIntegerWithZero(@SuppressWarnings("unused") final long lhs, final LargeIntegerObject rhs) {
            return rhs;
        }

        @Specialization(guards = {"lhs != 0", "rhs.fitsIntoLong()"}, rewriteOn = ArithmeticException.class)
        protected static final long doLongLargeIntegerAsLong(final long lhs, final LargeIntegerObject rhs) {
            return doLong(lhs, rhs.longValue());
        }

        @Specialization(guards = {"lhs != 0", "rhs.fitsIntoLong()"})
        protected static final Object doLongLargeIntegerOverflow(final long lhs, final LargeIntegerObject rhs) {
            return rhs.add(lhs);
        }

        @Specialization(guards = {"lhs != 0", "!rhs.fitsIntoLong()"})
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return rhs.add(lhs);
        }

        @Specialization(guards = {"rhs == 0"})
        protected static final LargeIntegerObject doLongLargeIntegerWithZero(final LargeIntegerObject lhs, @SuppressWarnings("unused") final long rhs) {
            return lhs;
        }

        @Specialization(guards = {"rhs != 0", "lhs.fitsIntoLong()"}, rewriteOn = ArithmeticException.class)
        protected static final long doLargeIntegerAsLongLong(final LargeIntegerObject lhs, final long rhs) {
            return doLong(lhs.longValue(), rhs);
        }

        @Specialization(guards = {"rhs != 0", "lhs.fitsIntoLong()"})
        protected static final Object doLargeIntegerAsLongLongOverflow(final LargeIntegerObject lhs, final long rhs) {
            return lhs.add(rhs);
        }

        @Specialization(guards = {"rhs != 0", "!lhs.fitsIntoLong()"})
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.add(rhs);
        }

        @Specialization(guards = {"lhs.sameSign(rhs)"})
        protected static final LargeIntegerObject doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.addNoReduce(rhs); // Value can only grow, no need to try to reduce.
        }

        @Specialization(guards = "!lhs.sameSign(rhs)")
        protected static final Object doLargeIntegerNegative(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.add(rhs);
        }

        @Specialization
        protected final Object doLongDouble(final long lhs, final double rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return doDouble(lhs, rhs, isFiniteProfile);
        }

        @Specialization
        protected final Object doDoubleLong(final double lhs, final long rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return doDouble(lhs, rhs, isFiniteProfile);
        }

        @Specialization
        protected final Object doDouble(final double lhs, final double rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return FloatObject.boxIfNecessary(method.image, lhs + rhs, isFiniteProfile);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {2, 22, 42, 542})
    public abstract static class PrimSubtractNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        public PrimSubtractNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs) {
            return Math.subtractExact(lhs, rhs);
        }

        @Specialization
        protected final Object doLongWithOverflow(final long lhs, final long rhs) {
            return LargeIntegerObject.subtract(method.image, lhs, rhs);
        }

        @Specialization(guards = {"lhs == 0"})
        protected static final LargeIntegerObject doLongLargeIntegerWithZero(@SuppressWarnings("unused") final long lhs, final LargeIntegerObject rhs) {
            return rhs.negate();
        }

        @Specialization(guards = {"lhs != 0", "rhs.fitsIntoLong()"}, rewriteOn = ArithmeticException.class)
        protected static final long doLongLargeIntegerAsLong(final long lhs, final LargeIntegerObject rhs) {
            return doLong(lhs, rhs.longValue());
        }

        @Specialization(guards = {"lhs != 0", "rhs.fitsIntoLong()"})
        protected static final Object doLongLargeIntegerOverflow(final long lhs, final LargeIntegerObject rhs) {
            return LargeIntegerObject.subtract(lhs, rhs);
        }

        @Specialization(guards = {"lhs != 0", "!rhs.fitsIntoLong()"})
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return LargeIntegerObject.subtract(lhs, rhs);
        }

        @Specialization(guards = {"rhs == 0"})
        protected static final LargeIntegerObject doLongLargeIntegerWithZero(final LargeIntegerObject lhs, @SuppressWarnings("unused") final long rhs) {
            return lhs;
        }

        @Specialization(guards = {"rhs != 0", "lhs.fitsIntoLong()"}, rewriteOn = ArithmeticException.class)
        protected static final long doLargeIntegerAsLongLong(final LargeIntegerObject lhs, final long rhs) {
            return doLong(lhs.longValue(), rhs);
        }

        @Specialization(guards = {"rhs != 0", "lhs.fitsIntoLong()"})
        protected static final Object doLargeIntegerAsLongLongOverflow(final LargeIntegerObject lhs, final long rhs) {
            return lhs.subtract(rhs);
        }

        @Specialization(guards = {"rhs != 0", "!lhs.fitsIntoLong()"})
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.subtract(rhs);
        }

        @Specialization(guards = {"lhs.sameSign(rhs)"})
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.subtract(rhs);
        }

        @Specialization(guards = "!lhs.sameSign(rhs)")
        protected static final Object doLargeIntegerNegative(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.subtractNoReduce(rhs); // Value can only grow, no need to try to reduce.
        }

        @Specialization
        protected final Object doLongDouble(final long lhs, final double rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return doDouble(lhs, rhs, isFiniteProfile);
        }

        @Specialization
        protected final Object doDoubleLong(final double lhs, final long rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return doDouble(lhs, rhs, isFiniteProfile);
        }

        @Specialization
        protected final Object doDouble(final double lhs, final double rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return FloatObject.boxIfNecessary(method.image, lhs - rhs, isFiniteProfile);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {3, 23, 43, 543})
    protected abstract static class PrimLessThanNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimLessThanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs < rhs);
        }

        @Specialization
        protected static final boolean doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) < 0);
        }

        @Specialization(guards = "rhs.fitsIntoLong()")
        protected static final boolean doLongLargeIntegerAsLong(final long lhs, final LargeIntegerObject rhs) {
            return doLong(lhs, rhs.longValue());
        }

        @Specialization(guards = "!rhs.fitsIntoLong()")
        protected static final boolean doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) >= 0);
        }

        @Specialization(guards = "lhs.fitsIntoLong()")
        protected static final boolean doLargeIntegerAsLongLong(final LargeIntegerObject lhs, final long rhs) {
            return doLong(lhs.longValue(), rhs);
        }

        @Specialization(guards = "!lhs.fitsIntoLong()")
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) < 0);
        }

        @Specialization
        protected static final boolean doLongDouble(final long lhs, final double rhs) {
            return BooleanObject.wrap(lhs < rhs);
        }

        @Specialization
        protected static final boolean doDoubleLong(final double lhs, final long rhs) {
            return BooleanObject.wrap(lhs < rhs);
        }

        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs < rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {4, 24, 44, 544})
    protected abstract static class PrimGreaterThanNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimGreaterThanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs > rhs);
        }

        @Specialization
        protected static final boolean doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) > 0);
        }

        @Specialization(guards = "rhs.fitsIntoLong()")
        protected static final boolean doLongLargeIntegerAsLong(final long lhs, final LargeIntegerObject rhs) {
            return doLong(lhs, rhs.longValue());
        }

        @Specialization(guards = "!rhs.fitsIntoLong()")
        protected static final boolean doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) <= 0);
        }

        @Specialization(guards = "lhs.fitsIntoLong()")
        protected static final boolean doLargeIntegerAsLongLong(final LargeIntegerObject lhs, final long rhs) {
            return doLong(lhs.longValue(), rhs);
        }

        @Specialization(guards = "!lhs.fitsIntoLong()")
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) > 0);
        }

        @Specialization
        protected static final boolean doLongDouble(final long lhs, final double rhs) {
            return BooleanObject.wrap(lhs > rhs);
        }

        @Specialization
        protected static final boolean doDoubleLong(final double lhs, final long rhs) {
            return BooleanObject.wrap(lhs > rhs);
        }

        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs > rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {5, 25, 45, 545})
    protected abstract static class PrimLessOrEqualNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimLessOrEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs <= rhs);
        }

        @Specialization
        protected static final boolean doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) <= 0);
        }

        @Specialization(guards = "rhs.fitsIntoLong()")
        protected static final boolean doLongLargeIntegerAsLong(final long lhs, final LargeIntegerObject rhs) {
            return doLong(lhs, rhs.longValue());
        }

        @Specialization(guards = "!rhs.fitsIntoLong()")
        protected static final boolean doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) > 0);
        }

        @Specialization(guards = "lhs.fitsIntoLong()")
        protected static final boolean doLargeIntegerAsLongLong(final LargeIntegerObject lhs, final long rhs) {
            return doLong(lhs.longValue(), rhs);
        }

        @Specialization(guards = "!lhs.fitsIntoLong()")
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) <= 0);
        }

        @Specialization
        protected static final boolean doLongDouble(final long lhs, final double rhs) {
            return BooleanObject.wrap(lhs <= rhs);
        }

        @Specialization
        protected static final boolean doDoubleLong(final double lhs, final long rhs) {
            return BooleanObject.wrap(lhs <= rhs);
        }

        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs <= rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {6, 26, 46, 546})
    protected abstract static class PrimGreaterOrEqualNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimGreaterOrEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs >= rhs);
        }

        @Specialization
        protected static final boolean doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) >= 0);
        }

        @Specialization(guards = "rhs.fitsIntoLong()")
        protected static final boolean doLongLargeIntegerAsLong(final long lhs, final LargeIntegerObject rhs) {
            return doLong(lhs, rhs.longValue());
        }

        @Specialization(guards = "!rhs.fitsIntoLong()")
        protected static final boolean doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) < 0);
        }

        @Specialization(guards = "lhs.fitsIntoLong()")
        protected static final boolean doLargeIntegerAsLongLong(final LargeIntegerObject lhs, final long rhs) {
            return doLong(lhs.longValue(), rhs);
        }

        @Specialization(guards = "!lhs.fitsIntoLong()")
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) >= 0);
        }

        @Specialization
        protected static final boolean doLongDouble(final long lhs, final double rhs) {
            return BooleanObject.wrap(lhs >= rhs);
        }

        @Specialization
        protected static final boolean doDoubleLong(final double lhs, final long rhs) {
            return BooleanObject.wrap(lhs >= rhs);
        }

        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs >= rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {7, 27, 47, 547})
    protected abstract static class PrimEqualNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs == rhs);
        }

        @Specialization
        protected static final boolean doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) == 0);
        }

        @Specialization(guards = "rhs.fitsIntoLong()")
        protected static final boolean doLongLargeIntegerAsLong(final long lhs, final LargeIntegerObject rhs) {
            return doLong(lhs, rhs.longValue());
        }

        @Specialization(guards = "!rhs.fitsIntoLong()")
        protected static final boolean doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) == 0);
        }

        @Specialization(guards = "lhs.fitsIntoLong()")
        protected static final boolean doLargeIntegerAsLongLong(final LargeIntegerObject lhs, final long rhs) {
            return doLong(lhs.longValue(), rhs);
        }

        @Specialization(guards = "!lhs.fitsIntoLong()")
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) == 0);
        }

        @Specialization(guards = "isAnExactFloat(lhs)")
        protected static final boolean doLongExactDouble(final long lhs, final double rhs) {
            return doDouble(lhs, rhs);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isAnExactFloat(lhs)")
        protected static final boolean doLongNotExactDouble(final long lhs, final double rhs) {
            return BooleanObject.FALSE;
        }

        @Specialization
        protected static final boolean doDoubleLongExact(final double lhs, final long rhs) {
            return doDouble(lhs, rhs);
        }

        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs == rhs);
        }

        /** Quick return `false` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(rhs)", "!isPointersObject(rhs)"})
        protected static final boolean doQuickFalse(final Object lhs, final AbstractSqueakObject rhs) {
            return BooleanObject.FALSE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {8, 28, 48, 548})
    protected abstract static class PrimNotEqualNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimNotEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs != rhs);
        }

        @Specialization
        protected static final boolean doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) != 0);
        }

        @Specialization(guards = "rhs.fitsIntoLong()")
        protected static final boolean doLongLargeIntegerAsLong(final long lhs, final LargeIntegerObject rhs) {
            return doLong(lhs, rhs.longValue());
        }

        @Specialization(guards = "!rhs.fitsIntoLong()")
        protected static final boolean doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) != 0);
        }

        @Specialization(guards = "lhs.fitsIntoLong()")
        protected static final boolean doLargeIntegerAsLongLong(final LargeIntegerObject lhs, final long rhs) {
            return doLong(lhs.longValue(), rhs);
        }

        @Specialization(guards = "!lhs.fitsIntoLong()")
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) != 0);
        }

        @Specialization(guards = "isAnExactFloat(lhs)")
        protected static final boolean doLongExactDouble(final long lhs, final double rhs) {
            return doDouble(lhs, rhs);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isAnExactFloat(lhs)")
        protected static final boolean doLongNotExactDouble(final long lhs, final double rhs) {
            return BooleanObject.TRUE;
        }

        @Specialization
        protected static final boolean doDoubleLongExact(final double lhs, final long rhs) {
            return doDouble(lhs, rhs);
        }

        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs != rhs);
        }

        /** Quick return `true` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(rhs)", "!isPointersObject(rhs)"})
        protected static final boolean doQuickTrue(final Object lhs, final AbstractSqueakObject rhs) {
            return BooleanObject.TRUE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {9, 29, 49, 549})
    public abstract static class PrimMultiplyNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        public PrimMultiplyNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"lhs != 0", "rhs != 0"}, rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs) {
            return Math.multiplyExact(lhs, rhs);
        }

        @Specialization(guards = {"lhs != 0", "rhs != 0"})
        protected final Object doLongWithOverflow(final long lhs, final long rhs) {
            return LargeIntegerObject.multiply(method.image, lhs, rhs);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"lhs == 0 || rhs == 0"})
        protected static final Object doLongWithZero(final long lhs, final long rhs) {
            return 0L;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"lhs == 0 || rhs.isZero()"})
        protected static final long doLongLargeIntegerWithZero(final long lhs, final LargeIntegerObject rhs) {
            return 0L;
        }

        @Specialization(guards = {"lhs != 0 ", "rhs.fitsIntoLong()"}, rewriteOn = ArithmeticException.class)
        protected static final long doLongLargeIntegerAsLong(final long lhs, final LargeIntegerObject rhs) {
            return doLong(lhs, rhs.longValue());
        }

        @Specialization(guards = {"lhs != 0", "rhs.fitsIntoLong()"})
        protected static final Object doLongLargeIntegerOverflow(final long lhs, final LargeIntegerObject rhs) {
            return rhs.multiply(lhs);
        }

        @Specialization(guards = {"isPowerOfTwo(lhs)", "!rhs.fitsIntoLong()"})
        protected static final LargeIntegerObject doLargeIntegerLongShift(final long lhs, final LargeIntegerObject rhs) {
            final long shiftBy = Long.numberOfTrailingZeros(lhs);
            assert 0 < shiftBy && shiftBy <= Integer.MAX_VALUE && lhs == Long.highestOneBit(lhs);
            return rhs.shiftLeftNoReduce((int) shiftBy);
        }

        @Specialization(guards = {"lhs != 0", "!isPowerOfTwo(lhs)", "!rhs.fitsIntoLong()"})
        protected final LargeIntegerObject doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return doLargeIntegerNoReduce(asLargeInteger(lhs), rhs);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"lhs.isZero() || rhs == 0"})
        protected static final long doLongLargeIntegerWithZero(final LargeIntegerObject lhs, final long rhs) {
            return 0L;
        }

        @Specialization(guards = {"rhs != 0", "lhs.fitsIntoLong()"}, rewriteOn = ArithmeticException.class)
        protected static final long doLargeIntegerAsLongLong(final LargeIntegerObject lhs, final long rhs) {
            return doLong(lhs.longValue(), rhs);
        }

        @Specialization(guards = {"rhs != 0", "lhs.fitsIntoLong()"})
        protected static final Object doLargeIntegerAsLongLongOverflow(final LargeIntegerObject lhs, final long rhs) {
            return lhs.multiply(rhs);
        }

        @Specialization(guards = {"isPowerOfTwo(rhs)", "!lhs.fitsIntoLong()"})
        protected static final LargeIntegerObject doLargeIntegerLongShift(final LargeIntegerObject lhs, @SuppressWarnings("unused") final long rhs) {
            final long shiftBy = Long.numberOfTrailingZeros(rhs);
            assert 0 < shiftBy && shiftBy <= Integer.MAX_VALUE && rhs == Long.highestOneBit(rhs);
            return lhs.shiftLeftNoReduce((int) shiftBy);
        }

        @Specialization(guards = {"rhs != 0", "!isPowerOfTwo(rhs)", "!lhs.fitsIntoLong()"})
        protected final LargeIntegerObject doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return doLargeIntegerNoReduce(lhs, asLargeInteger(rhs));
        }

        @Specialization(guards = {"!lhs.fitsIntoLong() || !rhs.fitsIntoLong()", "!lhs.isZero()", "!rhs.isZero()"})
        protected static final LargeIntegerObject doLargeIntegerNoReduce(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.multiplyNoReduce(rhs);
        }

        @Specialization(guards = {"lhs.fitsIntoLong()", "rhs.fitsIntoLong()"})
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.multiply(rhs);
        }

        @Specialization
        protected final Object doLongDouble(final long lhs, final double rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return doDouble(lhs, rhs, isFiniteProfile);
        }

        @Specialization
        protected final Object doDoubleLong(final double lhs, final long rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return doDouble(lhs, rhs, isFiniteProfile);
        }

        @Specialization
        protected final Object doDouble(final double lhs, final double rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return FloatObject.boxIfNecessary(method.image, lhs * rhs, isFiniteProfile);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {10, 30, 50, 550})
    protected abstract static class PrimDivideNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimDivideNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"rhs != 0", "isIntegralWhenDividedBy(lhs, rhs)", "!isOverflowDivision(lhs, rhs)"})
        public static final long doLong(final long lhs, final long rhs) {
            return lhs / rhs;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isOverflowDivision(lhs, rhs)")
        protected final LargeIntegerObject doLongWithOverflow(final long lhs, final long rhs) {
            return LargeIntegerObject.createLongMinOverflowResult(method.image);
        }

        @Specialization(guards = {"!rhs.isZero()", "lhs.isIntegralWhenDividedBy(rhs)"})
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.divide(rhs);
        }

        @Specialization(guards = {"rhs != 0", "lhs.isIntegralWhenDividedBy(rhs)"})
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.divide(rhs);
        }

        @Specialization(guards = {"!rhs.isZero()", "rhs.fitsIntoLong()", "isIntegralWhenDividedBy(lhs, rhs.longValue())", "!isOverflowDivision(lhs, rhs.longValue())"})
        protected static final long doLongLargeIntegerAsLong(final long lhs, final LargeIntegerObject rhs) {
            return doLong(lhs, rhs.longValue());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!rhs.isZero()", "rhs.fitsIntoLong()", "isOverflowDivision(lhs, rhs.longValue())"})
        protected final LargeIntegerObject doLongLargeIntegerAsLongWithOverflow(final long lhs, final LargeIntegerObject rhs) {
            return LargeIntegerObject.createLongMinOverflowResult(method.image);
        }

        @Specialization(guards = {"!isZero(rhs)"})
        protected final Object doLongDouble(final long lhs, final double rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return doDouble(lhs, rhs, isFiniteProfile);
        }

        @Specialization(guards = {"rhs != 0"})
        protected final Object doDoubleLong(final double lhs, final long rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return doDouble(lhs, rhs, isFiniteProfile);
        }

        @Specialization(guards = {"!isZero(rhs)"})
        protected final Object doDouble(final double lhs, final double rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return FloatObject.boxIfNecessary(method.image, lhs / rhs, isFiniteProfile);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {11, 31})
    protected abstract static class PrimFloorModNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloorModNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"rhs != 0"})
        protected long doLong(final long lhs, final long rhs) {
            return Math.floorMod(lhs, rhs);
        }

        @Specialization
        protected Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.floorMod(rhs);
        }

        @Specialization(guards = "rhs.fitsIntoLong()")
        protected final long doLongLargeIntegerAsLong(final long lhs, final LargeIntegerObject rhs) {
            return doLong(lhs, rhs.longValue());
        }

        @Specialization(guards = "!rhs.fitsIntoLong()")
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return LargeIntegerObject.floorMod(lhs, rhs);
        }

        @Specialization(guards = "lhs.fitsIntoLong()")
        protected final long doLargeIntegerAsLongLong(final LargeIntegerObject lhs, final long rhs) {
            return doLong(lhs.longValue(), rhs);
        }

        @Specialization(guards = "!lhs.fitsIntoLong()")
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.floorMod(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {12, 32})
    protected abstract static class PrimFloorDivideNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloorDivideNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)"})
        protected static final long doLong(final long lhs, final long rhs) {
            return Math.floorDiv(lhs, rhs);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isOverflowDivision(lhs, rhs)")
        protected final LargeIntegerObject doLongWithOverflow(final long lhs, final long rhs) {
            return LargeIntegerObject.createLongMinOverflowResult(method.image);
        }

        @Specialization(guards = "!rhs.isZero()")
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.floorDivide(rhs);
        }

        @Specialization(guards = {"!rhs.isZero()", "rhs.fitsIntoLong()", "!isOverflowDivision(lhs, rhs.longValue())"})
        protected static final long doLongLargeIntegerAsLong(final long lhs, final LargeIntegerObject rhs) {
            return doLong(lhs, rhs.longValue());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"rhs.fitsIntoLong()", "isOverflowDivision(lhs, rhs.longValue())"})
        protected final LargeIntegerObject doLongLargeIntegerAsLongWithOverflow(final long lhs, final LargeIntegerObject rhs) {
            return LargeIntegerObject.createLongMinOverflowResult(method.image);
        }

        @Specialization(guards = {"!rhs.isZero()", "!rhs.fitsIntoLong()"})
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return LargeIntegerObject.floorDivide(lhs, rhs);
        }

        @Specialization(guards = "rhs != 0")
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.floorDivide(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {13, 33})
    protected abstract static class PrimQuoNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimQuoNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)"})
        public static final long doLong(final long lhs, final long rhs) {
            return lhs / rhs;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isOverflowDivision(lhs, rhs)")
        protected final LargeIntegerObject doLongWithOverflow(final long lhs, final long rhs) {
            return LargeIntegerObject.createLongMinOverflowResult(method.image);
        }

        @Specialization(guards = {"!rhs.isZero()", "rhs.fitsIntoLong()", "!isOverflowDivision(lhs, rhs.longValue())"})
        protected static final long doLongLargeIntegerAsLong(final long lhs, final LargeIntegerObject rhs) {
            return doLong(lhs, rhs.longValue());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"rhs.fitsIntoLong()", "isOverflowDivision(lhs, rhs.longValue())"})
        protected final LargeIntegerObject doLongLargeIntegerWithOverflow(final long lhs, final LargeIntegerObject rhs) {
            return LargeIntegerObject.createLongMinOverflowResult(method.image);
        }

        @Specialization(guards = {"!rhs.isZero()", "!rhs.fitsIntoLong()"})
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return LargeIntegerObject.divide(lhs, rhs);
        }

        @Specialization(guards = "!rhs.isZero()")
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.divide(rhs);
        }

        @Specialization(guards = "rhs != 0")
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final long rhs) {
            return lhs.divide(rhs);
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

        @Specialization(guards = {"arg.fitsIntoLong() || receiver >= 0"})
        protected static final long doLongLargeAsLong(final long receiver, final LargeIntegerObject arg) {
            return receiver & arg.longValue();
        }

        @Specialization(guards = {"!arg.fitsIntoLong()", "receiver < 0"})
        protected static final Object doLongLargeNegative(final long receiver, final LargeIntegerObject arg) {
            return arg.and(receiver);
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

        @Specialization(guards = {"rhs != 0"})
        protected static final long doLong(final long lhs, final long rhs) {
            return lhs % rhs;
        }

        @Specialization(guards = {"!rhs.isZero()"})
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.remainder(rhs);
        }

        @Specialization(guards = {"!rhs.isZero()", "rhs.fitsIntoLong()"})
        protected static final long doLong(final long lhs, final LargeIntegerObject rhs) {
            return doLong(lhs, rhs.longValue());
        }

        @Specialization(guards = {"!rhs.isZero()", "!rhs.fitsIntoLong()"})
        protected final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return doLargeInteger(asLargeInteger(lhs), rhs);
        }

        @Specialization(guards = {"rhs != 0"})
        protected final Object doLargeInteger(final LargeIntegerObject lhs, final long rhs) {
            return doLargeInteger(lhs, asLargeInteger(rhs));
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

        @Specialization(guards = "isZero(receiver)")
        protected static final long doDoubleZero(@SuppressWarnings("unused") final double receiver) {
            return 0L;
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
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {54, 554})
    protected abstract static class PrimFloatTimesTwoPowerNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloatTimesTwoPowerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"!isZero(matissa)", "isFinite(matissa)", "exponent != 0"})
        protected final Object doDoubleLong(final double matissa, final long exponent,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return doDouble(matissa, exponent, isFiniteProfile);
        }

        @Specialization(guards = {"!isZero(matissa)", "isFinite(matissa)", "exponent == 0"})
        protected static final double doDoubleExponentLongZero(final double matissa, @SuppressWarnings("unused") final long exponent) {
            return matissa;
        }

        @Specialization(guards = {"!isZero(matissa)", "isFinite(matissa)", "!isZero(exponent)"})
        protected final Object doDouble(final double matissa, final double exponent,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            final double steps = Math.min(3, Math.ceil(Math.abs(exponent) / 1023));
            double result = matissa;
            for (int i = 0; i < steps; i++) {
                final double pow = Math.pow(2, Math.floor((exponent + i) / steps));
                assert pow != Double.POSITIVE_INFINITY && pow != Double.NEGATIVE_INFINITY;
                result *= pow;
            }
            return FloatObject.boxIfNecessary(method.image, result, isFiniteProfile);
        }

        @Specialization(guards = {"!isZero(matissa)", "isFinite(matissa)", "isZero(exponent)"})
        protected static final double doDoubleExponentZero(final double matissa, @SuppressWarnings("unused") final double exponent) {
            return matissa;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isZero(matissa)"})
        protected static final double doDoubleMatissaZero(final double matissa, final Object exponent) {
            return 0D;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!matissa.isFinite()"})
        protected static final FloatObject doDoubleNotFinite(final FloatObject matissa, final Object exponent) {
            return matissa;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {55, 555})
    protected abstract static class PrimSquareRootNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimSquareRootNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isFinite(receiver)", "isZeroOrGreater(receiver)"})
        protected static final double doDouble(final double receiver) {
            return Math.sqrt(receiver);
        }

        @Specialization(guards = "!receiver.isFinite()")
        protected static final FloatObject doNotFinite(final FloatObject receiver) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {56, 556})
    protected abstract static class PrimSinNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimSinNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "isFinite(receiver)")
        protected static final double doDouble(final double receiver) {
            return Math.sin(receiver);
        }

        @Specialization(guards = "!receiver.isFinite()")
        protected final FloatObject doInfinite(@SuppressWarnings("unused") final FloatObject receiver) {
            return method.image.asFloatObject(Double.NaN);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {57, 557})
    protected abstract static class PrimArcTanNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimArcTanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "!isNaN(receiver)")
        protected static final double doDouble(final double receiver) {
            return Math.atan(receiver);
        }

        @Specialization(guards = "receiver.isNaN()")
        protected static final FloatObject doInfinite(final FloatObject receiver) {
            return receiver;
        }
    }

    @ImportStatic(Double.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = {58, 558})
    protected abstract static class PrimLogNNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimLogNNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "isFinite(receiver)")
        protected static final double doDouble(final double receiver) {
            return Math.log(receiver);
        }

        @Specialization(guards = "!receiver.isFinite()")
        protected static final FloatObject doInfinite(final FloatObject receiver) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {59, 559})
    protected abstract static class PrimExpNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimExpNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "isFinite(receiver)")
        protected final Object doDouble(final double receiver,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return FloatObject.boxIfNecessary(method.image, Math.exp(receiver), isFiniteProfile);
        }

        @Specialization(guards = "!receiver.isFinite()")
        protected static final FloatObject doInfinite(final FloatObject receiver) {
            return receiver;
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

    @ImportStatic(Double.class)
    @TypeSystemReference(ArithmeticBaseTypeSystem.class)
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

        protected static final boolean sameSign(final long lhs, final long rhs) {
            return lhs >= 0 && rhs >= 0 || lhs < 0 && rhs < 0;
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArithmeticPrimitivesFactory.getFactories();
    }
}
