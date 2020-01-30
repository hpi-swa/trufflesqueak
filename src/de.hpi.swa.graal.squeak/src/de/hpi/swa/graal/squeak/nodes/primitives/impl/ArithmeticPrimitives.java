/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.math.BigDecimal;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.BooleanObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.FloatObjectNodes.AsFloatObjectIfNessaryNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class ArithmeticPrimitives extends AbstractPrimitiveFactoryHolder {
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 1)
    public abstract static class PrimAddNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        public PrimAddNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs) {
            return Math.addExact(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        protected final Object doLongWithOverflow(final long lhs, final long rhs) {
            return LargeIntegerObject.add(method.image, lhs, rhs);
        }

        @Specialization
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return rhs.add(lhs);
        }

        @Specialization
        protected static final Object doLongDouble(final long lhs, final double rhs,
                        @Cached("create(method.image)") final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs + rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 2)
    public abstract static class PrimSubtractNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        public PrimSubtractNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs) {
            return Math.subtractExact(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        protected final Object doLongWithOverflow(final long lhs, final long rhs) {
            return LargeIntegerObject.subtract(method.image, lhs, rhs);
        }

        @Specialization
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return LargeIntegerObject.subtract(lhs, rhs);
        }

        @Specialization
        protected static final Object doLongDouble(final long lhs, final double rhs,
                        @Cached("create(method.image)") final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs - rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 3)
    protected abstract static class PrimLessThanNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimLessThanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs < rhs);
        }

        @Specialization
        protected static final boolean doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) >= 0);
        }

        @Specialization(guards = "isAnExactFloat(lhs)")
        protected static final boolean doLongDouble(final long lhs, final double rhs) {
            return BooleanObject.wrap(lhs < rhs);
        }

        @TruffleBoundary
        @Specialization(guards = "!isAnExactFloat(lhs)")
        protected static final boolean doLongNotExactDouble(final long lhs, final double rhs) {
            return new BigDecimal(lhs).compareTo(new BigDecimal(rhs)) < 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 4)
    protected abstract static class PrimGreaterThanNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimGreaterThanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs > rhs);
        }

        @Specialization
        protected static final boolean doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) <= 0);
        }

        @Specialization(guards = "isAnExactFloat(lhs)")
        protected static final boolean doLongDouble(final long lhs, final double rhs) {
            return BooleanObject.wrap(lhs > rhs);
        }

        @TruffleBoundary
        @Specialization(guards = "!isAnExactFloat(lhs)")
        protected static final boolean doLongNotExactDouble(final long lhs, final double rhs) {
            return new BigDecimal(lhs).compareTo(new BigDecimal(rhs)) > 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 5)
    protected abstract static class PrimLessOrEqualNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimLessOrEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs <= rhs);
        }

        @Specialization
        protected static final boolean doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) > 0);
        }

        @Specialization(guards = "isAnExactFloat(lhs)")
        protected static final boolean doLongDouble(final long lhs, final double rhs) {
            return BooleanObject.wrap(lhs <= rhs);
        }

        @TruffleBoundary
        @Specialization(guards = "!isAnExactFloat(lhs)")
        protected static final boolean doLongNotExactDouble(final long lhs, final double rhs) {
            return new BigDecimal(lhs).compareTo(new BigDecimal(rhs)) <= 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 6)
    protected abstract static class PrimGreaterOrEqualNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimGreaterOrEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs >= rhs);
        }

        @Specialization
        protected static final boolean doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) < 0);
        }

        @Specialization(guards = "isAnExactFloat(lhs)")
        protected static final boolean doLongDouble(final long lhs, final double rhs) {
            return BooleanObject.wrap(lhs >= rhs);
        }

        @TruffleBoundary
        @Specialization(guards = "!isAnExactFloat(lhs)")
        protected static final boolean doLongNotExactDouble(final long lhs, final double rhs) {
            return new BigDecimal(lhs).compareTo(new BigDecimal(rhs)) >= 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 7)
    protected abstract static class PrimEqualNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs == rhs);
        }

        @Specialization
        protected static final boolean doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) == 0);
        }

        @Specialization
        protected static final boolean doLongExactDouble(final long lhs, final double rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile isAnExactFloatProfile) {
            if (isAnExactFloatProfile.profile(isAnExactFloat(lhs))) {
                return BooleanObject.wrap(lhs == rhs);
            } else {
                return BooleanObject.FALSE;
            }
        }

        /** Quick return `false` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(rhs)", "!isPointersObject(rhs)"})
        protected static final boolean doQuickFalse(final Object lhs, final AbstractSqueakObject rhs) {
            return BooleanObject.FALSE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 8)
    protected abstract static class PrimNotEqualNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimNotEqualNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs != rhs);
        }

        @Specialization
        protected static final boolean doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return BooleanObject.wrap(rhs.compareTo(lhs) != 0);
        }

        @Specialization
        protected static final boolean doLongDouble(final long lhs, final double rhs,
                        @Cached("createBinaryProfile()") final ConditionProfile isAnExactFloatProfile) {
            if (isAnExactFloatProfile.profile(isAnExactFloat(lhs))) {
                return BooleanObject.wrap(lhs != rhs);
            } else {
                return BooleanObject.TRUE;
            }
        }

        /** Quick return `true` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(rhs)", "!isPointersObject(rhs)"})
        protected static final boolean doQuickTrue(final Object lhs, final AbstractSqueakObject rhs) {
            return BooleanObject.TRUE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 9)
    public abstract static class PrimMultiplyNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        public PrimMultiplyNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs) {
            return Math.multiplyExact(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        protected final Object doLongWithOverflow(final long lhs, final long rhs) {
            return LargeIntegerObject.multiply(method.image, lhs, rhs);
        }

        @Specialization
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return rhs.multiply(lhs);
        }

        @Specialization
        protected static final Object doLongDouble(final long lhs, final double rhs,
                        @Cached("create(method.image)") final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs * rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 10)
    protected abstract static class PrimDivideNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimDivideNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"rhs != 0", "isIntegralWhenDividedBy(lhs, rhs)"})
        public final Object doLong(final long lhs, final long rhs) {
            if (SqueakGuards.isOverflowDivision(lhs, rhs)) {
                return LargeIntegerObject.createLongMinOverflowResult(method.image);
            } else {
                return lhs / rhs;
            }
        }

        @Specialization(guards = {"!isZero(rhs)"})
        protected static final Object doLongDouble(final long lhs, final double rhs,
                        @Cached("create(method.image)") final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs / rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 11)
    protected abstract static class PrimFloorModNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloorModNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "rhs != 0")
        protected long doLong(final long lhs, final long rhs) {
            return Math.floorMod(lhs, rhs);
        }

        @Specialization(guards = "!rhs.isZero()")
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return LargeIntegerObject.floorMod(lhs, rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 12)
    protected abstract static class PrimFloorDivideNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloorDivideNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"rhs != 0"})
        protected final Object doLong(final long lhs, final long rhs) {
            if (SqueakGuards.isOverflowDivision(lhs, rhs)) {
                return LargeIntegerObject.createLongMinOverflowResult(method.image);
            } else {
                return Math.floorDiv(lhs, rhs);
            }
        }

        @Specialization(guards = {"!rhs.isZero()"})
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return LargeIntegerObject.floorDivide(lhs, rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 13)
    protected abstract static class PrimQuoNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimQuoNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"rhs != 0"})
        public final Object doLong(final long lhs, final long rhs) {
            if (SqueakGuards.isOverflowDivision(lhs, rhs)) {
                return LargeIntegerObject.createLongMinOverflowResult(method.image);
            } else {
                return lhs / rhs;
            }
        }

        @Specialization(guards = {"!rhs.isZero()"})
        protected static final Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
            return LargeIntegerObject.divide(lhs, rhs);
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

        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final Object doLongLargeQuick(final long receiver, final LargeIntegerObject arg,
                        @Cached("createBinaryProfile()") final ConditionProfile positiveProfile) {
            if (positiveProfile.profile(receiver >= 0)) {
                return receiver & arg.longValue();
            } else {
                return receiver & arg.longValueExact();
            }
        }

        @Specialization(replaces = "doLongLargeQuick")
        protected static final Object doLongLarge(final long receiver, final LargeIntegerObject arg) {
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
    protected abstract static class PrimBitXorNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimBitXorNode(final CompiledMethodObject method) {
            super(method);
        }

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
    public abstract static class PrimBitShiftNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimBitShiftNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doLong(final long receiver, final long arg,
                        @Cached("createBinaryProfile()") final ConditionProfile isPositiveProfile,
                        @Cached("createBinaryProfile()") final ConditionProfile isLShiftLongOverflowProfile,
                        @Cached("createBinaryProfile()") final ConditionProfile isArgInLongSizeRangeProfile) {
            if (isPositiveProfile.profile(arg >= 0)) {
                if (isLShiftLongOverflowProfile.profile(Long.numberOfLeadingZeros(receiver) - 1 < arg)) {
                    /*
                     * -1 in check needed, because we do not want to shift a positive long into
                     * negative long (most significant bit indicates positive/negative).
                     */
                    return LargeIntegerObject.shiftLeft(method.image, receiver, (int) arg);
                } else {
                    return receiver << arg;
                }
            } else {
                if (isArgInLongSizeRangeProfile.profile(-Long.SIZE < arg)) {
                    /*
                     * The result of a right shift can only become smaller than the receiver and 0
                     * or -1 at minimum, so no BigInteger needed here.
                     */
                    return receiver >> -arg;
                } else {
                    return receiver >= 0 ? 0L : -1L;
                }
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 18)
    protected abstract static class PrimMakePointNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        protected PrimMakePointNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final PointersObject doObject(final Object xPos, final Object yPos,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            return method.image.asPoint(writeNode, xPos, yPos);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 20)
    protected abstract static class PrimRemLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimRemLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"rhs != 0"})
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.remainder(rhs);
        }

        @Specialization(guards = {"!rhs.isZero()"})
        protected static final Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.remainder(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 21)
    public abstract static class PrimAddLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        public PrimAddLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

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
    public abstract static class PrimSubtractLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        public PrimSubtractLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

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
    protected abstract static class PrimLessThanLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimLessThanLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

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
    protected abstract static class PrimGreaterThanLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimGreaterThanLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

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
    protected abstract static class PrimLessOrEqualLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimLessOrEqualLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

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
    protected abstract static class PrimGreaterOrEqualLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimGreaterOrEqualLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

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
    protected abstract static class PrimEqualLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimEqualLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) == 0);
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
    protected abstract static class PrimNotEqualLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimNotEqualLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return BooleanObject.wrap(lhs.compareTo(rhs) != 0);
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
    public abstract static class PrimMultiplyLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        public PrimMultiplyLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

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
    protected abstract static class PrimDivideLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimDivideLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

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
    protected abstract static class PrimFloorModLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloorModLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doLargeIntegerLong(final LargeIntegerObject lhs, final long rhs) {
            return lhs.floorMod(rhs);
        }

        @Specialization
        protected Object doLargeInteger(final LargeIntegerObject lhs, final LargeIntegerObject rhs) {
            return lhs.floorMod(rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 32)
    protected abstract static class PrimFloorDivideLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloorDivideLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

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
    protected abstract static class PrimQuoLargeIntegersNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimQuoLargeIntegersNode(final CompiledMethodObject method) {
            super(method);
        }

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
    protected abstract static class PrimFloatAtNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimFloatAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doDouble(final double receiver, final long index,
                        @Cached final BranchProfile highProfile,
                        @Cached final BranchProfile lowProfile,
                        @Cached final BranchProfile errorProfile) {
            final long bits = Double.doubleToRawLongBits(receiver);
            if (index == 1) {
                highProfile.enter();
                return Integer.toUnsignedLong((int) (bits >> 32));
            } else if (index == 2) {
                lowProfile.enter();
                return Integer.toUnsignedLong((int) bits);
            } else {
                errorProfile.enter();
                throw PrimitiveFailed.BAD_INDEX;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 39)
    protected abstract static class PrimFloatAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        protected PrimFloatAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doFloatHigh(final FloatObject receiver, final long index, final long value,
                        @Cached final BranchProfile highProfile,
                        @Cached final BranchProfile lowProfile,
                        @Cached final BranchProfile errorProfile) {
            if (index == 1) {
                highProfile.enter();
                receiver.setHigh(value);
            } else if (index == 2) {
                lowProfile.enter();
                receiver.setLow(value);
            } else {
                errorProfile.enter();
                throw PrimitiveFailed.BAD_INDEX;
            }
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
    @SqueakPrimitive(indices = {41, 541})
    public abstract static class PrimAddFloatNode extends AbstractArithmeticFloatPrimitiveNode implements BinaryPrimitive {
        public PrimAddFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doDouble(final double lhs, final double rhs,
                        @Cached("create(method.image)") final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs + rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {42, 542})
    public abstract static class PrimSubtractFloatNode extends AbstractArithmeticFloatPrimitiveNode implements BinaryPrimitive {
        public PrimSubtractFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doDouble(final double lhs, final double rhs,
                        @Cached("create(method.image)") final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs - rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {43, 543})
    protected abstract static class PrimLessThanFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimLessThanFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs < rhs);
        }

        @Specialization(guards = "isAnExactFloat(rhs)")
        protected static final boolean doDoubleLong(final double lhs, final long rhs) {
            return BooleanObject.wrap(lhs < rhs);
        }

        @TruffleBoundary
        @Specialization(guards = "!isAnExactFloat(rhs)")
        protected static final boolean doDoubleLongNotExact(final double lhs, final long rhs) {
            return new BigDecimal(lhs).compareTo(new BigDecimal(rhs)) < 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {44, 544})
    protected abstract static class PrimGreaterThanFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimGreaterThanFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs > rhs);
        }

        @Specialization(guards = "isAnExactFloat(rhs)")
        protected static final boolean doDoubleLong(final double lhs, final long rhs) {
            return BooleanObject.wrap(lhs > rhs);
        }

        @TruffleBoundary
        @Specialization(guards = "!isAnExactFloat(rhs)")
        protected static final boolean doDoubleLongNotExact(final double lhs, final long rhs) {
            return new BigDecimal(lhs).compareTo(new BigDecimal(rhs)) > 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {45, 545})
    protected abstract static class PrimLessOrEqualFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimLessOrEqualFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs <= rhs);
        }

        @Specialization(guards = "isAnExactFloat(rhs)")
        protected static final boolean doDoubleLong(final double lhs, final long rhs) {
            return BooleanObject.wrap(lhs <= rhs);
        }

        @TruffleBoundary
        @Specialization(guards = "!isAnExactFloat(rhs)")
        protected static final boolean doDoubleLongNotExact(final double lhs, final long rhs) {
            return new BigDecimal(lhs).compareTo(new BigDecimal(rhs)) <= 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {46, 546})
    protected abstract static class PrimGreaterOrEqualFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimGreaterOrEqualFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs >= rhs);
        }

        @Specialization(guards = "isAnExactFloat(rhs)")
        protected static final boolean doDoubleLong(final double lhs, final long rhs) {
            return BooleanObject.wrap(lhs >= rhs);
        }

        @TruffleBoundary
        @Specialization(guards = "!isAnExactFloat(rhs)")
        protected static final boolean doDoubleLongNotExact(final double lhs, final long rhs) {
            return new BigDecimal(lhs).compareTo(new BigDecimal(rhs)) >= 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {47, 547})
    protected abstract static class PrimEqualFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimEqualFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs == rhs);
        }

        @Specialization
        protected static final boolean doDoubleLong(final double lhs, final long rhs) {
            return BooleanObject.wrap(isAnExactFloat(rhs) && lhs == rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {48, 548})
    protected abstract static class PrimNotEqualFloatNode extends AbstractArithmeticPrimitiveNode implements BinaryPrimitive {
        protected PrimNotEqualFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doDouble(final double lhs, final double rhs) {
            return BooleanObject.wrap(lhs != rhs);
        }

        @Specialization
        protected static final boolean doDoubleLong(final double lhs, final long rhs) {
            return BooleanObject.wrap(!(isAnExactFloat(rhs) && lhs == rhs));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {49, 549})
    protected abstract static class PrimMultiplyFloatNode extends AbstractArithmeticFloatPrimitiveNode implements BinaryPrimitive {
        protected PrimMultiplyFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doDouble(final double lhs, final double rhs,
                        @Cached("create(method.image)") final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs * rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {50, 550})
    protected abstract static class PrimDivideFloatNode extends AbstractArithmeticFloatPrimitiveNode implements BinaryPrimitive {
        protected PrimDivideFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"!isZero(rhs)"})
        protected static final Object doDouble(final double lhs, final double rhs,
                        @Cached("create(method.image)") final AsFloatObjectIfNessaryNode boxNode) {
            return boxNode.execute(lhs / rhs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 51)
    protected abstract static class PrimFloatTruncatedNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimFloatTruncatedNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "inSafeIntegerRange(receiver.getValue())")
        protected static final long doFloat(final FloatObject receiver,
                        @Cached("createBinaryProfile()") final ConditionProfile positiveProfile) {
            final double value = receiver.getValue();
            return (long) (positiveProfile.profile(value >= 0) ? Math.floor(value) : Math.ceil(value));
        }

        @Specialization(guards = "!inSafeIntegerRange(receiver.getValue())")
        protected final Object doFloatExact(final FloatObject receiver) {
            return LargeIntegerObject.truncateExact(method.image, receiver.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 52)
    protected abstract static class PrimFloatFractionPartNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimFloatFractionPartNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "inSafeIntegerRange(receiver.getValue())")
        protected static final double doFloat(final FloatObject receiver) {
            return receiver.getValue() - (long) receiver.getValue();
        }

        @TruffleBoundary
        @Specialization(guards = "!inSafeIntegerRange(receiver.getValue())")
        protected static final double doFloatExact(final FloatObject receiver) {
            return receiver.getValue() - new BigDecimal(receiver.getValue()).toBigInteger().doubleValue();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 53)
    protected abstract static class PrimFloatExponentNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        private static final long BIAS = 1023;

        protected PrimFloatExponentNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doFloat(final FloatObject receiver,
                        @Cached final BranchProfile zeroProfile,
                        @Cached final BranchProfile nonZeroProfile,
                        @Cached final BranchProfile subnormalFloatProfile) {
            final double value = receiver.getValue();
            if (value == 0) {
                zeroProfile.enter();
                return 0L;
            } else {
                nonZeroProfile.enter();
                final long bits = Double.doubleToRawLongBits(value) >>> 52 & 0x7FF;
                if (bits == 0) { // we have a subnormal float (actual zero was handled above)
                    subnormalFloatProfile.enter();
                    // make it normal by multiplying a large number
                    final double data = value * Math.pow(2, 64);
                    // access its exponent bits, and subtract the large number's exponent and bias
                    return (Double.doubleToRawLongBits(data) >>> 52 & 0x7FF) - 64 - BIAS;
                } else {
                    return bits - BIAS; // apply bias
                }
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 54)
    protected abstract static class PrimFloatTimesTwoPowerNode extends AbstractArithmeticFloatPrimitiveNode implements BinaryPrimitive {
        @Child private AsFloatObjectIfNessaryNode asFloatNode;

        protected PrimFloatTimesTwoPowerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doFloatDouble(final FloatObject matissa, final double exponent,
                        @Cached("createBinaryProfile()") final ConditionProfile matissaZeroProfile,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile,
                        @Cached("createBinaryProfile()") final ConditionProfile exponentZeroProfile) {
            if (matissaZeroProfile.profile(matissa.getValue() == 0)) {
                return 0D;
            } else {
                if (isFiniteProfile.profile(matissa.isFinite())) {
                    if (exponentZeroProfile.profile(exponent == 0)) {
                        return matissa;
                    } else {
                        final double steps = Math.min(3, Math.ceil(Math.abs(exponent) / 1023));
                        double result = matissa.getValue();
                        for (int i = 0; i < steps; i++) {
                            final double pow = Math.pow(2, Math.floor((exponent + i) / steps));
                            assert pow != Double.POSITIVE_INFINITY && pow != Double.NEGATIVE_INFINITY;
                            result *= pow;
                        }
                        return getAsFloatObjectIfNessaryNode().execute(result);
                    }
                } else {
                    return matissa;
                }
            }
        }

        private AsFloatObjectIfNessaryNode getAsFloatObjectIfNessaryNode() {
            if (asFloatNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                asFloatNode = insert(AsFloatObjectIfNessaryNode.create(method.image));
            }
            return asFloatNode;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 55)
    protected abstract static class PrimSquareRootNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimSquareRootNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"receiver.isPositive()"})
        protected static final Object doFloat(final FloatObject receiver,
                        @Cached final BranchProfile isFiniteProfile,
                        @Cached final BranchProfile isNotFiniteProfile) {
            if (receiver.isFinite()) {
                isFiniteProfile.enter();
                return Math.sqrt(receiver.getValue());
            } else {
                isNotFiniteProfile.enter();
                return receiver;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 56)
    protected abstract static class PrimSinNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimSinNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doFloat(final FloatObject receiver,
                        @Cached final BranchProfile notFiniteProfile) {
            if (receiver.isFinite()) {
                return Math.sin(receiver.getValue());
            } else {
                notFiniteProfile.enter();
                return FloatObject.valueOf(method.image, Double.NaN);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 57)
    protected abstract static class PrimArcTanNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimArcTanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doFloat(final FloatObject receiver,
                        @Cached final BranchProfile isNaNProfile) {
            if (receiver.isNaN()) {
                isNaNProfile.enter();
                return receiver;
            } else {
                return Math.atan(receiver.getValue());
            }
        }
    }

    @ImportStatic(Double.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 58)
    protected abstract static class PrimLogNNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimLogNNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doFloat(final FloatObject receiver,
                        @Cached final BranchProfile isNotFiniteProfile) {
            if (receiver.isFinite()) {
                return Math.log(receiver.getValue());
            } else {
                isNotFiniteProfile.enter();
                return receiver;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 59)
    protected abstract static class PrimExpNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimExpNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doFloat(final FloatObject receiver,
                        @Cached final BranchProfile isNotFiniteProfile,
                        @Cached("create(method.image)") final AsFloatObjectIfNessaryNode boxNode) {
            if (receiver.isFinite()) {
                return boxNode.execute(Math.exp(receiver.getValue()));
            } else {
                isNotFiniteProfile.enter();
                return receiver;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 159)
    public abstract static class PrimHashMultiplyNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        public static final int HASH_MULTIPLY_CONSTANT = 1664525;
        public static final int HASH_MULTIPLY_MASK = 0xFFFFFFF;

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

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 551)
    protected abstract static class PrimSmallFloatTruncatedNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimSmallFloatTruncatedNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "inSafeIntegerRange(receiver)")
        protected static final long doDouble(final double receiver,
                        @Cached("createBinaryProfile()") final ConditionProfile positiveProfile) {
            return (long) (positiveProfile.profile(receiver >= 0) ? Math.floor(receiver) : Math.ceil(receiver));
        }

        @Specialization(guards = "!inSafeIntegerRange(receiver)")
        protected final Object doDoubleExact(final double receiver) {
            return LargeIntegerObject.truncateExact(method.image, receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 552)
    protected abstract static class PrimSmallFloatFractionPartNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimSmallFloatFractionPartNode(final CompiledMethodObject method) {
            super(method);
        }

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
    protected abstract static class PrimSmallFloatExponentNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        private static final long BIAS = 1023;

        protected PrimSmallFloatExponentNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doDouble(final double receiver,
                        @Cached final BranchProfile zeroProfile,
                        @Cached final BranchProfile nonZeroProfile,
                        @Cached final BranchProfile subnormalFloatProfile) {
            if (receiver == 0) {
                zeroProfile.enter();
                return 0L;
            } else {
                nonZeroProfile.enter();
                final long bits = Double.doubleToRawLongBits(receiver) >>> 52 & 0x7FF;
                if (bits == 0) { // we have a subnormal float (actual zero was handled above)
                    subnormalFloatProfile.enter();
                    // make it normal by multiplying a large number
                    final double data = receiver * Math.pow(2, 64);
                    // access its exponent bits, and subtract the large number's exponent and bias
                    return (Double.doubleToRawLongBits(data) >>> 52 & 0x7FF) - 64 - BIAS;
                } else {
                    return bits - BIAS; // apply bias
                }
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 554)
    protected abstract static class PrimSmallFloatTimesTwoPowerNode extends AbstractArithmeticFloatPrimitiveNode implements BinaryPrimitive {
        @Child private AsFloatObjectIfNessaryNode asFloatNode;

        protected PrimSmallFloatTimesTwoPowerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doDouble(final double matissa, final double exponent,
                        @Cached("createBinaryProfile()") final ConditionProfile matissaZeroProfile,
                        @Cached("createBinaryProfile()") final ConditionProfile exponentZeroProfile) {
            if (matissaZeroProfile.profile(matissa == 0)) {
                return 0D;
            } else {
                assert Double.isFinite(matissa);
                if (exponentZeroProfile.profile(exponent == 0)) {
                    return matissa;
                } else {
                    final double steps = Math.min(3, Math.ceil(Math.abs(exponent) / 1023));
                    double result = matissa;
                    for (int i = 0; i < steps; i++) {
                        final double pow = Math.pow(2, Math.floor((exponent + i) / steps));
                        assert pow != Double.POSITIVE_INFINITY && pow != Double.NEGATIVE_INFINITY;
                        result *= pow;
                    }
                    return getAsFloatObjectIfNessaryNode().execute(result);
                }
            }
        }

        private AsFloatObjectIfNessaryNode getAsFloatObjectIfNessaryNode() {
            if (asFloatNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                asFloatNode = insert(AsFloatObjectIfNessaryNode.create(method.image));
            }
            return asFloatNode;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 555)
    protected abstract static class PrimSquareRootSmallFloatNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimSquareRootSmallFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isZeroOrGreater(receiver)"})
        protected static final double doDouble(final double receiver) {
            assert Double.isFinite(receiver);
            return Math.sqrt(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 556)
    protected abstract static class PrimSinSmallFloatNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimSinSmallFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double receiver) {
            assert Double.isFinite(receiver);
            return Math.sin(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 557)
    protected abstract static class PrimArcTanSmallFloatNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimArcTanSmallFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double receiver) {
            assert !Double.isNaN(receiver);
            return Math.atan(receiver);
        }
    }

    @ImportStatic(Double.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 558)
    protected abstract static class PrimLogNSmallFloatNode extends AbstractArithmeticPrimitiveNode implements UnaryPrimitive {
        protected PrimLogNSmallFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final double doDouble(final double receiver) {
            assert Double.isFinite(receiver);
            return Math.log(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 559)
    protected abstract static class PrimExpSmallFloatNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimExpSmallFloatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doDouble(final double receiver,
                        @Cached("create(method.image)") final AsFloatObjectIfNessaryNode boxNode) {
            assert Double.isFinite(receiver);
            return boxNode.execute(Math.exp(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 575)
    protected abstract static class PrimHighBitNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimHighBitNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doLong(final long receiver,
                        @Cached("createBinaryProfile()") final ConditionProfile negativeProfile) {
            return Long.SIZE - Long.numberOfLeadingZeros(negativeProfile.profile(receiver < 0) ? -receiver : receiver);
        }
    }

    @TypeSystem
    public static class ArithmeticBaseTypeSystem {
        @ImplicitCast
        public static final double fromFloatObject(final FloatObject object) {
            return object.getValue();
        }
    }

    @ImportStatic(Double.class)
    @TypeSystemReference(ArithmeticBaseTypeSystem.class)
    public abstract static class AbstractArithmeticPrimitiveNode extends AbstractPrimitiveNode {
        private static final long ONE_SHIFTED_BY_53 = 1L << FloatObject.PRECISION;

        public AbstractArithmeticPrimitiveNode(final CompiledMethodObject method) {
            super(method);
        }

        public static final boolean isAnExactFloat(final long value) {
            // We are being a little tricky here:
            // if value is Long.MIN_VALUE, -value == value, so both tests will succeed
            // despite the fact that the absolute value is outside the long range, but that is ok,
            // since Long.MIN_VALUE is a power of two, therefore representable as a double anyway.
            // Similarly, ONE_SHIFTED_BY_53 is representable, being a power of two, regardless if it
            // fits in 53 bits or not, so we can use <= instead of <
            if (-value <= ONE_SHIFTED_BY_53 && value <= ONE_SHIFTED_BY_53) {
                return true;
            }
            final long abs = Math.abs(value);
            final long lowest = Long.lowestOneBit(abs);
            return lowest > Double.MAX_EXPONENT || lowest > 1 && Long.highestOneBit(abs) <= lowest << FloatObject.PRECISION;
        }

        protected static final boolean inSafeIntegerRange(final double d) {
            // The ends of the interval are also included, since they are powers of two
            return -ONE_SHIFTED_BY_53 <= d && d <= ONE_SHIFTED_BY_53;
        }

        protected static final boolean differentSign(final long lhs, final long rhs) {
            return lhs < 0 ^ rhs < 0;
        }
    }

    @TypeSystem
    public static class ArithmeticFloatTypeSystem extends ArithmeticBaseTypeSystem {
        @ImplicitCast
        public static final double fromLong(final long object) {
            return object;
        }
    }

    @TypeSystemReference(ArithmeticFloatTypeSystem.class)
    public abstract static class AbstractArithmeticFloatPrimitiveNode extends AbstractArithmeticPrimitiveNode {
        public AbstractArithmeticFloatPrimitiveNode(final CompiledMethodObject method) {
            super(method);
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArithmeticPrimitivesFactory.getFactories();
    }
}
