package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimLessThan extends PrimitiveBinaryOperation {
    public PrimLessThan(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    boolean lt(int a, int b) {
        return a < b;
    }

    @Specialization
    boolean lt(long a, long b) {
        return a < b;
    }

    @Specialization
    boolean lt(BigInteger a, BigInteger b) {
        return a.compareTo(b) < 0;
    }

    @Specialization
    boolean lt(double a, double b) {
        return a < b;
    }
}
