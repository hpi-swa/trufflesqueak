package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimNotEqual extends PrimitiveBinaryOperation {
    public PrimNotEqual(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    boolean neq(int a, int b) {
        return a != b;
    }

    @Specialization
    boolean neq(long a, long b) {
        return a != b;
    }

    @Specialization
    boolean neq(BigInteger a, BigInteger b) {
        return !a.equals(b);
    }

    @Specialization
    boolean neq(double a, double b) {
        return a != b;
    }
}
