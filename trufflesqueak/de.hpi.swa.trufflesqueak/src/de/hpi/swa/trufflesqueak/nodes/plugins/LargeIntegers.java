package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnBytecodes.ReturnReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.AbstractArithmeticPrimitiveNode;

public final class LargeIntegers extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return LargeIntegersFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {1, 21, 41}, name = "primDigitAdd", numArguments = 2)
    public static abstract class PrimAddNode extends AbstractArithmeticPrimitiveNode {

        public PrimAddNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected final static long doLong(final long a, final long b) {
            return Math.addExact(a, b);
        }

        @Specialization
        protected final Object doLongWithOverflow(final long a, final long argument) {
            return doLargeInteger(asLargeInteger(a), asLargeInteger(argument));
        }

        @Specialization
        protected final static Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.add(b);
        }

        @Specialization
        protected final static double doDouble(final double a, final double b) {
            return a + b;
        }

        @Specialization
        protected final Object doLong(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @Specialization
        protected final static double doLong(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final static double doDouble(final double a, final long b) {
            return doDouble(a, (double) b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {2, 22, 42}, name = "primDigitSubtract", numArguments = 2)
    public static abstract class PrimSubstractNode extends AbstractArithmeticPrimitiveNode {
        public PrimSubstractNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected final static long doLong(final long a, final long b) {
            return Math.subtractExact(a, b);
        }

        @Specialization
        protected final Object doLongWithOverflow(final long a, final long b) {
            return doLargeInteger(asLargeInteger(a), asLargeInteger(b));
        }

        @Specialization
        protected final static Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.subtract(b);
        }

        @Specialization
        protected final static double doDouble(final double a, final double b) {
            return a - b;
        }

        @Specialization
        protected final Object doLong(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization
        protected final static double doLong(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @Specialization
        protected final static double doDouble(final double a, final long b) {
            return doDouble(a, (double) b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {9, 29, 49}, name = "primDigitMultiplyNegative", numArguments = 2)
    public static abstract class PrimMultiplyNode extends AbstractArithmeticPrimitiveNode {
        public PrimMultiplyNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected final static long doLong(final long a, final long b) {
            return Math.multiplyExact(a, b);
        }

        @Specialization
        protected final Object doLongWithOverflow(final long a, final long b) {
            return doLargeInteger(asLargeInteger(a), asLargeInteger(b));
        }

        @Specialization
        protected final static Object doLargeInteger(final LargeIntegerObject a, final LargeIntegerObject b) {
            return a.multiply(b);
        }

        @Specialization
        protected final static double doDouble(final double a, final double b) {
            return a * b;
        }

        @Specialization
        protected final Object doLong(final long a, final LargeIntegerObject b) {
            return doLargeInteger(asLargeInteger(a), b);
        }

        @Specialization
        protected final static double doLong(final long a, final double b) {
            return doDouble(a, b);
        }

        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject a, final long b) {
            return doLargeInteger(a, asLargeInteger(b));
        }

        @Specialization
        protected final static double doDouble(final double a, final long b) {
            return doDouble(a, (double) b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primDigitDivNegative", numArguments = 3)
    public static abstract class PrimDigitDivNegativeNode extends AbstractArithmeticPrimitiveNode {
        public PrimDigitDivNegativeNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected final ListObject doLong(final long rcvr, final long arg, final boolean negative) {
            long divide = rcvr / arg;
            if ((negative && divide >= 0) || (!negative && divide < 0)) {
                divide = Math.negateExact(divide);
            }
            long remainder = rcvr % arg;
            return code.image.newList(new Object[]{divide, remainder});
        }

        @Specialization
        protected final ListObject doLongWithOverflow(final long rcvr, final long arg, final boolean negative) {
            return doLargeInteger(asLargeInteger(rcvr), asLargeInteger(arg), negative);
        }

        @Specialization
        protected final ListObject doLargeInteger(final LargeIntegerObject rcvr, final LargeIntegerObject arg, final boolean negative) {
            LargeIntegerObject divide = rcvr.divideNoReduce(arg);
            if ((negative && divide.signum() >= 0) || (!negative && divide.signum() < 0)) {
                divide = divide.negateNoReduce();
            }
            Object remainder = rcvr.remainder(arg);
            return code.image.newList(new Object[]{divide.reduceIfPossible(), remainder});
        }

        @Specialization
        protected final ListObject doLong(final long rcvr, final LargeIntegerObject arg, final boolean negative) {
            return doLargeInteger(asLargeInteger(rcvr), arg, negative);
        }

        @Specialization
        protected final ListObject doLargeInteger(final LargeIntegerObject rcvr, final long arg, final boolean negative) {
            return doLargeInteger(rcvr, asLargeInteger(arg), negative);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {14, 34}, name = "primDigitBitAnd", numArguments = 2)
    public static abstract class PrimBitAndNode extends AbstractArithmeticPrimitiveNode {
        public PrimBitAndNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static long doLong(final long receiver, final long arg) {
            return receiver & arg;
        }

        @Specialization
        protected final static Object doLargeInteger(final LargeIntegerObject receiver, final LargeIntegerObject arg) {
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
    @SqueakPrimitive(indices = {15, 35}, name = "primDigitBitOr", numArguments = 2)
    public static abstract class PrimBitOrNode extends AbstractArithmeticPrimitiveNode {
        public PrimBitOrNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static long bitOr(final long receiver, final long arg) {
            return receiver | arg;
        }

        @Specialization
        protected final static Object doLargeInteger(final LargeIntegerObject receiver, final LargeIntegerObject arg) {
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
    @SqueakPrimitive(indices = {17, 37}, name = "primDigitBitShiftMagnitude", numArguments = 2)
    public static abstract class PrimBitShiftNode extends AbstractArithmeticPrimitiveNode {

        public PrimBitShiftNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"arg >= 0"})
        protected final static long doLong(final long receiver, final long arg) {
            return receiver << arg;
        }

        @Specialization(guards = {"arg < 0"})
        protected final static long doLongNegative(final long receiver, final long arg) {
            return receiver >> -arg;
        }

        @Specialization(guards = {"arg >= 0"})
        protected final static Object doLargeInteger(final LargeIntegerObject receiver, final long arg) {
            return receiver.shiftLeft((int) arg);
        }

        @Specialization(guards = {"arg < 0"})
        protected final static Object doLargeIntegerNegative(final LargeIntegerObject receiver, final long arg) {
            return receiver.shiftRight((int) -arg);
        }

        @Specialization(guards = {"arg >= 0"})
        protected final static Object doNativeObject(final NativeObject receiver, final long arg) {
            return doLargeInteger(receiver.normalize(), arg);
        }

        @Specialization(guards = {"arg < 0"})
        protected final static Object doNativeObjectNegative(final NativeObject receiver, final long arg) {
            return doLargeIntegerNegative(receiver.normalize(), arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = {"primNormalizePositive", "primNormalizeNegative"})
    public static abstract class PrimNormalizeNode extends AbstractArithmeticPrimitiveNode {
        @Child private ReturnReceiverNode receiverNode;

        public PrimNormalizeNode(CompiledMethodObject method) {
            super(method);
            receiverNode = ReturnReceiverNode.create(method, -1);
        }

        @Specialization
        protected long doLong(long value) {
            return value;
        }

        @Specialization
        public Object doLargeInteger(LargeIntegerObject value) {
            return value.reduceIfPossible();
        }

        @Specialization
        protected Object doNativeObject(NativeObject value) {
            return value.normalize().reduceIfPossible();
        }
    }

}
