package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveUnaryOperation;

public class PrimNew extends PrimitiveUnaryOperation {
    public PrimNew(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    BaseSqueakObject newWithArg(ClassObject receiver) {
        return receiver.newInstance();
    }
}
