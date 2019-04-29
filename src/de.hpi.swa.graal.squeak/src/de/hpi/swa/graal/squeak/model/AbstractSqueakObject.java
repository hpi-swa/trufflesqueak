package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.interop.TruffleObject;

public abstract class AbstractSqueakObject implements TruffleObject {

    public abstract int instsize();

    public abstract int size();

}
