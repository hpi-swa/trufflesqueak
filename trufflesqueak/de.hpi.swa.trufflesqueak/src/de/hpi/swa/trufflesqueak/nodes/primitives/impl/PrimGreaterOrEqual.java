package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimGreaterOrEqual extends PrimitiveBinaryOperation {
    public PrimGreaterOrEqual(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    boolean ge(int a, int b) {
        return a >= b;
    }

    @Specialization
    boolean ge(long a, long b) {
        return a >= b;
    }

    @Specialization
    boolean ge(BigInteger a, BigInteger b) {
        return a.compareTo(b) >= 0;
    }

    @Specialization
    boolean ge(double a, double b) {
        return a >= b;
    }
}
