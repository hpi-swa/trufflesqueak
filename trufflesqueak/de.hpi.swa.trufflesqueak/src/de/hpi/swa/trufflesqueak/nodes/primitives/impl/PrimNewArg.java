package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimNewArg extends PrimitiveBinaryOperation {
    public PrimNewArg(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    BaseSqueakObject newWithArg(ClassObject receiver, int size) {
        if (size == 0)
            return null;
        if (!receiver.isVariable())
            return null;
        return receiver.newInstance(size);
    }
}
