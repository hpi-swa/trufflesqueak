package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.LargeInteger;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveUnaryOperation;

public abstract class PrimSize extends PrimitiveUnaryOperation {
    public PrimSize(CompiledMethodObject code) {
        super(code);
    }

    @Specialization(guards = "isNull(obj)")
    public int size(@SuppressWarnings("unused") char obj) {
        return 0;
    }

    @Specialization
    public int size(@SuppressWarnings("unused") boolean o) {
        return 0;
    }

    @Specialization
    public int size(@SuppressWarnings("unused") int o) {
        return 0;
    }

    @Specialization
    public int size(@SuppressWarnings("unused") long o) {
        return 0;
    }

    @Specialization
    public int size(String s) {
        return s.getBytes().length;
    }

    @Specialization
    public int size(BigInteger i) {
        return LargeInteger.byteSize(i);
    }

    @Specialization
    public int size(@SuppressWarnings("unused") double o) {
        return 2; // Float in words
    }

    @Specialization
    public int size(BaseSqueakObject obj) {
        return obj.size();
    }
}
