package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class EmptyObject extends AbstractSqueakObject {

    public EmptyObject(final SqueakImageContext image, final ClassObject classObject) {
        super(image, classObject);
    }

    public EmptyObject(final SqueakImageContext image, final long hash, final ClassObject classObject) {
        super(image, hash, classObject);
    }

    public EmptyObject(final EmptyObject original) {
        super(original.image, original.getSqueakClass());
    }

    public void become(final EmptyObject other) {
        becomeOtherClass(other);
    }

    public AbstractSqueakObject shallowCopy() {
        return new EmptyObject(this);
    }
}
