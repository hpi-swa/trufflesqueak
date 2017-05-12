package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ImmediateCharacter;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimCharacterValue extends PrimitiveBinaryOperation {

    public PrimCharacterValue(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    protected ImmediateCharacter value(@SuppressWarnings("unused") BaseSqueakObject ignored, int value) {
        return method.image.wrapChar(value);
    }
}
