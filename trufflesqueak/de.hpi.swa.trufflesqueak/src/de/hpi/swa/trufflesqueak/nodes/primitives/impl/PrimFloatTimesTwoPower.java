package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeBinary;

public abstract class PrimFloatTimesTwoPower extends PrimitiveNodeBinary {
    public PrimFloatTimesTwoPower(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    double calc(double receiver, long argument) {
        return receiver * Math.pow(2, argument);
    }
}
