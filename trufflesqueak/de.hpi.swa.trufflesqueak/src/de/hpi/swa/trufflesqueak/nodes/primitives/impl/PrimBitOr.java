package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimBitOr extends PrimitiveBinaryOperation {
    public PrimBitOr(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    protected int bitOr(int receiver, int arg) {
        return receiver | arg;
    }
}
