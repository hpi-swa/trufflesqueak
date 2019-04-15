package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

@ExportLibrary(InteropLibrary.class)
public final class NilObject extends AbstractSqueakObject {

    public NilObject(final SqueakImageContext img) {
        super(img, 1L, img.nilClass);
    }

    @Override
    public int instsize() {
        return 0;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "nil";
    }

    public AbstractSqueakObject shallowCopy() {
        return this;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isNull() {
        return true;
    }
}
