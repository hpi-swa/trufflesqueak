package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimDivide extends PrimitiveBinaryOperation {
    public PrimDivide(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    long divide(long a, long b) {
        if (a % b != 0) {
            throw new PrimitiveFailed();
        }
        return a / b;
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    long div(BigInteger a, BigInteger b) {
        if (a.mod(b.abs()).compareTo(BigInteger.ZERO) != 0) {
            throw new PrimitiveFailed();
        }
        return a.divide(b).longValueExact();
    }

    @Specialization
    BigInteger divBig(BigInteger a, BigInteger b) {
        if (a.mod(b.abs()).compareTo(BigInteger.ZERO) != 0) {
            throw new PrimitiveFailed();
        }
        return a.divide(b);
    }
}
