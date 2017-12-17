package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeBinary;

public abstract class PrimEqual extends PrimitiveNodeBinary {
    public PrimEqual(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    protected boolean eq(int receiver, int argument) {
        return receiver == argument;
    }

    @Specialization
    protected boolean eq(long receiver, long argument) {
        return receiver == argument;
    }

    @Specialization
    boolean eq(BigInteger a, BigInteger b) {
        return a.equals(b);
    }

    @Specialization
    boolean eq(double a, double b) {
        return a == b;
    }
}
