package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimBitShift extends PrimitiveBinaryOperation {
    public PrimBitShift(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    protected int bitShift(int receiver, int arg) {
        if (arg >= 0) {
            return receiver >> arg;
        } else {
            return BigInteger.valueOf(receiver).shiftLeft(-arg).intValueExact();
        }
    }

    protected BigInteger bitShiftLeft(int receiver, int arg) {
        assert arg < 0;
        return BigInteger.valueOf(receiver).shiftLeft(-arg);
    }
}
