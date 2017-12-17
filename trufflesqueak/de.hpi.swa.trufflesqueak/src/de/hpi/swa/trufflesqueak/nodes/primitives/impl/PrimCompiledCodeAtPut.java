package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeTernary;

public abstract class PrimCompiledCodeAtPut extends PrimitiveNodeTernary {
    public PrimCompiledCodeAtPut(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    Object setLiteral(CompiledCodeObject cc, int idx, Object value) {
        cc.setLiteral(idx, value);
        return value;
    }
}
