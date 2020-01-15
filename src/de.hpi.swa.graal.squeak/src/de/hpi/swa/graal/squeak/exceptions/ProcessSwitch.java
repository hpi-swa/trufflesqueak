/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.exceptions;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.ControlFlowException;

import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.PointersObject;

public final class ProcessSwitch extends ControlFlowException {
    private static final long serialVersionUID = 1L;
    private final ContextObject newContext;
    private final ContextObject oldContext;
    private final PointersObject oldProcess;

    private ProcessSwitch(final ContextObject newContext, final ContextObject oldContext, final PointersObject oldProcess) {
        this.oldContext = oldContext;
        this.oldProcess = oldProcess;
        this.newContext = newContext;
    }

    public static ProcessSwitch create(final ContextObject newContext, final ContextObject oldContext, final PointersObject oldProcess) {
        assert !newContext.isTerminated();
        return new ProcessSwitch(newContext, oldContext, oldProcess);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static ProcessSwitch createWithBoundary(final ContextObject newContext, final ContextObject oldContext, final PointersObject oldProcess) {
        return new ProcessSwitch(newContext, oldContext, oldProcess);
    }

    public ContextObject getNewContext() {
        return newContext;
    }

    public PointersObject getOldProcess() {
        return oldProcess;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "Process switch from process @" + Integer.toHexString(oldProcess.hashCode()) + " and " + oldContext + " to " + newContext;
    }
}
