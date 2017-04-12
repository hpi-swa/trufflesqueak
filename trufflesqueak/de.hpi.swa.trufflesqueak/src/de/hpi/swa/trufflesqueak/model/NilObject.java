package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

public final class NilObject extends EmptyObject implements TruffleObject {

    public static final NilObject SINGLETON = new NilObject();

    private NilObject() {
    }

    @Override
    public String toString() {
        return "nil";
    }

    @Override
    public ForeignAccess getForeignAccess() {
        // TODO Auto-generated method stub
        return null;
    }
}
