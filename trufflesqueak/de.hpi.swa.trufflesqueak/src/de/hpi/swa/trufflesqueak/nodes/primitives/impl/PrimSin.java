package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeUnary;

public abstract class PrimSin extends PrimitiveNodeUnary {
    public PrimSin(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    double sin(double a) {
        return Math.sin(a);
    }
}
