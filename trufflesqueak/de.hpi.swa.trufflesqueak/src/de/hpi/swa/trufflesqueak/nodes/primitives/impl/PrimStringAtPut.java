package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeTernary;

public abstract class PrimStringAtPut extends PrimitiveNodeTernary {
    public PrimStringAtPut(CompiledMethodObject code) {
        super(code);
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
