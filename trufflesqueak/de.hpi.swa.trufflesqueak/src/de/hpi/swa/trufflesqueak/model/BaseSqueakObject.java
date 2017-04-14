package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.Chunk;
import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.InvalidIndex;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;

public abstract class BaseSqueakObject {
    SqueakImageContext image;

    @SuppressWarnings("unused")
    public void fillin(Chunk chunk, SqueakImageContext img) {
        this.image = img;
    }

    @Override
    public String toString() {
        return "a " + getSqClassName();
    }

    public abstract BaseSqueakObject getSqClass();

    public boolean isClass() {
        return false;
    }

    public String nameAsClass() {
        return "???NotAClass";
    }

    public String getSqClassName() {
        if (isClass()) {
            return nameAsClass() + " class";
        } else {
            return getSqClass().nameAsClass();
        }
    }

    public abstract void become(BaseSqueakObject other) throws PrimitiveFailed;
}
