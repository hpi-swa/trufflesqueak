package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.Chunk;
import de.hpi.swa.trufflesqueak.exceptions.InvalidIndex;

public class ListObject extends SqueakObject implements TruffleObject {
    private BaseSqueakObject[] pointers;

    @Override
    public void fillin(Chunk chunk) {
        super.fillin(chunk);
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

}
