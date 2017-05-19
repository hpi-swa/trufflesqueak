package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimNotEqual extends PrimitiveBinaryOperation {
    public PrimNotEqual(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    protected boolean neq(int a, int b) {
        return a != b;
    }

    @Specialization
    protected boolean neq(long a, long b) {
        return a != b;
    }

    @Specialization
    protected boolean neq(BigInteger a, BigInteger b) {
        return !a.equals(b);
    }
}
