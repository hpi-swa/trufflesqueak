package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.ControlFlowException;

public final class PrimitiveExceptions {
    public static class PrimitiveFailed extends ControlFlowException {
        private static final long serialVersionUID = 1L;
        @CompilationFinal private final String reason;

        public PrimitiveFailed() {
            this(null);
        }

        public PrimitiveFailed(String reason) {
            this.reason = reason;
        }
    }

    public static class PrimitiveWithoutResultException extends ControlFlowException {
        private static final long serialVersionUID = 1L;
    }
}
