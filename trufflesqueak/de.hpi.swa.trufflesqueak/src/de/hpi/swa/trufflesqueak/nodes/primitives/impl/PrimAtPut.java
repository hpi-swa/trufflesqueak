package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.UnwrappingError;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveTernaryOperation;

public class PrimAtPut extends PrimitiveTernaryOperation {
    public PrimAtPut(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    protected BaseSqueakObject atput(BaseSqueakObject receiver, int idx, long value) {
        return atput(receiver, idx, method.image.wrapInt(value));
    }

    @Specialization
    protected BaseSqueakObject atput(BaseSqueakObject receiver, int idx, boolean value) {
        return atput(receiver, idx, method.image.wrapBool(value));
    }

    @Specialization
    protected BaseSqueakObject atput(BaseSqueakObject receiver, int idx, BaseSqueakObject value) {
        try {
            receiver.atput0(idx - 1, value);
            return value;
        } catch (UnwrappingError | ArrayIndexOutOfBoundsException e) {
            throw new PrimitiveFailed();
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isNull(value)")
    protected BaseSqueakObject atput(BaseSqueakObject receiver, int idx, Object value) {
        return atput(receiver, idx, method.image.nil);
    }
}
