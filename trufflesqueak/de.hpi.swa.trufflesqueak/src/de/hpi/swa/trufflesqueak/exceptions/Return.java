package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.ControlFlowException;

public class Return extends ControlFlowException {
    private static final long serialVersionUID = 1L;
    @CompilationFinal protected final Object returnValue;

    public Return(Object result) {
        returnValue = result;
    }

    public Object getReturnValue() {
        return returnValue;
    }
}
