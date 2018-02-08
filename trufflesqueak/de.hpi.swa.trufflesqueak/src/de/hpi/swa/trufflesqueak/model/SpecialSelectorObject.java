package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.SqueakImageContext;

public class SpecialSelectorObject extends NativeObject {
    private final int numArguments;

    public SpecialSelectorObject(SqueakImageContext img, int elementSize, int numArguments) {
        super(img, (byte) elementSize);
        this.numArguments = numArguments;
    }

    public SpecialSelectorObject(SqueakImageContext image) {
        this(image, 1, 1);
    }

    public int getNumArguments() {
        return numArguments;
    }
}
