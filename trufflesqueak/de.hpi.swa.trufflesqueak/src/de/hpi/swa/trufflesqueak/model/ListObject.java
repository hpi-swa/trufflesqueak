package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.InvalidIndex;
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

    public BaseSqueakObject at0(int i) throws InvalidIndex {
        if (i < pointers.length) {
            return pointers[i];
        }
        throw new InvalidIndex();
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
}
