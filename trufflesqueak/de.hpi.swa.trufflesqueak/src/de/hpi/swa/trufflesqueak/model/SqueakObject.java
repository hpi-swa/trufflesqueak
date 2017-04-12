package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.Chunk;

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
}
