package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class NilObject extends AbstractSqueakObject {

    public NilObject(final SqueakImageContext img) {
        super(img, 1L, img.nilClass);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "nil";
    }

    public AbstractSqueakObject shallowCopy() {
        return this;
    }
}
