package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public abstract class PrimLessOrEqual extends PrimitiveBinaryOperation {
    protected PrimLessOrEqual(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    boolean le(int a, int b) {
        return a <= b;
    }

    @Specialization
    boolean le(long a, long b) {
        return a <= b;
    }

    @Specialization
    boolean le(BigInteger a, BigInteger b) {
        return a.compareTo(b) <= 0;
    }

    @Specialization
    boolean le(double a, double b) {
        return a <= b;
    }
}
