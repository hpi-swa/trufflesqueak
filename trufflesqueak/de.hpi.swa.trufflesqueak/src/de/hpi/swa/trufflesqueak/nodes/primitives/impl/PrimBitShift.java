package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public abstract class PrimBitShift extends PrimitiveBinaryOperation {
    @Child PrimNormalize normalizeNode;

    public PrimBitShift(CompiledMethodObject code) {
        super(code);
        // TODO(fniephaus): fix
        // normalizeNode = new PrimNormalize(code);
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
