/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.ControlFlowException;

import de.hpi.swa.trufflesqueak.model.ContextObject;

public final class ProcessSwitch extends ControlFlowException {
    private static final long serialVersionUID = 1L;
    private final transient ContextObject newContext;

    private ProcessSwitch(final ContextObject newContext) {
        assert !newContext.isDead() : "Cannot switch to terminated context";
        this.newContext = newContext;
    }

    public static ProcessSwitch create(final ContextObject newContext) {
        return new ProcessSwitch(newContext);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static ProcessSwitch createWithBoundary(final ContextObject newContext) {
        return new ProcessSwitch(newContext);
    }

    /**
     * Throw this to continue execution with the new Context (with unconditional @TruffleBoundary).
     */
    public static ProcessSwitch transferExecutionToContextUncached(final ContextObject newActiveContext) {
        return ProcessSwitch.createWithBoundary(newActiveContext);
    }

    /**
     * Throw this to continue execution with the new Context.
     */
    public static ProcessSwitch transferExecutionToContext(final ContextObject newActiveContext) {
        if (CompilerDirectives.isPartialEvaluationConstant(newActiveContext)) {
            return ProcessSwitch.create(newActiveContext);
        } else {
            // Avoid further PE if newActiveContext is not a PE constant.
            return ProcessSwitch.createWithBoundary(newActiveContext);
        }
    }

    public ContextObject getNewContext() {
        return newContext;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "Process switch to " + newContext;
    }
}
