package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveQuinaryOperation;

public class PrimFileWrite extends PrimitiveQuinaryOperation {
    public PrimFileWrite(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    @TruffleBoundary
    int write(@SuppressWarnings("unused") Object receiver, int fd, NativeObject content, int start, int count) {
        // TODO: use registry of files
        String chars = content.toString();
        int elementSize = content.getElementSize();
        int byteStart = (start - 1) * elementSize;
        int byteEnd = Math.min(start - 1 + count, chars.length()) * elementSize;
        switch (fd) {
            case 1:
                method.image.getOutput().append(chars, byteStart, byteEnd);
                method.image.getOutput().flush();
                break;
            case 2:
                method.image.getError().append(chars, byteStart, byteEnd);
                method.image.getError().flush();
                break;
            default:
                throw new PrimitiveFailed();
        }
        return (byteEnd - byteStart) / elementSize;
    }

    // TODO: double, long, BigInteger
}
