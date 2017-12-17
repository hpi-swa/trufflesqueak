package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeBinary;

public abstract class PrimEquivalent extends PrimitiveNodeBinary {
    public PrimEquivalent(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    boolean equivalent(char a, char b) {
        return a == b;
    }

    @Specialization
    boolean equivalent(int a, int b) {
        return a == b;
    }

    @Specialization
    boolean equivalent(long a, long b) {
        return a == b;
    }

    @Specialization
    boolean equivalent(boolean a, boolean b) {
        return a == b;
    }

    @Specialization
    boolean equivalent(BigInteger a, BigInteger b) {
        return a.equals(b);
    }

    @Specialization
    boolean equivalent(Object a, Object b) {
        return a == b;
    }
}
