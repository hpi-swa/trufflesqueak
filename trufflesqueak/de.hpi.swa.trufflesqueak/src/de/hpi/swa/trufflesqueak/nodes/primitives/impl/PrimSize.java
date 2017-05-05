package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveUnaryOperation;

public class PrimSize extends PrimitiveUnaryOperation {
    public PrimSize(CompiledMethodObject cm) {
        super(cm);
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
    public int size(@SuppressWarnings("unused") double o) {
        return 2; // Float in words
    }

    @Specialization
    public int size(String s) {
        return s.getBytes().length;
    }

    @Specialization
    public int size(BigInteger i) {
        int bitCount = i.bitCount();
        return (int) Math.round(Math.ceil(bitCount / 8.0)); // Large*Integer in bytes
    }

    @Specialization(guards = "isNull(obj)")
    public int size(@SuppressWarnings("unused") Object obj) {
        return 0;
    }

    @Specialization
    public int size(BaseSqueakObject obj) {
        return obj.size();
    }

    protected static boolean isNull(Object obj) {
        return obj == null;
    }
}
