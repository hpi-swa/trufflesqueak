/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.exceptions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.ControlFlowException;

import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.ERROR_TABLE;

public final class PrimitiveExceptions {

    protected static class AbstractPrimitiveFailed extends ControlFlowException {
        private static final long serialVersionUID = 1L;
        private final int reasonCode;

        protected AbstractPrimitiveFailed(final int reasonCode) {
            this.reasonCode = reasonCode;
        }

        public final int getReasonCode() {
            return reasonCode;
        }
    }

    public static final class PrimitiveFailed extends AbstractPrimitiveFailed {
        private static final long serialVersionUID = 1L;

        public static final PrimitiveFailed GENERIC_ERROR = new PrimitiveFailed(ERROR_TABLE.GENERIC_ERROR.ordinal());
        public static final PrimitiveFailed BAD_RECEIVER = new PrimitiveFailed(ERROR_TABLE.BAD_RECEIVER.ordinal());
        public static final PrimitiveFailed BAD_ARGUMENT = new PrimitiveFailed(ERROR_TABLE.BAD_ARGUMENT.ordinal());
        public static final PrimitiveFailed BAD_INDEX = new PrimitiveFailed(ERROR_TABLE.BAD_INDEX.ordinal());
        public static final PrimitiveFailed INAPPROPRIATE_OPERATION = new PrimitiveFailed(ERROR_TABLE.INAPPROPRIATE_OPERATION.ordinal());
        public static final PrimitiveFailed INSUFFICIENT_OBJECT_MEMORY = new PrimitiveFailed(ERROR_TABLE.INSUFFICIENT_OBJECT_MEMORY.ordinal());

        private PrimitiveFailed(final int reasonCode) {
            super(reasonCode);
        }

        public static PrimitiveFailed andTransferToInterpreter() {
            CompilerDirectives.transferToInterpreter();
            throw GENERIC_ERROR;
        }

        public static PrimitiveFailed andTransferToInterpreter(final int reason) {
            CompilerDirectives.transferToInterpreter();
            throw new PrimitiveFailed(reason);
        }
    }

    public static final class SimulationPrimitiveFailed extends AbstractPrimitiveFailed {
        private static final long serialVersionUID = 1L;

        public SimulationPrimitiveFailed(final int reasonCode) {
            super(reasonCode);
        }
    }

    private PrimitiveExceptions() {
    }
}
