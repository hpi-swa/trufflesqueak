package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.ControlFlowException;

import de.hpi.swa.trufflesqueak.model.ObjectLayouts.ERROR_TABLE;

public final class PrimitiveExceptions {
    public static class AbstractPrimitiveFailed extends ControlFlowException {
        private static final long serialVersionUID = 1L;
        @CompilationFinal private final long reasonCode;

        public AbstractPrimitiveFailed(long reasonCode) {
            this.reasonCode = reasonCode;
        }

        public long getReasonCode() {
            return reasonCode;
        }
    }

    public static class PrimitiveFailed extends AbstractPrimitiveFailed {
        private static final long serialVersionUID = 1L;

        public PrimitiveFailed() {
            this(ERROR_TABLE.GENERIC_ERROR);
        }

        public PrimitiveFailed(long reasonCode) {
            super(reasonCode);
        }
    }

    public static class SimulationPrimitiveFailed extends AbstractPrimitiveFailed {
        private static final long serialVersionUID = 1L;

        public SimulationPrimitiveFailed(final long reasonCode) {
            super(reasonCode);
        }
    }

    public static class PrimitiveWithoutResultException extends ControlFlowException {
        private static final long serialVersionUID = 1L;
    }
}
