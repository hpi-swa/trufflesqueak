package de.hpi.swa.trufflesqueak.exceptions;

import de.hpi.swa.trufflesqueak.model.MethodContextObject;

public class NonLocalReturn extends Return {
    private static final long serialVersionUID = 1L;
    private final MethodContextObject targetContext;
    private boolean arrivedAtTargetContext = false;

    public NonLocalReturn(Object returnValue, MethodContextObject targetContext) {
        super(returnValue);
        this.targetContext = targetContext;
    }

    public MethodContextObject getTargetContext() {
        return targetContext;
    }

    public boolean hasArrivedAtTargetContext() {
        return arrivedAtTargetContext;
    }

    public void setArrivedAtTargetContext() {
        arrivedAtTargetContext = true;
    }
}
