package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimBitShift extends PrimitiveBinaryOperation {
    public PrimBitShift(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization(guards = {"arg >= 0"})
    protected long bitShiftRightPrim(long receiver, int arg) {
        return receiver >> arg;
    }

    @Specialization(guards = {"arg >= 0"}, replaces = {"bitShiftRightPrim"}, rewriteOn = ArithmeticException.class)
    protected long bitShiftRightLong(BigInteger receiver, int arg) {
        return receiver.shiftRight(arg).longValueExact();
    }

    @Specialization(guards = {"arg >= 0"}, replaces = {"bitShiftRightLong"})
    protected BigInteger bitShiftRight(BigInteger receiver, int arg) {
        return receiver.shiftRight(arg);
    }

    @Specialization(guards = {"arg < 0"}, rewriteOn = ArithmeticException.class)
    protected long bitShiftLeftLong(BigInteger receiver, int arg) {
        return receiver.shiftLeft(-arg).longValueExact();
    }

    @Specialization(guards = {"arg < 0"}, replaces = {"bitShiftLeftLong"})
    protected BigInteger bitShiftLeft(BigInteger receiver, int arg) {
        return receiver.shiftLeft(-arg);
    }

    @Specialization(replaces = {"bitShiftRight", "bitShiftLeft"})
    protected BigInteger bitShift(BigInteger receiver, int arg) {
        if (arg >= 0) {
            return receiver.shiftRight(arg);
        } else {
            return receiver.shiftLeft(-arg);
        }
    }
}
