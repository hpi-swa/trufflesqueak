package de.hpi.swa.trufflesqueak.exceptions;

public class Return extends Exception {
    private static final long serialVersionUID = 1L;
    public final Object returnValue;

    public Return(Object result) {
        returnValue = result;
    }
}
