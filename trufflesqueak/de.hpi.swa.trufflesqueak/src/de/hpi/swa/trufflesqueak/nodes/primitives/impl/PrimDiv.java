package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimDiv extends PrimitiveBinaryOperation {
    public PrimDiv(CompiledMethodObject code) {
        super(code);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    int div(int a, int b) {
        if (a == Integer.MIN_VALUE && b == -1) {
            throw new ArithmeticException();
        }
        return Math.floorDiv(a, b);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    int divInt(long a, long b) {
        if (a == Long.MIN_VALUE && b == -1) {
            throw new ArithmeticException();
        }
        return Math.toIntExact(Math.floorDiv(a, b));
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    long div(long a, long b) {
        if (a == Long.MIN_VALUE && b == -1) {
            throw new ArithmeticException();
        }
        return Math.floorDiv(a, b);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    int divInt(BigInteger a, BigInteger b) {
        return a.divide(b).intValueExact();
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
