package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeUnary;

public abstract class PrimExp extends PrimitiveNodeUnary {
    public PrimExp(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    double exp(double a) {
        return Math.exp(a);
    }
}
