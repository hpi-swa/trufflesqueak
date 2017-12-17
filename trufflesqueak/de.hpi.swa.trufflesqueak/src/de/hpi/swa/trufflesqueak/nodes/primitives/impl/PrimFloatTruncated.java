package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeUnary;

public abstract class PrimFloatTruncated extends PrimitiveNodeUnary {
    public PrimFloatTruncated(CompiledMethodObject code) {
        super(code);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    int truncateToInt(double receiver) {
        return Math.toIntExact((long) Math.floor(receiver));
    }

    @Specialization
    long truncate(double receiver) {
        return (long) Math.floor(receiver);
    }
}
