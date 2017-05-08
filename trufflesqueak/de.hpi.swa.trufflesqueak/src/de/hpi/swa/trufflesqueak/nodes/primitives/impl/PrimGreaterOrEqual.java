package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimGreaterOrEqual extends PrimitiveBinaryOperation {
    public PrimGreaterOrEqual(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    boolean gt(int a, int b) {
        return a >= b;
    }

    @Specialization
    boolean gt(long a, long b) {
        return a >= b;
    }
}
