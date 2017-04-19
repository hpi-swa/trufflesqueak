package de.hpi.swa.trufflesqueak.exceptions;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;

public class NonLocalReturn extends Return {
    private static final long serialVersionUID = 1L;

    public NonLocalReturn(BaseSqueakObject object) {
        super(object);
    }
}
