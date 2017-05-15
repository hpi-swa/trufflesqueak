package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimDigitDivNegative extends PrimitiveBinaryOperation {
    public PrimDigitDivNegative(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    ListObject div(BigInteger rcvr, BigInteger arg) {
        BigInteger[] divRem = rcvr.divideAndRemainder(arg);
        return method.image.wrap(new BaseSqueakObject[]{
                        method.image.wrap(divRem[0]),
                        method.image.wrap(divRem[1])});
    }
}
