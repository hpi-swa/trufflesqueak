package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimAdd extends PrimitiveBinaryOperation {
    public PrimAdd(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    long add(long a, long b) {
        return Math.addExact(a, b);
    }

    @Specialization
    BigInteger add(BigInteger a, BigInteger b) {
        return a.add(b);
    }
}
