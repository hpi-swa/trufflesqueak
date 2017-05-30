package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveUnaryOperation;

public class PrimFloatTruncated extends PrimitiveUnaryOperation {
    public PrimFloatTruncated(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    long truncate(double receiver) {
        return (long) Math.floor(receiver);
    }
}
