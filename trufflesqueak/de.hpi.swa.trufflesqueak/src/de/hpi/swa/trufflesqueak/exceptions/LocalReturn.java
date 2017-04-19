package de.hpi.swa.trufflesqueak.exceptions;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;

public class LocalReturn extends Return {
    private static final long serialVersionUID = 1L;

    public LocalReturn(BaseSqueakObject object) {
        super(object);
    }
}
