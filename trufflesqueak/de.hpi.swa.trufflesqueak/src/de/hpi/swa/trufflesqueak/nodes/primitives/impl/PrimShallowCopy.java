package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveUnaryOperation;

public abstract class PrimShallowCopy extends PrimitiveUnaryOperation {
    public PrimShallowCopy(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    Object copy(BaseSqueakObject self) {
        return self.shallowCopy();
    }
}
