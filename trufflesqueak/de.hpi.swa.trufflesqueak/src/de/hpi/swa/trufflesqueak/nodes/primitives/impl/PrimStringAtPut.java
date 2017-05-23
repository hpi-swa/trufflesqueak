package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveTernaryOperation;

public class PrimStringAtPut extends PrimitiveTernaryOperation {
    public PrimStringAtPut(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    char atput(NativeObject obj, int idx, char value) {
        obj.setNativeAt0(idx - 1, value);
        return value;
    }

    @Specialization
    char atput(NativeObject obj, int idx, int value) {
        char charValue = (char) ((Integer) value).byteValue();
        obj.setNativeAt0(idx - 1, charValue);
        return charValue;
    }
}
