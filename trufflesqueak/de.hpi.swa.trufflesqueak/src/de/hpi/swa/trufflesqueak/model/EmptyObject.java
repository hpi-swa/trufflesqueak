package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;

public class EmptyObject extends SqueakObject implements TruffleObject {
    public ForeignAccess getForeignAccess() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void become(BaseSqueakObject other) throws PrimitiveFailed {
        if (other instanceof EmptyObject) {
            super.become(other);
        }
        throw new PrimitiveFailed();
    }
}
