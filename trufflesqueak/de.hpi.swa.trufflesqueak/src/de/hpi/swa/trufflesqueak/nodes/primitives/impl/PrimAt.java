package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimAt extends PrimitiveBinaryOperation {
    public PrimAt(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    protected BaseSqueakObject at(BaseSqueakObject receiver, int index) {
        try {
            return receiver.at0(index - 1);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new PrimitiveFailed();
        }
    }
}
