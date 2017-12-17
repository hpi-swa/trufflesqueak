package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeUnary;

public abstract class PrimAsFloat extends PrimitiveNodeUnary {
    public PrimAsFloat(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    double asFloat(int v) {
        return v;
    }

    @Specialization
    double asFloat(long v) {
        return v;
    }
}
