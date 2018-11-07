package de.hpi.swa.graal.squeak.exceptions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.ControlFlowException;

import de.hpi.swa.graal.squeak.model.ObjectLayouts.ERROR_TABLE;

public final class PrimitiveExceptions {

    protected static class AbstractPrimitiveFailed extends ControlFlowException {
        private static final long serialVersionUID = 1L;
        private final long reasonCode;

        protected AbstractPrimitiveFailed(final long reasonCode) {
            this.reasonCode = reasonCode;
        }

        public final long getReasonCode() {
            return reasonCode;
        }
    }

    public static final class PrimitiveFailed extends AbstractPrimitiveFailed {
        private static final long serialVersionUID = 1L;

        public static void andTransferToInterpreter() {
            CompilerDirectives.transferToInterpreter();
            throw new PrimitiveFailed();
        }

        public static void andTransferToInterpreter(final long reason) {
            CompilerDirectives.transferToInterpreter();
            throw new PrimitiveFailed(reason);
        }

        public PrimitiveFailed() {
            this(ERROR_TABLE.GENERIC_ERROR);
        }

        public PrimitiveFailed(final long reasonCode) {
            super(reasonCode);
        }
    }

    public static final class SimulationPrimitiveFailed extends AbstractPrimitiveFailed {
        private static final long serialVersionUID = 1L;

        public SimulationPrimitiveFailed(final long reasonCode) {
            super(reasonCode);
        }
    }

    public static class PrimitiveWithoutResultException extends ControlFlowException {
        private static final long serialVersionUID = 1L;
    }

    private PrimitiveExceptions() {
    }
}
