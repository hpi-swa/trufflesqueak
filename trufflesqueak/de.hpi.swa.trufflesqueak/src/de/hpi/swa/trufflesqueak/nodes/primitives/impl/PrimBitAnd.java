package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimBitAnd extends PrimitiveBinaryOperation {
    public PrimBitAnd(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    protected int bitAnd(int receiver, int arg) {
        return receiver & arg;
    }

    @Specialization
    protected long bitAnd(long receiver, long arg) {
        return receiver & arg;
    }

    @Specialization
    protected BigInteger bitAnd(BigInteger receiver, BigInteger arg) {
        return receiver.and(arg);
    }
}
