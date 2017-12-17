package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeUnary;

public abstract class PrimFloatExponent extends PrimitiveNodeUnary {
    public PrimFloatExponent(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    int exponentAsInt(double receiver) {
        return Math.getExponent(receiver);
    }
}
