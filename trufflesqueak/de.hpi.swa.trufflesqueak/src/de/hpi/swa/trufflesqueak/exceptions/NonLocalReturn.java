package de.hpi.swa.trufflesqueak.exceptions;

import de.hpi.swa.trufflesqueak.model.ContextObject;

public class NonLocalReturn extends Return {
    private static final long serialVersionUID = 1L;
    private final ContextObject targetContext;
    private boolean arrivedAtTargetContext = false;

    public NonLocalReturn(Object returnValue, ContextObject targetContext) {
        super(returnValue);
        this.targetContext = targetContext;
    }

    public ContextObject getTargetContext() {
        return targetContext;
    }

    public boolean hasArrivedAtTargetContext() {
        return arrivedAtTargetContext;
    }

    public void setArrivedAtTargetContext() {
        arrivedAtTargetContext = true;
    }
}
