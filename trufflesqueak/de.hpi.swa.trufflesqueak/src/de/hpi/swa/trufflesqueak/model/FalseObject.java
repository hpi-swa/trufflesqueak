package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.SqueakImageContext;

public final class FalseObject extends BooleanObject implements TruffleObject {
    public FalseObject(SqueakImageContext img) {
        super(img);
    }

    @Override
    public String toString() {
        return "false";
    }

    @Override
    public ForeignAccess getForeignAccess() {
        // TODO Auto-generated method stub
        return null;
    }
}
