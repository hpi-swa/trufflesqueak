package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimMul extends PrimitiveBinaryOperation {
    public PrimMul(CompiledMethodObject code) {
        super(code);
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
