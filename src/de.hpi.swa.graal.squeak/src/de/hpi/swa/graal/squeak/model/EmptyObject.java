package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class EmptyObject extends AbstractSqueakObject {
    public EmptyObject(final SqueakImageContext img) {
        super(img);
    }

    public EmptyObject(final SqueakImageContext image, final ClassObject classObject) {
        super(image, classObject);
    }

    public void become(final EmptyObject other) {
        becomeOtherClass(other);
    }

    public AbstractSqueakObject shallowCopy() {
        return new EmptyObject(image, getSqClass());
    }
}
