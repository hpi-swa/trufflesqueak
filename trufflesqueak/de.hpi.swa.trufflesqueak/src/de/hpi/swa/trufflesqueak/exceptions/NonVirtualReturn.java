package de.hpi.swa.trufflesqueak.exceptions;

import de.hpi.swa.trufflesqueak.model.MethodContextObject;

public class NonVirtualReturn extends Return {
    private static final long serialVersionUID = 1L;
    private final MethodContextObject targetContext;
    private final MethodContextObject currentContext;

    public NonVirtualReturn(Object returnValue, MethodContextObject targetContext, MethodContextObject currentContext) {
        super(returnValue);
        this.targetContext = targetContext;
        this.currentContext = currentContext;
    }

    public MethodContextObject getTargetContext() {
        return targetContext;
    }

    public MethodContextObject getCurrentContext() {
        return currentContext;
    }
}
