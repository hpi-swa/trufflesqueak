package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveTernaryOperation;

public class PrimCompiledCodeAtPut extends PrimitiveTernaryOperation {
    public PrimCompiledCodeAtPut(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    Object setLiteral(CompiledCodeObject cc, int idx, Object value) {
        cc.setLiteral(idx, value);
        return value;
    }
}
