package de.hpi.swa.graal.squeak.exceptions;

import com.oracle.truffle.api.nodes.ControlFlowException;

import de.hpi.swa.graal.squeak.model.ContextObject;

public final class ProcessSwitch extends ControlFlowException {
    private static final long serialVersionUID = 1L;
    private final ContextObject newContext;

    private ContextObject lastSeenContext;

    public ProcessSwitch(final ContextObject newContext, final ContextObject lastSeenContext) {
        this.newContext = newContext;
        this.lastSeenContext = lastSeenContext;
    }

    public ContextObject getNewContext() {
        return newContext;
    }

    public ContextObject getLastSeenContext() {
        return lastSeenContext;
    }

    public void setLastSeenContext(final ContextObject lastSeenContext) {
        this.lastSeenContext = lastSeenContext;
    }

    @Override
    public String toString() {
        return "Process switch to " + newContext;
    }
}
