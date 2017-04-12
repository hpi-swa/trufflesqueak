package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.Chunk;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;

public abstract class SqueakObject extends BaseSqueakObject {
    private int hash;
    private BaseSqueakObject sqClass;

    @Override
    public void fillin(Chunk chunk) {
        hash = chunk.getHash();
        sqClass = chunk.getSqClass();
    }

    @Override
    public BaseSqueakObject getSqClass() {
        return sqClass;
    }

    @Override
    public void become(BaseSqueakObject other) throws PrimitiveFailed {
        if (other instanceof SqueakObject) {
            int hash2 = ((SqueakObject) other).hash;
            ((SqueakObject) other).hash = this.hash;
            this.hash = hash2;

            BaseSqueakObject sqClass2 = ((SqueakObject) other).sqClass;
            ((SqueakObject) other).sqClass = this.sqClass;
            this.sqClass = sqClass2;
        }
        throw new PrimitiveFailed();
    }
}
