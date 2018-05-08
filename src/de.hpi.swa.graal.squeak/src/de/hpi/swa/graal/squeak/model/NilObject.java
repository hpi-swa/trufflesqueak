package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class NilObject extends BaseSqueakObject {

    public NilObject(final SqueakImageContext img) {
        super(img);
    }

    @Override
    public String toString() {
        return "nil";
    }

    @Override
    public Object at0(final long l) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public void atput0(final long idx, final Object object) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public int instsize() {
        return 0;
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return this;
    }

    @Override
    public boolean isNil() {
        return true;
    }
}
