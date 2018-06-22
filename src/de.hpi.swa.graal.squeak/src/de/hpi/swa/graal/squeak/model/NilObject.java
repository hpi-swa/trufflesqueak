package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.image.SqueakImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class NilObject extends AbstractSqueakObject {

    public NilObject(final SqueakImageContext img) {
        super(img);
    }

    @Override
    public String toString() {
        return "nil";
    }

    public AbstractSqueakObject shallowCopy() {
        return this;
    }

    public void fillin(final SqueakImageChunk chunk) {
        super.fillinHashAndClass(chunk);
    }
}
