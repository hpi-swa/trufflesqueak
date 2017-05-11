package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimQuo extends PrimitiveBinaryOperation {
    public PrimQuo(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    long div(long a, long b) {
        return a / b;
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    long div(BigInteger a, BigInteger b) {
        return a.divide(b).longValueExact();
    }

    @Specialization
    BigInteger divBig(BigInteger a, BigInteger b) {
        return a.divide(b);
    }
}
