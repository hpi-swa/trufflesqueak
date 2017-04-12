package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

public final class TrueObject extends EmptyObject implements TruffleObject {

    public static final TrueObject SINGLETON = new TrueObject();

    private TrueObject() {
    }

    @Override
    public String toString() {
        return "true";
    }

    @Override
    public ForeignAccess getForeignAccess() {
        // TODO Auto-generated method stub
        return null;
    }
}
