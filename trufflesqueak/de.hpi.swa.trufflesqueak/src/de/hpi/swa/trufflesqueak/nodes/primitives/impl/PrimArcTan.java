package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveUnaryOperation;

public class PrimArcTan extends PrimitiveUnaryOperation {
    public PrimArcTan(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    double arctan(double a) {
        return Math.atan(a);
    }
}
