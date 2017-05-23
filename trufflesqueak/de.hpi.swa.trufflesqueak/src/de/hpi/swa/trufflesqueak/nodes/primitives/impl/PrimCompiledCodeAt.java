package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimCompiledCodeAt extends PrimitiveBinaryOperation {
    public PrimCompiledCodeAt(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    Object literalAt(CompiledCodeObject receiver, int idx) {
        return receiver.getLiteral(idx - 1);
    }
}
