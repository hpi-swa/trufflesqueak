package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public abstract class PrimIndexAtPut extends PrimAtPut {
    public PrimIndexAtPut(CompiledMethodObject code) {
        super(code);
    }

    @Override
    @Specialization
    protected Object atput(AbstractPointersObject receiver, int idx, Object value) {
        receiver.atput0(idx - 1 + receiver.instsize(), value);
        return value;
    }

    @Override
    @Specialization
    protected Object atput(BaseSqueakObject receiver, int idx, Object value) {
        return super.atput(receiver, idx, value);
    }
}
