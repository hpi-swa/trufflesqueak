package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.trufflesqueak.SqueakImageContext;

public class SpecialSelectorObject extends BytesObject {
    private final int numArguments;

    public SpecialSelectorObject(SqueakImageContext img, int numArguments) {
        super(img);
        this.numArguments = numArguments;
    }

    public SpecialSelectorObject(SqueakImageContext image) {
        this(image, 1);
    }

    public int getNumArguments() {
        return numArguments;
    }

    public void setBytes(byte[] bytes) {
        CompilerAsserts.neverPartOfCompilation("This method is for testing purposes only");
        this.bytes = bytes;
    }
}
