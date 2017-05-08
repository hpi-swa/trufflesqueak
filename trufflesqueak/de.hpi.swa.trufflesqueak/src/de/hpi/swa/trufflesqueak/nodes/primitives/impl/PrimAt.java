package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.UnwrappingError;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimAt extends PrimitiveBinaryOperation {
    public PrimAt(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization(rewriteOn = UnwrappingError.class)
    protected int intAt(BaseSqueakObject receiver, int idx) throws UnwrappingError {
        try {
            return receiver.at0(idx - 1).unwrapInt();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new PrimitiveFailed();
        }
    }

    @Specialization
    protected BaseSqueakObject at(BaseSqueakObject receiver, int idx) {
        try {
            return receiver.at0(idx - 1);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new PrimitiveFailed();
        }
    }
}
