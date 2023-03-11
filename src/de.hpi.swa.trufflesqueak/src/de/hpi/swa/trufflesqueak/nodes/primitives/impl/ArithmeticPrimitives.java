/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigDecimal;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.ExactMath;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.RespecializeException;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakGuards;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.FloatObjectNodes.AsFloatObjectIfNessaryNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.BinaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.TernaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.UnaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

public final class ArithmeticPrimitives extends AbstractPrimitiveFactoryHolder {
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 1)
    protected abstract static class PrimAddNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs) {
            return Math.addExact(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        protected final Object doLongWithOverflow(final long lhs, final long rhs) {
            return LargeIntegerObject.add(getContext(), lhs, rhs);
        }

        @Specialization
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return rhs.add(lhs);
        }

        @Specialization
        protected static final double doLongDouble(final long lhs, final double rhs) {
            return lhs + rhs;
        }

        @Specialization
        protected static final Object doLongFloat(final long lhs, final FloatObject rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs + rhs.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 2)
    protected abstract static class PrimSubtractNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs) {
            return Math.subtractExact(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        protected final Object doLongWithOverflow(final long lhs, final long rhs) {
            return LargeIntegerObject.subtract(getContext(), lhs, rhs);
        }

        @Specialization
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return LargeIntegerObject.subtract(lhs, rhs);
        }

        @Specialization
        protected static final double doLongDouble(final long lhs, final double rhs) {
            return lhs - rhs;
        }

        @Specialization
        protected static final Object doLongFloat(final long lhs, final FloatObject rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs - rhs.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 3)
    protected abstract static class PrimLessThanNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs < rhs);
        }

        @Specialization
        protected static final boolean doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) >= 0);
        }

        @Specialization(guards = "isExactDouble(lhs)")
        protected static final boolean doLongDouble(final long lhs, final double rhs) {
            return BooleanObject.wrap(lhs < rhs);
        }

        @Specialization(guards = "!isExactDouble(lhs)")
        protected static final boolean doLongDoubleNotExact(final long lhs, final double rhs) {
            return BooleanObject.wrap(compareNotExact(lhs, rhs) < 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 4)
    protected abstract static class PrimGreaterThanNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs > rhs);
        }

        @Specialization
        protected static final boolean doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) <= 0);
        }

        @Specialization(guards = "isExactDouble(lhs)")
        protected static final boolean doLongDouble(final long lhs, final double rhs) {
            return BooleanObject.wrap(lhs > rhs);
        }

        @Specialization(guards = "isExactDouble(lhs)")
        protected static final boolean doLongDoubleNotExact(final long lhs, final double rhs) {
            return BooleanObject.wrap(compareNotExact(lhs, rhs) > 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 5)
    protected abstract static class PrimLessOrEqualNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs <= rhs);
        }

        @Specialization
        protected static final boolean doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) > 0);
        }

        @Specialization(guards = "isExactDouble(lhs)")
        protected static final boolean doLongDouble(final long lhs, final double rhs) {
            return BooleanObject.wrap(lhs <= rhs);
        }

        @Specialization(guards = "isExactDouble(lhs)")
        protected static final boolean doLongDoubleNotExact(final long lhs, final double rhs) {
            return BooleanObject.wrap(compareNotExact(lhs, rhs) <= 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 6)
    protected abstract static class PrimGreaterOrEqualNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs >= rhs);
        }

        @Specialization
        protected static final boolean doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) < 0);
        }

        @Specialization(guards = "isExactDouble(lhs)")
        protected static final boolean doLongDouble(final long lhs, final double rhs) {
            return BooleanObject.wrap(lhs >= rhs);
        }

        @Specialization(guards = "isExactDouble(lhs)")
        protected static final boolean doLongDoubleNotExact(final long lhs, final double rhs) {
            return BooleanObject.wrap(compareNotExact(lhs, rhs) >= 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 7)
    protected abstract static class PrimEqualNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs == rhs);
        }

        @Specialization
        protected static final boolean doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) == 0);
        }

        @Specialization(guards = "isExactDouble(lhs)")
        protected static final boolean doLongExactDouble(final long lhs, final double rhs) {
            return BooleanObject.wrap(lhs == rhs);
        }

        /** Quick return `false` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(rhs)", "!isPointersObject(rhs)"})
        protected static final boolean doQuickFalse(final long lhs, final AbstractSqueakObject rhs) {
            return BooleanObject.FALSE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 8)
    protected abstract static class PrimNotEqualNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs != rhs);
        }

        @Specialization
        protected static final boolean doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) != 0);
        }

        @Specialization(guards = "isExactDouble(lhs)")
        protected static final boolean doLongExactDouble(final long lhs, final double rhs) {
            return BooleanObject.wrap(lhs != rhs);
        }

        /** Quick return `true` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(rhs)", "!isPointersObject(rhs)"})
        protected static final boolean doQuickTrue(final long lhs, final AbstractSqueakObject rhs) {
            return BooleanObject.TRUE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 9)
    protected abstract static class PrimMultiplyNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs) {
            return Math.multiplyExact(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        protected final Object doLongWithOverflow(final long lhs, final long rhs) {
            return LargeIntegerObject.multiply(getContext(), lhs, rhs);
        }

        @Specialization
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return rhs.multiply(lhs);
        }

        @Specialization(rewriteOn = RespecializeException.class)
        protected static final double doLongDoubleFinite(final long lhs, final double rhs) throws RespecializeException {
            return ensureFinite(lhs * rhs);
        }

        @Specialization(replaces = "doLongDoubleFinite")
        protected static final Object doLongDouble(final long lhs, final double rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs * rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 10)
    protected abstract static class PrimDivideNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)", "isIntegralWhenDividedBy(lhs, rhs)"})
        protected static final long doLong(final long lhs, final long rhs) {
            return lhs / rhs;
        }

        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)"}, replaces = "doLong")
        protected final Object doLongFraction(final long lhs, final long rhs,
                        @Cached final ConditionProfile fractionProfile,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            if (fractionProfile.profile(SqueakGuards.isIntegralWhenDividedBy(lhs, rhs))) {
                return lhs / rhs;
            } else {
                return getContext().asFraction(lhs, rhs, writeNode);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isOverflowDivision(lhs, rhs)"})
        protected final LargeIntegerObject doLongOverflow(final long lhs, final long rhs) {
            return LargeIntegerObject.createLongMinOverflowResult(getContext());
        }

        @Specialization(guards = {"!isZero(rhs)"}, rewriteOn = RespecializeException.class)
        protected static final double doLongDoubleFinite(final long lhs, final double rhs) throws RespecializeException {
            return ensureFinite(lhs / rhs);
        }

        @Specialization(guards = {"!isZero(rhs)"}, replaces = "doLongDoubleFinite")
        protected static final Object doLongDouble(final long lhs, final double rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs / rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 11)
    protected abstract static class PrimFloorModNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = "rhs != 0")
        protected static final long doLong(final long lhs, final long rhs) {
            return Math.floorMod(lhs, rhs);
        }

        @Specialization(guards = "!rhs.isZero()")
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return rhs.floorModReverseOrder(lhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 12)
    protected abstract static class PrimFloorDivideNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)"})
        protected static final long doLong(final long lhs, final long rhs) {
            return Math.floorDiv(lhs, rhs);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isOverflowDivision(lhs, rhs)"})
        protected final LargeIntegerObject doLongOverflowDivision(final long lhs, final long rhs) {
            return LargeIntegerObject.createLongMinOverflowResult(getContext());
        }

        @Specialization(guards = {"!rhs.isZero()"})
        protected static final long doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return LargeIntegerObject.floorDivide(lhs, rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 13)
    protected abstract static class PrimQuoNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)"})
        protected static final long doLong(final long lhs, final long rhs) {
            return lhs / rhs;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isOverflowDivision(lhs, rhs)"})
        protected final LargeIntegerObject doLongOverflow(final long lhs, final long rhs) {
            return LargeIntegerObject.createLongMinOverflowResult(getContext());
        }

        @Specialization(guards = {"!rhs.isZero()"})
        protected static final long doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return LargeIntegerObject.divide(lhs, rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 14)
    protected abstract static class PrimBitAndNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final long doLong(final long receiver, final long arg) {
            return receiver & arg;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLongLargeQuick(final long receiver, final LargeIntegerObject arg,
                        @Cached final ConditionProfile positiveProfile) {
            return receiver & (positiveProfile.profile(receiver >= 0) ? arg.longValue() : arg.longValueExact());
        }

        @Specialization(replaces = "doLongLargeQuick")
        protected static final Object doLongLarge(final long receiver, final LargeIntegerObject arg) {
            return arg.and(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 15)
    protected abstract static class PrimBitOrNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final long doLong(final long receiver, final long arg) {
            return receiver | arg;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLongLargeQuick(final long receiver, final LargeIntegerObject arg) {
            return receiver | arg.longValueExact();
        }

        @Specialization(replaces = "doLongLargeQuick")
        protected static final Object doLongLarge(final long receiver, final LargeIntegerObject arg) {
            return arg.or(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 16)
    protected abstract static class PrimBitXorNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final long doLong(final long receiver, final long arg) {
            return receiver ^ arg;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLongLargeQuick(final long receiver, final LargeIntegerObject arg) {
            return receiver ^ arg.longValueExact();
        }

        @Specialization(replaces = "doLongLargeQuick")
        protected static final Object doLongLarge(final long receiver, final LargeIntegerObject arg) {
            return arg.xor(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 17)
    protected abstract static class PrimBitShiftNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = {"arg >= 0", "!isLShiftLongOverflow(receiver, arg)"})
        protected static final long doLongPositive(final long receiver, final long arg) {
            return receiver << arg;
        }

        @Specialization(guards = {"arg >= 0", "isLShiftLongOverflow(receiver, arg)"})
        protected final Object doLongPositiveOverflow(final long receiver, final long arg) {
            /*
             * -1 in check needed, because we do not want to shift a positive long into negative
             * long (most significant bit indicates positive/negative).
             */
            return LargeIntegerObject.shiftLeftPositive(getContext(), receiver, (int) arg);
        }

        @Specialization(guards = {"arg < 0", "inLongSizeRange(arg)"})
        protected static final long doLongNegativeInLongSizeRange(final long receiver, final long arg) {
            /*
             * The result of a right shift can only become smaller than the receiver and 0 or -1 at
             * minimum, so no BigInteger needed here.
             */
            return receiver >> -arg;
        }

        @Specialization(guards = {"arg < 0", "!inLongSizeRange(arg)"})
        protected static final long doLongNegative(final long receiver, @SuppressWarnings("unused") final long arg) {
            return receiver >= 0 ? 0L : -1L;
        }

        protected static final boolean isLShiftLongOverflow(final long receiver, final long arg) {
            return Long.numberOfLeadingZeros(receiver) - 1 < arg;
        }

        protected static final boolean inLongSizeRange(final long arg) {
            return -Long.SIZE < arg;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 18)
    protected abstract static class PrimMakePointNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {
        // TODO: Object/Object specialization sufficient
        @Specialization
        protected final PointersObject doLong(final long xPos, final Object yPos,
                        @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
            return getContext().asPoint(writeNode, xPos, yPos);
        }

        @Specialization
        protected final PointersObject doDouble(final double xPos, final Object yPos,
                        @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
            return getContext().asPoint(writeNode, xPos, yPos);
        }

        @Specialization
        protected final PointersObject doLargeInteger(final LargeIntegerObject xPos, final Object yPos,
                        @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
            return getContext().asPoint(writeNode, xPos, yPos);
        }

        @Specialization
        protected final PointersObject doFloatObject(final FloatObject xPos, final Object yPos,
                        @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
            return getContext().asPoint(writeNode, xPos, yPos);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 20)
    protected abstract static class PrimRemLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = {"rhs != 0"})
        protected static final long doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.remainder(rhs);
        }

        @Specialization(guards = {"!rhs.isZero()"})
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.remainder(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 21)
    protected abstract static class PrimAddLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.add(rhs);
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.add(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 22)
    protected abstract static class PrimSubtractLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.subtract(rhs);
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.subtract(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 23)
    protected abstract static class PrimLessThanLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) < 0);
        }

        @Specialization
        protected static final boolean doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) < 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 24)
    protected abstract static class PrimGreaterThanLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) > 0);
        }

        @Specialization
        protected static final boolean doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) > 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 25)
    protected abstract static class PrimLessOrEqualLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) <= 0);
        }

        @Specialization
        protected static final boolean doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) <= 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 26)
    protected abstract static class PrimGreaterOrEqualLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) >= 0);
        }

        @Specialization
        protected static final boolean doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) >= 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 27)
    protected abstract static class PrimEqualLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, @SuppressWarnings("unused") final long rhs) {
            assert !lhs.fitsIntoLong() : "non-reduced large integer!";
            return BooleanObject.FALSE;
        }

        @Specialization
        protected static final boolean doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) == 0);
        }

        /** Quick return `false` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(rhs)", "!isPointersObject(rhs)"})
        protected static final boolean doQuickFalse(final LargeIntegerObject lhs, final AbstractSqueakObject rhs) {
            return BooleanObject.FALSE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 28)
    protected abstract static class PrimNotEqualLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, @SuppressWarnings("unused") final long rhs) {
            assert !lhs.fitsIntoLong() : "non-reduced large integer!";
            return BooleanObject.TRUE;
        }

        @Specialization
        protected static final boolean doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) != 0);
        }

        /** Quick return `true` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(rhs)", "!isPointersObject(rhs)"})
        protected static final boolean doQuickTrue(final Object lhs, final AbstractSqueakObject rhs) {
            return BooleanObject.TRUE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 29)
    protected abstract static class PrimMultiplyLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.multiply(rhs);
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.multiply(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 30)
    protected abstract static class PrimDivideLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = {"rhs != 0", "lhs.isIntegralWhenDividedBy(rhs)"})
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.divide(rhs);
        }

        @Specialization(guards = {"!rhs.isZero()", "lhs.isIntegralWhenDividedBy(rhs)"})
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.divide(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 31)
    protected abstract static class PrimFloorModLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.floorMod(rhs);
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.floorMod(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 32)
    protected abstract static class PrimFloorDivideLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = "rhs != 0")
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.floorDivide(rhs);
        }

        @Specialization(guards = "!rhs.isZero()")
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.floorDivide(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 33)
    protected abstract static class PrimQuoLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = "rhs != 0")
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final long rhs) {
            return lhs.divide(rhs);
        }

        @Specialization(guards = "!rhs.isZero()")
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.divide(rhs);
        }
    }

    // Squeak/Smalltalk uses LargeIntegers plugin for bit operations instead of primitives 34 to 37.

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 38)
    protected abstract static class PrimFloatAtNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = "index == 1")
        protected static final long doDoubleHigh(final double receiver, @SuppressWarnings("unused") final long index) {
            return Integer.toUnsignedLong((int) (Double.doubleToRawLongBits(receiver) >> 32));
        }

        @Specialization(guards = "index == 2")
        protected static final long doDoubleLow(final double receiver, @SuppressWarnings("unused") final long index) {
            return Integer.toUnsignedLong((int) Double.doubleToRawLongBits(receiver));
        }

        @Specialization(guards = "index == 1")
        protected static final long doFloatHigh(final FloatObject receiver, final long index) {
            return doDoubleHigh(receiver.getValue(), index);
        }

        @Specialization(guards = "index == 2")
        protected static final long doFloatLow(final FloatObject receiver, final long index) {
            return doDoubleLow(receiver.getValue(), index);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"index != 1", "index != 2"})
        protected static final long doDoubleFail(final Object receiver, final long index) {
            throw PrimitiveFailed.BAD_INDEX;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 39)
    protected abstract static class PrimFloatAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitiveFallback {
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
        @Specialization(guards = {"index != 1", "index != 2"})
        protected static final long doFloatFail(final FloatObject receiver, final long index, final long value) {
            throw PrimitiveFailed.BAD_INDEX;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 40)
    protected abstract static class PrimAsFloatNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization
        protected static final double doLong(final long receiver) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 41)
    protected abstract static class PrimAddFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final Object doDouble(final FloatObject lhs, final double rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs.getValue() + rhs);
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final Object doLong(final FloatObject lhs, final long rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return doDouble(lhs, rhs, boxNode);
        }

        @Specialization
        protected static final Object doFloat(final FloatObject lhs, final FloatObject rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return doDouble(lhs, rhs.getValue(), boxNode);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 42)
    protected abstract static class PrimSubtractFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final Object doDouble(final FloatObject lhs, final double rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs.getValue() - rhs);
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final Object doLong(final FloatObject lhs, final long rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return doDouble(lhs, rhs, boxNode);
        }

        @Specialization
        protected static final Object doFloat(final FloatObject lhs, final FloatObject rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return doDouble(lhs, rhs.getValue(), boxNode);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 43)
    protected abstract static class PrimLessThanFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doDouble(final FloatObject lhs, final double rhs) {
            return BooleanObject.wrap(lhs.getValue() < rhs);
        }

        @Specialization
        protected static final boolean doFloat(final FloatObject lhs, final FloatObject rhs) {
            return BooleanObject.wrap(lhs.getValue() < rhs.getValue());
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final boolean doDoubleLong(final FloatObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.getValue() < rhs);
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final boolean doDoubleLongNotExact(final FloatObject lhs, final long rhs) {
            return BooleanObject.wrap(compareNotExact(lhs.getValue(), rhs) < 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 44)
    protected abstract static class PrimGreaterThanFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doDouble(final FloatObject lhs, final double rhs) {
            return BooleanObject.wrap(lhs.getValue() > rhs);
        }

        @Specialization
        protected static final boolean doFloat(final FloatObject lhs, final FloatObject rhs) {
            return BooleanObject.wrap(lhs.getValue() > rhs.getValue());
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final boolean doDoubleLong(final FloatObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.getValue() > rhs);
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final boolean doDoubleLongNotExact(final FloatObject lhs, final long rhs) {
            return BooleanObject.wrap(compareNotExact(lhs.getValue(), rhs) > 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 45)
    protected abstract static class PrimLessOrEqualFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doDouble(final FloatObject lhs, final double rhs) {
            return BooleanObject.wrap(lhs.getValue() <= rhs);
        }

        @Specialization
        protected static final boolean doFloat(final FloatObject lhs, final FloatObject rhs) {
            return BooleanObject.wrap(lhs.getValue() <= rhs.getValue());
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final boolean doDoubleLong(final FloatObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.getValue() <= rhs);
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final boolean doDoubleLongNotExact(final FloatObject lhs, final long rhs) {
            return BooleanObject.wrap(compareNotExact(lhs.getValue(), rhs) <= 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 46)
    protected abstract static class PrimGreaterOrEqualFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doDouble(final FloatObject lhs, final double rhs) {
            return BooleanObject.wrap(lhs.getValue() >= rhs);
        }

        @Specialization
        protected static final boolean doFloat(final FloatObject lhs, final FloatObject rhs) {
            return BooleanObject.wrap(lhs.getValue() >= rhs.getValue());
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final boolean doDoubleLong(final FloatObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.getValue() >= rhs);
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final boolean doDoubleLongNotExact(final FloatObject lhs, final long rhs) {
            return BooleanObject.wrap(compareNotExact(lhs.getValue(), rhs) >= 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 47)
    protected abstract static class PrimEqualFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doDouble(final FloatObject lhs, final double rhs) {
            return BooleanObject.wrap(lhs.getValue() == rhs);
        }

        @Specialization
        protected static final boolean doFloat(final FloatObject lhs, final FloatObject rhs) {
            return BooleanObject.wrap(lhs.getValue() == rhs.getValue());
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final boolean doDoubleLong(final FloatObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.getValue() == rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 48)
    protected abstract static class PrimNotEqualFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doDouble(final FloatObject lhs, final double rhs) {
            return BooleanObject.wrap(lhs.getValue() != rhs);
        }

        @Specialization
        protected static final boolean doFloat(final FloatObject lhs, final FloatObject rhs) {
            return BooleanObject.wrap(lhs.getValue() != rhs.getValue());
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final boolean doDoubleLong(final FloatObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.getValue() != rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 49)
    protected abstract static class PrimMultiplyFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final Object doDouble(final FloatObject lhs, final double rhs,
                        @Shared("boxNode") @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs.getValue() * rhs);
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final Object doLong(final FloatObject lhs, final long rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return doDouble(lhs, rhs, boxNode);
        }

        @Specialization
        protected static final Object doFloat(final FloatObject lhs, final FloatObject rhs,
                        @Shared("boxNode") @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs.getValue() * rhs.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 50)
    protected abstract static class PrimDivideFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = {"!isZero(rhs)"})
        protected static final Object doDouble(final FloatObject lhs, final double rhs,
                        @Shared("boxNode") @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs.getValue() / rhs);
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final Object doLong(final FloatObject lhs, final long rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return doDouble(lhs, rhs, boxNode);
        }

        @Specialization(guards = {"!rhs.isZero()"})
        protected static final Object doFloat(final FloatObject lhs, final FloatObject rhs,
                        @Shared("boxNode") @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return doDouble(lhs, rhs.getValue(), boxNode);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 51)
    protected abstract static class PrimFloatTruncatedNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization(guards = "inSafeIntegerRange(receiver.getValue())")
        protected static final long doFloat(final FloatObject receiver) {
            assert receiver.isFinite();
            final double value = receiver.getValue();
            return (long) ExactMath.truncate(value);
        }

        @Specialization(guards = {"!inSafeIntegerRange(receiver.getValue())", "receiver.isFinite()"})
        protected final Object doFloatExact(final FloatObject receiver) {
            return LargeIntegerObject.truncateExact(getContext(), receiver.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 52)
    protected abstract static class PrimFloatFractionPartNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization(guards = "inSafeIntegerRange(receiver.getValue())")
        protected static final double doFloat(final FloatObject receiver) {
            return receiver.getValue() - (long) receiver.getValue();
        }

        @TruffleBoundary
        @Specialization(guards = {"!inSafeIntegerRange(receiver.getValue())", "receiver.isFinite()"})
        protected static final double doFloatExact(final FloatObject receiver) {
            return receiver.getValue() - new BigDecimal(receiver.getValue()).toBigInteger().doubleValue();
        }

        @Specialization(guards = "receiver.isNaN()")
        protected static final FloatObject doFloatNaN(final FloatObject receiver) {
            return receiver.shallowCopy();
        }

        @Specialization(guards = "receiver.isInfinite()")
        protected static final double doFloatInfinite(@SuppressWarnings("unused") final FloatObject receiver,
                        @Cached final ConditionProfile isNegativeInfinityProfile) {
            return isNegativeInfinityProfile.profile(receiver.getValue() == Double.NEGATIVE_INFINITY) ? -0.0D : 0.0D;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 53)
    protected abstract static class PrimFloatExponentNode extends AbstractFloatArithmeticPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization(guards = "receiver.isZero()")
        protected static final long doFloatZero(@SuppressWarnings("unused") final FloatObject receiver) {
            return 0L;
        }

        @Specialization(guards = "!receiver.isZero()")
        protected static final long doFloat(final FloatObject receiver,
                        @Cached final BranchProfile subnormalFloatProfile) {
            return exponentNonZero(receiver.getValue(), subnormalFloatProfile);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 54)
    protected abstract static class PrimFloatTimesTwoPowerNode extends AbstractFloatArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = "matissa.isZero() || isZero(exponent)")
        protected static final FloatObject doDoubleZero(final FloatObject matissa, @SuppressWarnings("unused") final long exponent) {
            return matissa; /* Can be either 0.0 or -0.0. */
        }

        @Specialization(guards = {"!matissa.isZero()", "!isZero(exponent)"}, rewriteOn = RespecializeException.class)
        protected static final double doDoubleFinite(final FloatObject matissa, final long exponent) throws RespecializeException {
            return ensureFinite(timesToPower(matissa.getValue(), exponent));
        }

        @Specialization(guards = {"!matissa.isZero()", "!isZero(exponent)"}, replaces = "doDoubleFinite")
        protected static final Object doDouble(final FloatObject matissa, final long exponent,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(timesToPower(matissa.getValue(), exponent));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 55)
    protected abstract static class PrimSquareRootNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization(guards = {"receiver.isPositive()", "receiver.isFinite()"})
        protected static final double doFloat(final FloatObject receiver) {
            return Math.sqrt(receiver.getValue());
        }

        @Specialization(guards = {"receiver.isPositiveInfinity()"})
        protected static final FloatObject doFloatPositiveInfinity(final FloatObject receiver) {
            return receiver.shallowCopy();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 56)
    protected abstract static class PrimSinNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization(guards = {"receiver.isFinite()"})
        protected static final double doFloat(final FloatObject receiver) {
            return Math.sin(receiver.getValue());
        }

        @Specialization(guards = {"!receiver.isFinite()"})
        protected final FloatObject doFloatNotFinite(@SuppressWarnings("unused") final FloatObject receiver) {
            return FloatObject.valueOf(getContext(), Double.NaN);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 57)
    protected abstract static class PrimArcTanNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization(guards = {"!receiver.isNaN()"})
        protected static final double doFloat(final FloatObject receiver) {
            return Math.atan(receiver.getValue());
        }

        @Specialization(guards = {"receiver.isNaN()"})
        protected static final FloatObject doFloatNaN(final FloatObject receiver) {
            return receiver.shallowCopy();
        }
    }

    @ImportStatic(Double.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 58)
    protected abstract static class PrimLogNNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization(guards = {"receiver.isFinite()", "!receiver.isZero()"})
        protected static final double doFloat(final FloatObject receiver) {
            return Math.log(receiver.getValue());
        }

        @Specialization(guards = "receiver.isZero()")
        protected final FloatObject doFloatZero(@SuppressWarnings("unused") final FloatObject receiver) {
            return FloatObject.valueOf(getContext(), Double.NEGATIVE_INFINITY);
        }

        @Specialization(guards = "receiver.isPositiveInfinity()")
        protected static final FloatObject doFloatPositiveInfinity(final FloatObject receiver) {
            return receiver.shallowCopy();
        }

        @Specialization(guards = "receiver.isNegativeInfinity() || receiver.isNaN()")
        protected final FloatObject doFloatOthers(@SuppressWarnings("unused") final FloatObject receiver) {
            return FloatObject.valueOf(getContext(), Double.NaN);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 59)
    protected abstract static class PrimExpNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization(guards = "receiver.isFinite()")
        protected static final Object doFloat(final FloatObject receiver,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(Math.exp(receiver.getValue()));
        }

        @Specialization(guards = "receiver.isNegativeInfinity()")
        protected static final double doFloatNegativeInfinity(@SuppressWarnings("unused") final FloatObject receiver) {
            return 0.0;
        }

        @Specialization(guards = "receiver.isPositiveInfinity() || receiver.isNaN()")
        protected static final FloatObject doFloatOthers(final FloatObject receiver) {
            return receiver.shallowCopy();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 159)
    public abstract static class PrimHashMultiplyNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitiveFallback {
        public static final int HASH_MULTIPLY_CONSTANT = 1664525;
        public static final int HASH_MULTIPLY_MASK = 0xFFFFFFF;

        @Specialization
        protected static final long doLargeInteger(final LargeIntegerObject receiver) {
            return doLong(receiver.longValue());
        }

        @Specialization
        protected static final long doLong(final long receiver) {
            return receiver * HASH_MULTIPLY_CONSTANT & HASH_MULTIPLY_MASK;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 541)
    protected abstract static class PrimSmallFloatAddFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final double doDouble(final double lhs, final double rhs) {
            return lhs + rhs;
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final double doLong(final double lhs, final long rhs) {
            return doDouble(lhs, rhs);
        }

        @Specialization
        protected static final Object doFloat(final double lhs, final FloatObject rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs + rhs.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 542)
    protected abstract static class PrimSmallFloatSubtractFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final double doDouble(final double lhs, final double rhs) {
            return lhs - rhs;
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final double doLong(final double lhs, final long rhs) {
            return doDouble(lhs, rhs);
        }

        @Specialization
        protected static final Object doFloat(final double lhs, final FloatObject rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs - rhs.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 543)
    protected abstract static class PrimSmallFloatLessThanFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs < rhs);
        }

        @Specialization
        protected static final boolean doFloat(final double lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final boolean doDoubleLong(final double lhs, final long rhs) {
            return doDouble(lhs, rhs);
        }

        @Specialization(guards = "!isExactDouble(rhs)")
        protected static final boolean doDoubleLongNotExact(final double lhs, final long rhs) {
            return BooleanObject.wrap(compareNotExact(lhs, rhs) < 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 544)
    protected abstract static class PrimSmallFloatGreaterThanFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs > rhs);
        }

        @Specialization
        protected static final boolean doFloat(final double lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final boolean doDoubleLong(final double lhs, final long rhs) {
            return doDouble(lhs, rhs);
        }

        @Specialization(guards = "!isExactDouble(rhs)")
        protected static final boolean doDoubleLongNotExact(final double lhs, final long rhs) {
            return BooleanObject.wrap(compareNotExact(lhs, rhs) > 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 545)
    protected abstract static class PrimSmallFloatLessOrEqualFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs <= rhs);
        }

        @Specialization
        protected static final boolean doFloat(final double lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final boolean doDoubleLong(final double lhs, final long rhs) {
            return doDouble(lhs, rhs);
        }

        @Specialization(guards = "!isExactDouble(rhs)")
        protected static final boolean doDoubleLongNotExact(final double lhs, final long rhs) {
            return BooleanObject.wrap(compareNotExact(lhs, rhs) <= 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 546)
    protected abstract static class PrimSmallFloatGreaterOrEqualFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs >= rhs);
        }

        @Specialization
        protected static final boolean doFloat(final double lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final boolean doDoubleLong(final double lhs, final long rhs) {
            return doDouble(lhs, rhs);
        }

        @Specialization(guards = "!isExactDouble(rhs)")
        protected static final boolean doDoubleLongNotExact(final double lhs, final long rhs) {
            return BooleanObject.wrap(compareNotExact(lhs, rhs) >= 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 547)
    protected abstract static class PrimSmallFloatEqualFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs == rhs);
        }

        @Specialization
        protected static final boolean doFloat(final double lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final boolean doDoubleLong(final double lhs, final long rhs) {
            return doDouble(lhs, rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 548)
    protected abstract static class PrimSmallFloatNotEqualFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs != rhs);
        }

        @Specialization
        protected static final boolean doFloat(final double lhs, final FloatObject rhs) {
            return doDouble(lhs, rhs.getValue());
        }

        @Specialization(guards = "isExactDouble(rhs)")
        protected static final boolean doDoubleLong(final double lhs, final long rhs) {
            return doDouble(lhs, rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 549)
    protected abstract static class PrimSmallFloatMultiplyFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(rewriteOn = RespecializeException.class)
        protected static final double doDoubleFinite(final double lhs, final double rhs) throws RespecializeException {
            return ensureFinite(lhs * rhs);
        }

        @Specialization(guards = "isExactDouble(rhs)", rewriteOn = RespecializeException.class)
        protected static final double doLongFinite(final double lhs, final long rhs) throws RespecializeException {
            return ensureFinite(lhs * rhs);
        }

        @Specialization(replaces = "doDoubleFinite")
        protected static final Object doDouble(final double lhs, final double rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs * rhs);
        }

        @Specialization(guards = "isExactDouble(rhs)", replaces = "doLongFinite")
        protected static final Object doLong(final double lhs, final long rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return doDouble(lhs, rhs, boxNode);
        }

        @Specialization
        protected static final Object doFloat(final double lhs, final FloatObject rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return doDouble(lhs, rhs.getValue(), boxNode);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 550)
    protected abstract static class PrimSmallFloatDivideFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"!isZero(rhs)"}, rewriteOn = RespecializeException.class)
        protected static final double doDoubleFinite(final double lhs, final double rhs) throws RespecializeException {
            return ensureFinite(lhs / rhs);
        }

        @Specialization(guards = {"!isZero(rhs)", "isExactDouble(rhs)"}, rewriteOn = RespecializeException.class)
        protected static final double doLongFinite(final double lhs, final long rhs) throws RespecializeException {
            return ensureFinite(lhs / rhs);
        }

        @Specialization(guards = {"!isZero(rhs)"}, replaces = "doDoubleFinite")
        protected static final Object doDouble(final double lhs, final double rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs / rhs);
        }

        @Specialization(guards = {"!isZero(rhs)", "isExactDouble(rhs)"}, replaces = "doLongFinite")
        protected static final Object doLong(final double lhs, final long rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return doDouble(lhs, rhs, boxNode);
        }

        @Specialization(guards = {"!rhs.isZero()"})
        protected static final Object doFloat(final double lhs, final FloatObject rhs,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return doDouble(lhs, rhs.getValue(), boxNode);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 551)
    protected abstract static class PrimSmallFloatTruncatedNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization(guards = "inSafeIntegerRange(receiver)")
        protected static final long doDouble(final double receiver,
                        @Cached final ConditionProfile positiveProfile) {
            return (long) (positiveProfile.profile(receiver >= 0) ? Math.floor(receiver) : Math.ceil(receiver));
        }

        @Specialization(guards = "!inSafeIntegerRange(receiver)")
        protected final Object doDoubleExact(final double receiver) {
            return LargeIntegerObject.truncateExact(getContext(), receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 552)
    protected abstract static class PrimSmallFloatFractionPartNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization(guards = "inSafeIntegerRange(receiver)")
        protected static final double doDouble(final double receiver) {
            return receiver - (long) receiver;
        }

        @TruffleBoundary
        @Specialization(guards = "!inSafeIntegerRange(receiver)")
        protected static final double doDoubleExact(final double receiver) {
            return receiver - new BigDecimal(receiver).toBigInteger().doubleValue();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 553)
    protected abstract static class PrimSmallFloatExponentNode extends AbstractFloatArithmeticPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization(guards = "isZero(receiver)")
        protected static final long doDoubleZero(@SuppressWarnings("unused") final double receiver) {
            return 0L;
        }

        @Specialization(guards = "!isZero(receiver)")
        protected static final long doDouble(final double receiver,
                        @Cached final BranchProfile subnormalFloatProfile) {
            return exponentNonZero(receiver, subnormalFloatProfile);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 554)
    protected abstract static class PrimSmallFloatTimesTwoPowerNode extends AbstractFloatArithmeticPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = "isZero(matissa) || isZero(exponent)")
        protected static final double doDoubleZero(final double matissa, @SuppressWarnings("unused") final long exponent) {
            return matissa; /* Can be either 0.0 or -0.0. */
        }

        @Specialization(guards = {"!isZero(matissa)", "!isZero(exponent)"}, rewriteOn = RespecializeException.class)
        protected static final double doDoubleFinite(final double matissa, final long exponent) throws RespecializeException {
            return ensureFinite(timesToPower(matissa, exponent));
        }

        @Specialization(guards = {"!isZero(matissa)", "!isZero(exponent)"}, replaces = "doDoubleFinite")
        protected static final Object doDouble(final double matissa, final long exponent,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(timesToPower(matissa, exponent));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 555)
    protected abstract static class PrimSquareRootSmallFloatNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization(guards = {"isZeroOrGreater(receiver)"})
        protected static final double doDouble(final double receiver) {
            assert Double.isFinite(receiver);
            return Math.sqrt(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 556)
    protected abstract static class PrimSinSmallFloatNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization
        protected static final double doDouble(final double receiver) {
            assert Double.isFinite(receiver);
            return Math.sin(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 557)
    protected abstract static class PrimArcTanSmallFloatNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization
        protected static final double doDouble(final double receiver) {
            assert Double.isFinite(receiver);
            return Math.atan(receiver);
        }
    }

    @ImportStatic(Double.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 558)
    protected abstract static class PrimLogNSmallFloatNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization(guards = "isGreaterThanZero(receiver)")
        protected static final double doDouble(final double receiver) {
            assert Double.isFinite(receiver);
            return Math.log(receiver);
        }

        @Specialization(guards = "isZero(receiver)")
        protected final FloatObject doFloatZero(@SuppressWarnings("unused") final double receiver) {
            return FloatObject.valueOf(getContext(), Double.NEGATIVE_INFINITY);
        }

        @Specialization(guards = "isLessThanZero(receiver)")
        protected final FloatObject doDoubleNegative(@SuppressWarnings("unused") final double receiver) {
            return FloatObject.valueOf(getContext(), Double.NaN);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 559)
    protected abstract static class PrimExpSmallFloatNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization(rewriteOn = RespecializeException.class)
        protected static final double doDoubleFinite(final double receiver) throws RespecializeException {
            assert Double.isFinite(receiver);
            return ensureFinite(Math.exp(receiver));
        }

        @Specialization(replaces = "doDoubleFinite")
        protected static final Object doDouble(final double receiver,
                        @Cached final AsFloatObjectIfNessaryNode boxNode) {
            assert Double.isFinite(receiver);
            return boxNode.execute(Math.exp(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 575)
    protected abstract static class PrimHighBitNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization
        protected static final long doLong(final long receiver,
                        @Cached final ConditionProfile negativeProfile) {
            return Long.SIZE - Long.numberOfLeadingZeros(negativeProfile.profile(receiver < 0) ? -receiver : receiver);
        }
    }

    @ImportStatic(Double.class)
    public abstract static class AbstractArithmeticPrimitiveNode extends AbstractPrimitiveNode {
        private static final long MAX_SAFE_INTEGER_LONG = (1L << FloatObject.PRECISION) - 1;
        private static final long MIN_SAFE_INTEGER_LONG = -MAX_SAFE_INTEGER_LONG;

        protected static final double ensureFinite(final double value) throws RespecializeException {
            if (Double.isFinite(value)) {
                return value;
            } else {
                throw RespecializeException.transferToInterpreterInvalidateAndThrow();
            }
        }

        @TruffleBoundary
        protected static final int compareNotExact(final double lhs, final long rhs) {
            return new BigDecimal(lhs).compareTo(new BigDecimal(rhs));
        }

        @TruffleBoundary
        protected static final int compareNotExact(final long lhs, final double rhs) {
            return new BigDecimal(lhs).compareTo(new BigDecimal(rhs));
        }

        protected static final boolean inSafeIntegerRange(final double d) {
            // The ends of the interval are also included, since they are powers of two
            return MIN_SAFE_INTEGER_LONG <= d && d <= MAX_SAFE_INTEGER_LONG;
        }

        protected static final boolean differentSign(final long lhs, final long rhs) {
            return lhs < 0 ^ rhs < 0;
        }
    }

    protected abstract static class AbstractFloatArithmeticPrimitiveNode extends AbstractArithmeticPrimitiveNode {
        private static final int LARGE_NUMBER_EXP = 64;
        private static final double LARGE_NUMBER = Math.pow(2, LARGE_NUMBER_EXP);

        protected static final double timesToPower(final double matissa, final long exponent) {
            return Math.scalb(matissa, (int) MiscUtils.clamp(exponent, Integer.MIN_VALUE, Integer.MAX_VALUE));
        }

        protected static final long exponentNonZero(final double receiver, final BranchProfile subnormalFloatProfile) {
            final int exp = Math.getExponent(receiver);
            if (exp == Double.MIN_EXPONENT - 1) {
                // we have a subnormal float (actual zero was handled above)
                subnormalFloatProfile.enter();
                // make it normal by multiplying a large number and subtract the number's exponent
                return Math.getExponent(receiver * LARGE_NUMBER) - LARGE_NUMBER_EXP;
            } else {
                return exp;
            }
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArithmeticPrimitivesFactory.getFactories();
    }
}
