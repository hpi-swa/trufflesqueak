package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimFileSize extends PrimitiveBinaryOperation {
    public PrimFileSize(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    int size(@SuppressWarnings("unused") Object receiver, int fd) {
        // TODO: use registry of files
        if (fd <= 2) {
            return 0;
        }
        throw new PrimitiveFailed();
    }

    // TODO: double, long, BigInteger
}
