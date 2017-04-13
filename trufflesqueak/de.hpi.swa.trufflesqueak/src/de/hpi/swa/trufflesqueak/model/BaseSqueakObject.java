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

    public String getSqClassName() {
        BaseSqueakObject cls = getSqClass();
        if (cls == image.metaclass) {
            return "Metaclass";
        } else if (cls instanceof PointersObject) {
            return ((PointersObject) cls).nameAsClass();
        }
        return "?class?";
    }

    public abstract void become(BaseSqueakObject other) throws PrimitiveFailed;
}
