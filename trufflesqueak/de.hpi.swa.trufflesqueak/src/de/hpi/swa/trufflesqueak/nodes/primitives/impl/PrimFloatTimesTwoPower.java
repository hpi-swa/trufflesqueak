package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimFloatTimesTwoPower extends PrimitiveBinaryOperation {
    public PrimFloatTimesTwoPower(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    double calc(double receiver, long argument) {
        return receiver * Math.pow(2, argument);
    }
}
