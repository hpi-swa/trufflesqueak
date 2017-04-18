package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.Chunk;

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

    /**
     * @param other The object to swap identities with
     */
    public boolean become(BaseSqueakObject other) {
        return false;
    }
}
