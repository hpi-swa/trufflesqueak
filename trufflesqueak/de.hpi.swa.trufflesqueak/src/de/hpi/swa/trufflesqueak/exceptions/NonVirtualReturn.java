package de.hpi.swa.trufflesqueak.exceptions;

import de.hpi.swa.trufflesqueak.model.ContextObject;

public class NonVirtualReturn extends Return {
    private static final long serialVersionUID = 1L;
    private final ContextObject targetContext;
    private final ContextObject currentContext;

    public NonVirtualReturn(Object returnValue, ContextObject targetContext, ContextObject currentContext) {
        super(returnValue);
        this.targetContext = targetContext;
        this.currentContext = currentContext;
    }

    public ContextObject getTargetContext() {
        return targetContext;
    }

    public ContextObject getCurrentContext() {
        return currentContext;
    }
}
