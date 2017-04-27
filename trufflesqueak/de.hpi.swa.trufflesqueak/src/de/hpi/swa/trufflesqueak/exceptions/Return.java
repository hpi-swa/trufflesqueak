package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.nodes.ControlFlowException;

public class Return extends ControlFlowException {
    private static final long serialVersionUID = 1L;
    public final Object returnValue;

    public Return(Object result) {
        returnValue = result;
    }
}
