package de.hpi.swa.trufflesqueak.exceptions;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;

public class NonVirtualReturn extends Return {

    public NonVirtualReturn(BaseSqueakObject object) {
        super(object);
    }
}
