package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.graal.squeak.SqueakImageContext;

public final class SpecialSelectorObject extends NativeObject {
    @CompilationFinal private final int numArguments;

    public SpecialSelectorObject(final SqueakImageContext img, final int numArguments) {
        super(img, null, new NativeBytesStorage(0));
        this.numArguments = numArguments;
    }

    public SpecialSelectorObject(final SqueakImageContext image) {
        this(image, 1);
    }

    public int getNumArguments() {
        return numArguments;
    }

    public void setBytes(final byte[] bytes) {
        CompilerAsserts.neverPartOfCompilation("This method is for testing purposes only");
        storage.setBytes(bytes);
    }
}
