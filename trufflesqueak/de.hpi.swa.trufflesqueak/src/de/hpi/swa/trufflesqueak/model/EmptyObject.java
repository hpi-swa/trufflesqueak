package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.SqueakImageContext;

public class EmptyObject extends SqueakObject implements TruffleObject {
    public EmptyObject(SqueakImageContext img) {
        super(img);
    }

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

    @Override
    public BaseSqueakObject at0(int idx) {
        throw new ArrayIndexOutOfBoundsException();
    }

    @Override
    public void atput0(int idx, BaseSqueakObject obj) {
        throw new ArrayIndexOutOfBoundsException();
    }

    @Override
    public int instsize() {
        return 0;
    }
}
