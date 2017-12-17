package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeBinary;

public abstract class PrimStringAt extends PrimitiveNodeBinary {
    public PrimStringAt(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    char stringAt(NativeObject obj, int idx) {
        byte nativeAt0 = ((Long) obj.getNativeAt0(idx - 1)).byteValue();
        return (char) nativeAt0;
    }
}
