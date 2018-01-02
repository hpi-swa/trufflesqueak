package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

import de.hpi.swa.trufflesqueak.SqueakImageContext;

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

    public void atContextPut0(int i, Object obj) {
        atput0(i, obj);
    }

    public Object getFrameMarker() {
        return this;
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new WriteableContextObject(this);
    }
}
