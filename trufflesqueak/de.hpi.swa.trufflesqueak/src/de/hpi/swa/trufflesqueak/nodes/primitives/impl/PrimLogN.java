package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveUnaryOperation;

public abstract class PrimLogN extends PrimitiveUnaryOperation {
    public PrimLogN(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    double logn(double a) {
        return Math.log(a);
    }
}
