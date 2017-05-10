package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimMod extends PrimitiveBinaryOperation {
    public PrimMod(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    long mod(long a, long b) {
        return a % b;
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    long mod(BigInteger a, BigInteger b) {
        return a.mod(b).longValueExact();
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    BigInteger modBig(BigInteger a, BigInteger b) {
        return a.mod(b);
    }
}
