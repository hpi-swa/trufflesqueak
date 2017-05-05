package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimEqual extends PrimitiveBinaryOperation {
    public PrimEqual(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    protected boolean eq(int receiver, int argument) {
        return receiver == argument;
    }
}
