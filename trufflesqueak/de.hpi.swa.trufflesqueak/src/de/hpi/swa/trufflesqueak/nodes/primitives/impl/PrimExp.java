package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveUnaryOperation;

public class PrimExp extends PrimitiveUnaryOperation {
    public PrimExp(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    double exp(double a) {
        return Math.exp(a);
    }
}
