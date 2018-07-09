package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodes.ReturnReceiverNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ArithmeticPrimitives.AbstractArithmeticBinaryPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ArithmeticPrimitives.AbstractArithmeticPrimitiveNode;

public final class LargeIntegers extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return LargeIntegersFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primAnyBitFromTo") // TODO: implement primitive
    public abstract static class PrimAnyBitFromToNode extends AbstractArithmeticBinaryPrimitiveNode {

        public PrimAnyBitFromToNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {1, 21, 41}, name = "primDigitAdd")
    public abstract static class PrimAddNode extends AbstractArithmeticBinaryPrimitiveNode {

        public PrimAddNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Override
        @Specialization(rewriteOn = ArithmeticException.class)
        protected final Object doLong(final long a, final long b) {
            return Math.addExact(a, b);
        }

        @Specialization
        protected final Object doLongWithOverflow(final long a, final long argument) {
            return doLargeInteger(asLargeInteger(a), asLargeInteger(argument));
        }

        @Override
        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.add(b);
        }

        @Override
        @Specialization
        protected final Object doDouble(final double a, final double b) {
            return a + b;
        }

        @Override
        @Specialization
        protected final Object doFloat(final FloatObject a, final FloatObject b) {
            return asFloatObject(a.getValue() + b.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {2, 22, 42}, name = "primDigitSubtract")
    public abstract static class PrimSubstractNode extends AbstractArithmeticBinaryPrimitiveNode {
        public PrimSubstractNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
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

        @Override
        @Specialization
        protected final Object doDouble(final double a, final double b) {
            return a - b;
        }

        @Override
        @Specialization
        protected final Object doFloat(final FloatObject a, final FloatObject b) {
            return asFloatObject(a.getValue() - b.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {9, 29, 49}, name = "primDigitMultiplyNegative")
    public abstract static class PrimMultiplyNode extends AbstractArithmeticBinaryPrimitiveNode {
        public PrimMultiplyNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
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

        @Override
        @Specialization
        protected final Object doDouble(final double a, final double b) {
            return a * b;
        }

        @Override
        @Specialization
        protected final Object doFloat(final FloatObject a, final FloatObject b) {
            return asFloatObject(a.getValue() * b.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {14, 34}, name = "primDigitBitAnd")
    public abstract static class PrimBitAndNode extends AbstractArithmeticPrimitiveNode {
        public PrimBitAndNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final long doLong(final long receiver, final long arg) {
            return receiver & arg;
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final LargeIntegerObject arg) {
            return receiver.and(arg);
        }

        @Specialization
        protected final Object doLong(final long receiver, final LargeIntegerObject arg) {
            return doLargeInteger(asLargeInteger(receiver), arg);
        }

        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject receiver, final long arg) {
            return doLargeInteger(receiver, asLargeInteger(arg));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {15, 35}, name = "primDigitBitOr")
    public abstract static class PrimBitOrNode extends AbstractArithmeticPrimitiveNode {
        public PrimBitOrNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final long bitOr(final long receiver, final long arg) {
            return receiver | arg;
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final LargeIntegerObject arg) {
            return receiver.or(arg);
        }

        @Specialization
        protected final Object doLong(final long receiver, final LargeIntegerObject arg) {
            return doLargeInteger(asLargeInteger(receiver), arg);
        }

        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject receiver, final long arg) {
            return doLargeInteger(receiver, asLargeInteger(arg));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {17, 37}, name = "primDigitBitShiftMagnitude")
    public abstract static class PrimBitShiftNode extends AbstractArithmeticPrimitiveNode {
        private final ValueProfile storageType = ValueProfile.createClassProfile();

        public PrimBitShiftNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"arg >= 0", "!isLShiftLongOverflow(receiver, arg)"})
        protected static final long doLong(final long receiver, final long arg) {
            return receiver << arg;
        }

        @Specialization(guards = {"arg >= 0", "isLShiftLongOverflow(receiver, arg)"})
        protected final Object doLongLargeInteger(final long receiver, final long arg) {
            return doLargeInteger(asLargeInteger(receiver), arg);
        }

        @Specialization(guards = {"arg < 0", "isArgInLongSizeRange(arg)"})
        protected static final long doLongNegativeLong(final long receiver, final long arg) {
            // The result of a right shift can only become smaller than the receiver and 0 or -1 at
            // minimum, so no BigInteger needed here
            return receiver >> -arg;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"arg < 0", "!isArgInLongSizeRange(arg)"})
        protected static final long doLongNegative(final long receiver, final long arg) {
            return receiver >= 0 ? 0L : -1L;
        }

        @Specialization(guards = {"arg >= 0"})
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final long arg) {
            return receiver.shiftLeft((int) arg);
        }

        @Specialization(guards = {"arg < 0"})
        protected static final Object doLargeIntegerNegative(final LargeIntegerObject receiver, final long arg) {
            return receiver.shiftRight((int) -arg);
        }

        @Specialization(guards = {"arg >= 0", "receiver.isByteType()"})
        protected final Object doNativeObject(final NativeObject receiver, final long arg) {
            return doLargeInteger(receiver.normalize(storageType), arg);
        }

        @Specialization(guards = {"arg < 0", "receiver.isByteType()"})
        protected final Object doNativeObjectNegative(final NativeObject receiver, final long arg) {
            return doLargeIntegerNegative(receiver.normalize(storageType), arg);
        }

        protected static final boolean isLShiftLongOverflow(final long receiver, final long arg) {
            // -1 needed, because we do not want to shift a positive long into negative long (most
            // significant bit indicates positive/negative)
            return Long.numberOfLeadingZeros(receiver) - 1 < arg;
        }

        protected static final boolean isArgInLongSizeRange(final long value) {
            return -Long.SIZE < value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {16, 36}, name = "primDigitBitXor")
    protected abstract static class PrimBitXorNode extends AbstractArithmeticPrimitiveNode {
        protected PrimBitXorNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final long doLong(final long receiver, final long b) {
            return receiver ^ b;
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final LargeIntegerObject arg) {
            return receiver.xor(arg);
        }

        @Specialization
        protected final Object doLong(final long receiver, final LargeIntegerObject arg) {
            return doLargeInteger(asLargeInteger(receiver), arg);
        }

        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject receiver, final long arg) {
            return doLargeInteger(receiver, asLargeInteger(arg));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primDigitCompare")
    public abstract static class PrimDigitCompareNode extends AbstractArithmeticPrimitiveNode {

        public PrimDigitCompareNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "a == b")
        protected long doLongEqual(final long a, final long b) {
            return 0L;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "a > b")
        protected long doLongLarger(final long a, final long b) {
            return 1L;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "a < b")
        protected long doLongSmaller(final long a, final long b) {
            return -1L;
        }

        @Specialization
        protected long doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.compareTo(b);
        }

        @Specialization
        protected long doLong(final long a, final LargeIntegerObject b) {
            final long longValueExact;
            try {
                longValueExact = b.longValueExact();
            } catch (ArithmeticException e) {
                return -1L; // If `b` does not fit into a long, it must be larger
            }
            if (a == longValueExact) {
                return 0L;
            } else if (a > longValueExact) {
                return 1L;
            } else {
                return -1L;
            }
        }

        @Specialization
        protected long doLargeInteger(final LargeIntegerObject a, final long b) {
            final long longValueExact;
            try {
                longValueExact = a.longValueExact();
            } catch (ArithmeticException e) {
                return 1L; // If `a` does not fit into a long, it must be larger
            }
            if (longValueExact == b) {
                return 0;
            } else if (longValueExact > b) {
                return 1L;
            } else {
                return -1L;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primDigitDivNegative")
    public abstract static class PrimDigitDivNegativeNode extends AbstractArithmeticPrimitiveNode {
        public PrimDigitDivNegativeNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected final PointersObject doLong(final long rcvr, final long arg, final boolean negative) {
            long divide = rcvr / arg;
            if ((negative && divide >= 0) || (!negative && divide < 0)) {
                divide = Math.negateExact(divide);
            }
            final long remainder = rcvr % arg;
            return code.image.newListWith(divide, remainder);
        }

        @Specialization
        protected final PointersObject doLongWithOverflow(final long rcvr, final long arg, final boolean negative) {
            return doLargeInteger(asLargeInteger(rcvr), asLargeInteger(arg), negative);
        }

        @Specialization
        protected final PointersObject doLargeInteger(final LargeIntegerObject rcvr, final LargeIntegerObject arg, final boolean negative) {
            LargeIntegerObject divide = rcvr.divideNoReduce(arg);
            if (negative != divide.isNegative()) {
                divide = divide.negateNoReduce();
            }
            final Object remainder = rcvr.remainder(arg);
            return code.image.newListWith(divide.reduceIfPossible(), remainder);
        }

        @Specialization
        protected final PointersObject doLong(final long rcvr, final LargeIntegerObject arg, final boolean negative) {
            return doLargeInteger(asLargeInteger(rcvr), arg, negative);
        }

        @Specialization
        protected final PointersObject doLargeInteger(final LargeIntegerObject rcvr, final long arg, final boolean negative) {
            return doLargeInteger(rcvr, asLargeInteger(arg), negative);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primMontgomeryDigitLength")
    public abstract static class PrimMontgomeryDigitLengthNode extends AbstractArithmeticPrimitiveNode {

        public PrimMontgomeryDigitLengthNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected static final long doDigitLength(final Object receiver) {
            return 8L; // TODO: update when primMontgomeryTimesModulo is implemented
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primMontgomeryTimesModulo")
    public abstract static class PrimMontgomeryTimesModuloNode extends AbstractArithmeticPrimitiveNode {

        public PrimMontgomeryTimesModuloNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected static final Object doFail(final Object receiver, final Object a, final Object m, final Object mInv) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = {"primNormalizePositive", "primNormalizeNegative"})
    public abstract static class PrimNormalizeNode extends AbstractArithmeticPrimitiveNode {
        private final ValueProfile storageType = ValueProfile.createClassProfile();
        @Child private ReturnReceiverNode receiverNode;

        public PrimNormalizeNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            receiverNode = ReturnReceiverNode.create(method, -1);
        }

        @Specialization
        protected static final long doLong(final long value) {
            return value;
        }

        @Specialization
        public static final Object doLargeInteger(final LargeIntegerObject value) {
            return value.reduceIfPossible();
        }

        @Specialization(guards = "receiver.isByteType()")
        protected final Object doNativeObject(final NativeObject receiver) {
            return receiver.normalize(storageType);
        }
    }
}
