package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveUnaryOperation;

public class PrimSin extends PrimitiveUnaryOperation {
    public PrimSin(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    double sin(double a) {
        return Math.sin(a);
    }
}
