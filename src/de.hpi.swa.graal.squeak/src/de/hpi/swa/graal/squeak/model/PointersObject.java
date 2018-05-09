package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class PointersObject extends AbstractPointersObject {
    public PointersObject(final SqueakImageContext img) {
        super(img);
    }

    public PointersObject(final SqueakImageContext img, final ClassObject sqClass, final Object[] objects) {
        super(img, sqClass, objects);
    }

    public PointersObject(final SqueakImageContext image, final ClassObject classObject, final int size) {
        super(image, classObject, size);
    }

    public AbstractSqueakObject shallowCopy() {
        return new PointersObject(image, getSqClass(), getPointers().clone());
    }
}
