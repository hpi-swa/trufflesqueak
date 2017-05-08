package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimSub extends PrimitiveBinaryOperation {
    public PrimSub(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    long sub(long a, long b) {
        return Math.subtractExact(a, b);
    }

    @Specialization
    BigInteger sub(BigInteger a, BigInteger b) {
        return a.subtract(b);
    }
}
