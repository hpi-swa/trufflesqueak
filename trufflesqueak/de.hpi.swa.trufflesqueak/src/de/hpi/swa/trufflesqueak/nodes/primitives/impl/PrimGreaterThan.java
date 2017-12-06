package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public abstract class PrimGreaterThan extends PrimitiveBinaryOperation {
    public PrimGreaterThan(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    boolean gt(int a, int b) {
        return a > b;
    }

    @Specialization
    boolean gt(long a, long b) {
        return a > b;
    }

    @Specialization
    boolean gt(BigInteger a, BigInteger b) {
        return a.compareTo(b) > 0;
    }

    @Specialization
    boolean gt(double a, double b) {
        return a > b;
    }
}
