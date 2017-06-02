package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.nodes.ControlFlowException;

public class SqueakExit extends ControlFlowException {
    public final int code;
    private static final long serialVersionUID = 1L;

    public SqueakExit(int code) {
        this.code = code;
    }
}
