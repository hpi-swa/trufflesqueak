package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

public class EmptyObject extends SqueakObject implements TruffleObject {
    public ForeignAccess getForeignAccess() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof EmptyObject) {
            return super.become(other);
        }
        return false;
    }

    @Override
    public int size() {
        return 0;
    }
}
