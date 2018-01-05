package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.math.BigInteger;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnBytecodes.ReturnReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.LargeIntegersFactory.PrimNormalizeNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public final class LargeIntegers extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return LargeIntegersFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {1, 21, 41}, name = "primDigitAdd", numArguments = 2)
    public static abstract class PrimAddNode extends AbstractPrimitiveNode {

        public PrimAddNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        int add(int a, int b) {
            return Math.addExact(a, b);
        }

        @Specialization
        long addOverflow(int a, int b) {
            return (long) a + (long) b;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        long add(long a, long b) {
            return Math.addExact(a, b);
        }

        @Specialization
        @TruffleBoundary
        BigInteger add(BigInteger a, BigInteger b) {
            return a.add(b);
        }

        @Specialization
        double add(double a, double b) {
            return a + b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {2, 22, 42}, name = "primDigitSubtract", numArguments = 2)
    public static abstract class PrimSubNode extends AbstractPrimitiveNode {
        public PrimSubNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        int sub(int a, int b) {
            return Math.subtractExact(a, b);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        int subInt(long a, long b) {
            return Math.toIntExact(Math.subtractExact(a, b));
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        long sub(long a, long b) {
            return Math.subtractExact(a, b);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        int subInt(BigInteger a, BigInteger b) {
            return a.subtract(b).intValueExact();
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        long sub(BigInteger a, BigInteger b) {
            return a.subtract(b).longValueExact();
        }

        @Specialization
        BigInteger subBig(BigInteger a, BigInteger b) {
            return a.subtract(b);
        }

        @Specialization
        double sub(double a, double b) {
            return a - b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {9, 29, 49}, name = "primDigitMultiplyNegative", numArguments = 2)
    public static abstract class PrimMultiplyNode extends AbstractPrimitiveNode {
        public PrimMultiplyNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        int mul(int a, int b) {
            return Math.multiplyExact(a, b);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        long mul(long a, long b) {
            return Math.multiplyExact(a, b);
        }

        @Specialization
        BigInteger mul(BigInteger a, BigInteger b) {
            return a.multiply(b);
        }

        @Specialization
        double mul(double a, double b) {
            return a * b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primDigitDivNegative", numArguments = 2)
    public static abstract class PrimDigitDivNegativeNode extends AbstractPrimitiveNode {
        public PrimDigitDivNegativeNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        ListObject div(BigInteger rcvr, BigInteger arg) {
            BigInteger[] divRem = rcvr.divideAndRemainder(arg);
            return code.image.wrap(new Object[]{
                            code.image.wrap(divRem[0]),
                            code.image.wrap(divRem[1])});
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {14, 34}, name = "primDigitBitAnd", numArguments = 2)
    public static abstract class PrimBitAndNode extends AbstractPrimitiveNode {
        public PrimBitAndNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected int bitAnd(int receiver, int arg) {
            return receiver & arg;
        }

        @Specialization
        protected long bitAnd(long receiver, long arg) {
            return receiver & arg;
        }

        @Specialization
        protected BigInteger bitAnd(BigInteger receiver, BigInteger arg) {
            return receiver.and(arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {15, 35}, name = "primDigitBitOr", numArguments = 2)
    public static abstract class PrimBitOrNode extends AbstractPrimitiveNode {
        public PrimBitOrNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected int bitOr(int receiver, int arg) {
            return receiver | arg;
        }

        @Specialization
        protected long bitOr(long receiver, long arg) {
            return receiver | arg;
        }

        @Specialization
        protected BigInteger bitAnd(BigInteger receiver, BigInteger arg) {
            return receiver.or(arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {17, 37}, name = "primDigitBitShiftMagnitude", numArguments = 2)
    public static abstract class PrimBitShiftNode extends AbstractPrimitiveNode {
        @Child private PrimNormalizeNode normalizeNode;

        public PrimBitShiftNode(CompiledMethodObject method) {
            super(method);
            normalizeNode = PrimNormalizeNodeFactory.create(method, new SqueakNode[]{new FrameReceiverNode()});
        }

        @Specialization(guards = {"arg <= 0"})
        protected int bitShiftRightInt(int receiver, int arg) {
            return receiver >> -arg;
        }

        @Specialization(guards = {"arg <= 0"}, rewriteOn = ArithmeticException.class)
        protected int bitShiftRightInt(long receiver, int arg) {
            return Math.toIntExact(receiver >> -arg);
        }

        @Specialization(guards = {"arg <= 0"})
        protected long bitShiftRightLong(long receiver, int arg) {
            return receiver >> -arg;
        }

        @Specialization(guards = {"arg <= 0"}, rewriteOn = ArithmeticException.class)
        protected int bitShiftRightInt(BigInteger receiver, int arg) {
            return receiver.shiftRight(-arg).intValueExact();
        }

        @Specialization(guards = {"arg <= 0"}, rewriteOn = ArithmeticException.class)
        protected long bitShiftRightLong(BigInteger receiver, int arg) {
            return receiver.shiftRight(-arg).longValueExact();
        }

        @Specialization(guards = {"arg <= 0"})
        protected BigInteger bitShiftRightBig(BigInteger receiver, int arg) {
            return receiver.shiftRight(-arg);
        }

        @Specialization(guards = {"arg > 0"}, rewriteOn = ArithmeticException.class)
        protected int bitShiftLeftInt(BigInteger receiver, int arg) {
            return receiver.shiftLeft(arg).intValueExact();
        }

        @Specialization(guards = {"arg > 0"}, rewriteOn = ArithmeticException.class)
        protected long bitShiftLeftLong(BigInteger receiver, int arg) {
            return receiver.shiftLeft(arg).longValueExact();
        }

        @Specialization(guards = {"arg > 0"})
        protected BigInteger bitShiftLeft(BigInteger receiver, int arg) {
            return receiver.shiftLeft(arg);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected long bitShiftNativeLong(NativeObject receiver, int arg) {
            return shiftNative(receiver, arg).longValueExact();
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected BigInteger bitShiftNativeBig(NativeObject receiver, int arg) {
            return shiftNative(receiver, arg);
        }

        private BigInteger shiftNative(NativeObject receiver, int arg) {
            BigInteger integer = normalizeNode.normalizeBig(receiver);
            if (arg < 0) {
                return integer.shiftRight(-arg);
            } else {
                return integer.shiftLeft(arg);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = {"primNormalizePositive", "primNormalizeNegative"})
    public static abstract class PrimNormalizeNode extends AbstractPrimitiveNode {
        @Child private ReturnReceiverNode receiverNode;

        public PrimNormalizeNode(CompiledMethodObject method) {
            super(method);
            receiverNode = new ReturnReceiverNode(method, -1);
        }

        @Specialization
        int normalizeInt(int o) {
            return o;
        }

        @Specialization
        long normalizeLong(long o) {
            return o;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        int normalizeInt(BigInteger o) {
            return o.intValueExact();
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        long normalizeLong(BigInteger o) {
            return o.longValueExact();
        }

        @Specialization
        BigInteger normalizeBig(BigInteger o) {
            return o;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        int normalizeInt(NativeObject o) {
            return bigIntFromNative(o).intValueExact();
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        long normalizeLong(NativeObject o) {
            return bigIntFromNative(o).longValueExact();
        }

        @Specialization
        public BigInteger normalizeBig(NativeObject o) {
            return bigIntFromNative(o);
        }

        private BigInteger bigIntFromNative(NativeObject o) {
            return new LargeIntegerObject(code.image, o.getSqClass(), o.getBytes()).getValue();
        }
    }

}
