package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveUnaryOperation;

public class PrimSquareRoot extends PrimitiveUnaryOperation {
    public PrimSquareRoot(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    double squareRoot(double a) {
        return Math.sqrt(a);
    }
}
