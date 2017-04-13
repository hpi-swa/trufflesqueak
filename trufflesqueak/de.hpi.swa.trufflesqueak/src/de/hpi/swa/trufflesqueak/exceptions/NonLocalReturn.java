package de.hpi.swa.trufflesqueak.exceptions;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;

public class NonLocalReturn extends Return {

    public NonLocalReturn(BaseSqueakObject object) {
        super(object);
    }
}
