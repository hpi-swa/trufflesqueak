package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimMul extends PrimitiveBinaryOperation {
    public PrimMul(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    long mod(long a, long b) {
        return Math.multiplyExact(a, b);
    }

    @Specialization
    BigInteger mod(BigInteger a, BigInteger b) {
        return a.multiply(b);
    }
}
