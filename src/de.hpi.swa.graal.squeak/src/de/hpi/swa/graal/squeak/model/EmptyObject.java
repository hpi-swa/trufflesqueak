package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions;
import de.hpi.swa.graal.squeak.image.SqueakImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class EmptyObject extends AbstractSqueakObject {
    public EmptyObject(final SqueakImageContext img) {
        super(img);
    }

    public EmptyObject(final SqueakImageContext image, final ClassObject classObject) {
        super(image, classObject);
    }

    @Override
    public boolean become(final AbstractSqueakObject other) {
        if (!(other instanceof EmptyObject)) {
            throw new PrimitiveExceptions.PrimitiveFailed();
        }
        return super.become(other);
    }

    public AbstractSqueakObject shallowCopy() {
        return new EmptyObject(image, getSqClass());
    }

    public void fillin(final SqueakImageChunk chunk) {
        super.fillinHashAndClass(chunk);
    }
}
