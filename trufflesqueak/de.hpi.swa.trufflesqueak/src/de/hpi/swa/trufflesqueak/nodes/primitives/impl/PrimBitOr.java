package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimBitOr extends PrimitiveBinaryOperation {
    public PrimBitOr(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    protected long bitOr(long receiver, long arg) {
        return receiver | arg;
    }

    @Specialization
    protected BigInteger bitAnd(BigInteger receiver, BigInteger arg) {
        return receiver.or(arg);
    }
}
