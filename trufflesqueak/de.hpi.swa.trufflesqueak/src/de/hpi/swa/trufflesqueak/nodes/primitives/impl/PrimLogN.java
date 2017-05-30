package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveUnaryOperation;

public class PrimLogN extends PrimitiveUnaryOperation {
    public PrimLogN(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    double logn(double a) {
        return Math.log(a);
    }
}
