package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.nodes.ControlFlowException;

import de.hpi.swa.trufflesqueak.model.MethodContextObject;

public class ProcessSwitch extends ControlFlowException {
    private static final long serialVersionUID = 1L;
    private final MethodContextObject context;

    public ProcessSwitch(MethodContextObject context) {
        this.context = context;
    }

    public MethodContextObject getNewContext() {
        return context;
    }
}
