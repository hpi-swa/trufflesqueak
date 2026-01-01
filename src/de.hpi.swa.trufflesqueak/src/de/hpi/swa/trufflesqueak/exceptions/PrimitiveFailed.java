/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.ControlFlowException;

import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ERROR_TABLE;

public final class PrimitiveFailed extends ControlFlowException {
    private static final long serialVersionUID = 1L;

    public static final PrimitiveFailed GENERIC_ERROR = new PrimitiveFailed(ERROR_TABLE.GENERIC_ERROR.ordinal());
    public static final PrimitiveFailed BAD_RECEIVER = new PrimitiveFailed(ERROR_TABLE.BAD_RECEIVER.ordinal());
    public static final PrimitiveFailed BAD_ARGUMENT = new PrimitiveFailed(ERROR_TABLE.BAD_ARGUMENT.ordinal());
    public static final PrimitiveFailed BAD_INDEX = new PrimitiveFailed(ERROR_TABLE.BAD_INDEX.ordinal());
    public static final PrimitiveFailed BAD_NUMBER_OF_ARGUMENTS = new PrimitiveFailed(ERROR_TABLE.BAD_NUMBER_OF_ARGUMENTS.ordinal());
    public static final PrimitiveFailed INSUFFICIENT_OBJECT_MEMORY = new PrimitiveFailed(ERROR_TABLE.INSUFFICIENT_OBJECT_MEMORY.ordinal());

    private final int primFailCode;

    private PrimitiveFailed(final int primFailCode) {
        this.primFailCode = primFailCode;
    }

    public int getPrimFailCode() {
        return primFailCode;
    }

    public static PrimitiveFailed andTransferToInterpreter() {
        CompilerDirectives.transferToInterpreter();
        throw GENERIC_ERROR;
    }

    public static PrimitiveFailed andTransferToInterpreter(final int primFailCode) {
        CompilerDirectives.transferToInterpreter();
        throw new PrimitiveFailed(primFailCode);
    }

    public static PrimitiveFailed andTransferToInterpreterWithError(final Throwable t) {
        CompilerDirectives.transferToInterpreter();
        t.printStackTrace();
        throw GENERIC_ERROR;
    }
}
