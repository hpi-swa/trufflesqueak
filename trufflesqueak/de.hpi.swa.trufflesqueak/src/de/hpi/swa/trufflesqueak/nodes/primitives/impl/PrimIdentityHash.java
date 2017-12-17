package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeUnary;

public abstract class PrimIdentityHash extends PrimitiveNodeUnary {
    public PrimIdentityHash(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    int hash(char obj) {
        return obj;
    }

    @Specialization
    int hash(int obj) {
        return obj;
    }

    @Specialization
    int hash(long obj) {
        return (int) obj;
    }

    @Specialization
    int hash(BigInteger obj) {
        return obj.hashCode();
    }

    @Specialization
    int hash(BaseSqueakObject obj) {
        return obj.squeakHash();
    }
}
