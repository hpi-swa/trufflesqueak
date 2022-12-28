/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.ControlFlowException;

import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ERROR_TABLE;

public final class PrimitiveExceptions {

    public static final class PrimitiveFailed extends ControlFlowException {
        private static final long serialVersionUID = 1L;

        public static final PrimitiveFailed GENERIC_ERROR = new PrimitiveFailed(ERROR_TABLE.GENERIC_ERROR.ordinal());
        public static final PrimitiveFailed BAD_RECEIVER = new PrimitiveFailed(ERROR_TABLE.BAD_RECEIVER.ordinal());
        public static final PrimitiveFailed BAD_ARGUMENT = new PrimitiveFailed(ERROR_TABLE.BAD_ARGUMENT.ordinal());
        public static final PrimitiveFailed BAD_INDEX = new PrimitiveFailed(ERROR_TABLE.BAD_INDEX.ordinal());
        public static final PrimitiveFailed INAPPROPRIATE_OPERATION = new PrimitiveFailed(ERROR_TABLE.INAPPROPRIATE_OPERATION.ordinal());
        public static final PrimitiveFailed INSUFFICIENT_OBJECT_MEMORY = new PrimitiveFailed(ERROR_TABLE.INSUFFICIENT_OBJECT_MEMORY.ordinal());

        private final int reasonCode;

        private PrimitiveFailed(final int reasonCode) {
            this.reasonCode = reasonCode;
        }

        public int getReasonCode() {
            return reasonCode;
        }

        public static PrimitiveFailed andTransferToInterpreter() {
            CompilerDirectives.transferToInterpreter();
            throw GENERIC_ERROR;
        }

        public static PrimitiveFailed andTransferToInterpreter(final int reason) {
            CompilerDirectives.transferToInterpreter();
            throw new PrimitiveFailed(reason);
        }

        public static PrimitiveFailed andTransferToInterpreterWithError(final Exception e) {
            CompilerDirectives.transferToInterpreter();
            e.printStackTrace();
            throw GENERIC_ERROR;
        }
    }

    private PrimitiveExceptions() {
    }
}
