package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveUnaryOperation;

public class PrimIdentityHash extends PrimitiveUnaryOperation {
    public PrimIdentityHash(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    int hashLong(long obj) {
        return (int) obj;
    }

    @Specialization
    int hashLong(BigInteger obj) {
        return obj.hashCode();
    }

    @Specialization
    int hashObject(BaseSqueakObject obj) {
        return obj.squeakHash();
    }
}
