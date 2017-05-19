package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimQuo extends PrimitiveBinaryOperation {
    public PrimQuo(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    int quo(int a, int b) {
        return a / b;
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    int quoInt(long a, long b) {
        return Math.toIntExact(a / b);
    }

    @Specialization
    long quo(long a, long b) {
        return a / b;
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    int quoInt(BigInteger a, BigInteger b) {
        return a.divide(b).intValueExact();
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    long quo(BigInteger a, BigInteger b) {
        return a.divide(b).longValueExact();
    }

    @Specialization
    BigInteger quoBig(BigInteger a, BigInteger b) {
        return a.divide(b);
    }
}
