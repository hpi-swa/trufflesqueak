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

        public static final PrimitiveFailed GENERIC_ERROR = new PrimitiveFailed(ERROR_TABLE.GENERIC_ERROR);
        public static final PrimitiveFailed BAD_RECEIVER = new PrimitiveFailed(ERROR_TABLE.BAD_RECEIVER);
        public static final PrimitiveFailed BAD_ARGUMENT = new PrimitiveFailed(ERROR_TABLE.BAD_ARGUMENT);
        public static final PrimitiveFailed BAD_INDEX = new PrimitiveFailed(ERROR_TABLE.BAD_INDEX);
        public static final PrimitiveFailed INAPPROPRIATE_OPERATION = new PrimitiveFailed(ERROR_TABLE.INAPPROPRIATE_OPERATION);
        public static final PrimitiveFailed INSUFFICIENT_OBJECT_MEMORY = new PrimitiveFailed(ERROR_TABLE.INSUFFICIENT_OBJECT_MEMORY);

        private PrimitiveFailed(final long reasonCode) {
            super(reasonCode);
        }

        public static PrimitiveFailed andTransferToInterpreter() {
            CompilerDirectives.transferToInterpreter();
            throw GENERIC_ERROR;
        }

        public static PrimitiveFailed andTransferToInterpreter(final long reason) {
            CompilerDirectives.transferToInterpreter();
            throw new PrimitiveFailed(reason);
        }
    }

    public static final class SimulationPrimitiveFailed extends AbstractPrimitiveFailed {
        private static final long serialVersionUID = 1L;

        public SimulationPrimitiveFailed(final long reasonCode) {
            super(reasonCode);
        }
    }

    private PrimitiveExceptions() {
    }
}
