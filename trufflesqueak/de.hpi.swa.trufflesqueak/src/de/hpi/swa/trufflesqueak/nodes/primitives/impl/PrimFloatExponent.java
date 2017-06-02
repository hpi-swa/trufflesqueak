package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveUnaryOperation;

public class PrimFloatExponent extends PrimitiveUnaryOperation {
    public PrimFloatExponent(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    int exponentAsInt(double receiver) {
        return Math.getExponent(receiver);
    }
}
