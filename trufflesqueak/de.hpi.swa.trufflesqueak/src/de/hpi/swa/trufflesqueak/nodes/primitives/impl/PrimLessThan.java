package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimLessThan extends PrimitiveBinaryOperation {
    public PrimLessThan(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    boolean lt(long a, long b) {
        return a < b;
    }
}
