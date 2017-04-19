package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.Chunk;

public class ListObject extends SqueakObject implements TruffleObject {
    private BaseSqueakObject[] pointers;

    @Override
    public void fillin(Chunk chunk, SqueakImageContext img) {
        super.fillin(chunk, img);
        pointers = chunk.getPointers();
    }

    public ForeignAccess getForeignAccess() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BaseSqueakObject at0(int i) {
        return pointers[i];
    }

    @Override
    public void atput0(int i, BaseSqueakObject object) {
        pointers[i] = object;
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof ListObject) {
            if (super.become(other)) {
                BaseSqueakObject[] pointers2 = ((ListObject) other).pointers;
                ((ListObject) other).pointers = this.pointers;
                this.pointers = pointers2;
                return false;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return pointers.length;
    }

    @Override
    public int instsize() {
        // TODO getSqClass().getInstanceSize()
        return 0;
    }
}
