package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public abstract class PrimDigitDivNegative extends PrimitiveBinaryOperation {
    public PrimDigitDivNegative(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    ListObject div(BigInteger rcvr, BigInteger arg) {
        BigInteger[] divRem = rcvr.divideAndRemainder(arg);
        return code.image.wrap(new Object[]{
                        code.image.wrap(divRem[0]),
                        code.image.wrap(divRem[1])});
    }
}
