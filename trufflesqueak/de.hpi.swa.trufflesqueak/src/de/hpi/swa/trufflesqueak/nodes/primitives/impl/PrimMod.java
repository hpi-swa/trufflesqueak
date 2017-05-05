package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimMod extends PrimitiveBinaryOperation {
    public PrimMod(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    int mod(int a, int b) {
        return a % b;
    }
}
