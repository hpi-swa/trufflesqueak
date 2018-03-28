package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class NilObject extends BaseSqueakObject {

    public NilObject(SqueakImageContext img) {
        super(img);
    }

    @Override
    public String toString() {
        return "nil";
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
    }

    @Override
    public final ClassObject getSqClass() {
        return image.nilClass;
    }

    @Override
    public Object at0(long l) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public void atput0(long idx, Object object) {
    }

    @Override
    public final int size() {
        return 0;
    }

    @Override
    public final int instsize() {
        return 0;
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return this;
    }

    @Override
    public long squeakHash() {
        return 1L;
    }
}
