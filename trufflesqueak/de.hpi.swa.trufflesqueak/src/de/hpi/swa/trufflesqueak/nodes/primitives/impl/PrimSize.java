package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.LargeInteger;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveUnaryOperation;

public class PrimSize extends PrimitiveUnaryOperation {
    public PrimSize(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization(guards = "isNull(obj)")
    public long size(@SuppressWarnings("unused") Object obj) {
        return 0;
    }

    @Specialization
    public long size(@SuppressWarnings("unused") boolean o) {
        return 0;
    }

    @Specialization
    public long size(@SuppressWarnings("unused") int o) {
        return 0;
    }

    @Specialization
    public long size(@SuppressWarnings("unused") double o) {
        return 2; // Float in words
    }

    @Specialization
    public long size(String s) {
        return s.getBytes().length;
    }

    @Specialization
    public long size(BigInteger i) {
        return LargeInteger.byteSize(i);
    }

    @Specialization
    public long size(BaseSqueakObject obj) {
        return obj.size();
    }
}
