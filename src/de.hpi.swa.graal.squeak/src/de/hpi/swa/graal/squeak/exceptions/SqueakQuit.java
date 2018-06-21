package de.hpi.swa.graal.squeak.exceptions;

import com.oracle.truffle.api.TruffleException;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.Node;

public class SqueakQuit extends ControlFlowException implements TruffleException {
    private static final long serialVersionUID = 1L;
    private final int exitStatus;

    public SqueakQuit(final int exitStatus) {
        this.exitStatus = exitStatus;
    }

    public int getExitStatus() {
        return exitStatus;
    }

    public Node getLocation() {
        return null;
    }

    public boolean isExit() {
        return true;
    }
}
