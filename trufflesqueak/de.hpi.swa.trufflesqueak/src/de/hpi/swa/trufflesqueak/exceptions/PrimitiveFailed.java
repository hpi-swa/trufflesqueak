package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public class PrimitiveFailed extends RuntimeException {
    private static final long serialVersionUID = -7373781496172787180L;
    @CompilationFinal private final String reason;

    public PrimitiveFailed() {
        this(null);
    }

    public PrimitiveFailed(String reason) {
        this.reason = reason;
    }

}
