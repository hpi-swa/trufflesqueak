package de.hpi.swa.trufflesqueak.nodes.primitives.impl.file;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeBinary;

public abstract class PrimFileSize extends PrimitiveNodeBinary {
    public PrimFileSize(CompiledMethodObject code) {
        super(code);
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
