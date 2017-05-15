package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimBitShift extends PrimitiveBinaryOperation {
    @Child PrimNormalize normalizeNode;

    public PrimBitShift(CompiledMethodObject cm) {
        super(cm);
        normalizeNode = new PrimNormalize(cm);
    }

    @Specialization(guards = {"arg <= 0"})
    protected long bitShiftRightPrim(long receiver, int arg) {
        return receiver >> -arg;
    }

    @Specialization(guards = {"arg <= 0"}, rewriteOn = ArithmeticException.class)
    protected long bitShiftRightLong(BigInteger receiver, int arg) {
        return receiver.shiftRight(-arg).longValueExact();
    }

    @Specialization(guards = {"arg <= 0"})
    protected BigInteger bitShiftRight(BigInteger receiver, int arg) {
        return receiver.shiftRight(-arg);
    }

    @Specialization(guards = {"arg > 0"}, rewriteOn = ArithmeticException.class)
    protected long bitShiftLeftLong(BigInteger receiver, int arg) {
        return receiver.shiftLeft(arg).longValueExact();
    }

    @Specialization(guards = {"arg > 0"})
    protected BigInteger bitShiftLeft(BigInteger receiver, int arg) {
        return receiver.shiftLeft(arg);
    }

    @Specialization(replaces = {"bitShiftRight", "bitShiftLeft"})
    protected BigInteger bitShift(BigInteger receiver, int arg) {
        if (arg < 0) {
            return receiver.shiftRight(-arg);
        } else {
            return receiver.shiftLeft(arg);
        }
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
