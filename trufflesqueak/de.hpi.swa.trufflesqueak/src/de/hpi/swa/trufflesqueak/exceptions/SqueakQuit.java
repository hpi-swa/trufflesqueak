package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.nodes.ControlFlowException;

public class SqueakQuit extends ControlFlowException {
    private static final long serialVersionUID = 1L;
    private final int exitCode;

    public SqueakQuit(int exitCode) {
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }
}
