package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeBinary;

public abstract class PrimCompiledCodeAt extends PrimitiveNodeBinary {
    public PrimCompiledCodeAt(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    Object literalAt(CompiledCodeObject receiver, int idx) {
        return receiver.getLiteral(idx - 1);
    }
}
