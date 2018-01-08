package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;

public class WriteableContextObject extends AbstractPointersObject implements ActualContextObject {

    public WriteableContextObject(SqueakImageContext img) {
        super(img);
    }

    public WriteableContextObject(SqueakImageContext img, int size) {
        this(img);
        pointers = new Object[size];
        Arrays.fill(pointers, img.nil);
    }

    public WriteableContextObject(SqueakImageContext img, ReadOnlyContextObject actualContext) {
        this(img, actualContext.size());
        for (int i = 0; i < actualContext.size(); i++) {
            pointers[i] = actualContext.at0(i);
        }
    }

    private WriteableContextObject(WriteableContextObject original) {
        super(original.image, original.getSqClass(), original.getPointers().clone());
    }

    public void atContextPut0(int index, Object value) {
        atput0(index, value);
    }

    public Object getFrameMarker() {
        return this;
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new WriteableContextObject(this);
    }

    @Override
    public String toString() {
        return String.format("Writeable context for %s", at0(CONTEXT.METHOD));
    }
}
