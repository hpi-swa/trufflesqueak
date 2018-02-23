package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.nodes.ControlFlowException;

import de.hpi.swa.trufflesqueak.SqueakImageContext;

public class SqueakException extends ControlFlowException {
    private static final long serialVersionUID = 1L;
    private final String message;

    public SqueakException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    // For testing purposes, can be removed later and replaced with assertions
    public static final class SqueakTestException extends SqueakException {
        private static final long serialVersionUID = 1L;

        public SqueakTestException(SqueakImageContext image, String message) {
            super(message);
            image.printSqStackTrace();
        }
    }
}
