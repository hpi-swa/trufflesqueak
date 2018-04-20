package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.SqueakImageContext;
import de.hpi.swa.graal.squeak.util.SqueakImageChunk;

public class NilObject extends BaseSqueakObject {

    public NilObject(final SqueakImageContext img) {
        super(img);
    }

    @Override
    public String toString() {
        return "nil";
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
    }

    @Override
    public final ClassObject getSqClass() {
        return image.nilClass;
    }

    @Override
    public Object at0(final long l) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public void atput0(final long idx, final Object object) {
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

    @Override
    public final boolean isNil() {
        return true;
    }
}
