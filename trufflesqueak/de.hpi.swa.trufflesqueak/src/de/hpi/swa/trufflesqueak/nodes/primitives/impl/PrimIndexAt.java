package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public abstract class PrimIndexAt extends PrimAt {
    public PrimIndexAt(CompiledMethodObject code) {
        super(code);
    }

    @Override
    @Specialization
    protected Object at(AbstractPointersObject receiver, int idx) {
        return receiver.at0(idx - 1 + receiver.instsize());
    }

    @Override
    @Specialization
    protected Object at(BaseSqueakObject receiver, int idx) {
        return super.at(receiver, idx);
    }
}
