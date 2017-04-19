package de.hpi.swa.trufflesqueak.exceptions;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;

public class NonVirtualReturn extends Return {
    private static final long serialVersionUID = 1L;

    public NonVirtualReturn(BaseSqueakObject object) {
        super(object);
    }
}
