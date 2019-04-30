package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class EmptyObject extends AbstractSqueakObjectWithClassAndHash {

    public EmptyObject(final SqueakImageContext image, final ClassObject classObject) {
        super(image, classObject);
    }

    public EmptyObject(final SqueakImageContext image, final long hash, final ClassObject classObject) {
        super(image, hash, classObject);
    }

    private EmptyObject(final EmptyObject original) {
        super(original.image, original.getSqueakClass());
    }

    @Override
    public int instsize() {
        return 0;
    }

    @Override
    public int size() {
        return 0;
    }

    public void become(final EmptyObject other) {
        becomeOtherClass(other);
    }

    public EmptyObject shallowCopy() {
        return new EmptyObject(this);
    }
}
