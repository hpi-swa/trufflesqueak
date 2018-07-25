package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public abstract class AbstractPointersObject extends AbstractSqueakObject {
    protected Object[] pointers;

    protected AbstractPointersObject(final SqueakImageContext image) {
        super(image);
    }

    public AbstractPointersObject(final SqueakImageContext image, final ClassObject sqClass) {
        super(image, sqClass);
    }

    public AbstractPointersObject(final SqueakImageContext image, final long hash, final ClassObject sqClass) {
        super(image, hash, sqClass);
    }

    public final Object[] getPointers() {
        return pointers;
    }

    public final void setPointers(final Object[] pointers) {
        this.pointers = pointers;
    }

    public final int size() {
        return pointers.length;
    }
}
