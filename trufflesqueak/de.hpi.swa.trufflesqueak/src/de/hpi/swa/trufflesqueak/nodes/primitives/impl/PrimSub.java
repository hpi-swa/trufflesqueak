package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public abstract class PrimSub extends PrimitiveBinaryOperation {
    public PrimSub(CompiledMethodObject code) {
        super(code);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    int sub(int a, int b) {
        return Math.subtractExact(a, b);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    int subInt(long a, long b) {
        return Math.toIntExact(Math.subtractExact(a, b));
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    long sub(long a, long b) {
        return Math.subtractExact(a, b);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    int subInt(BigInteger a, BigInteger b) {
        return a.subtract(b).intValueExact();
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    long sub(BigInteger a, BigInteger b) {
        return a.subtract(b).longValueExact();
    }

    @Specialization
    BigInteger subBig(BigInteger a, BigInteger b) {
        return a.subtract(b);
    }

    @Specialization
    double sub(double a, double b) {
        return a - b;
    }
}
