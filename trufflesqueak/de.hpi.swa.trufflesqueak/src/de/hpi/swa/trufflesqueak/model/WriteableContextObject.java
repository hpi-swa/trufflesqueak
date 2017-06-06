package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.SqueakImageContext;

public class WriteableContextObject extends AbstractPointersObject implements ActualContextObject {
    public WriteableContextObject(SqueakImageContext img) {
        super(img);
    }

    public WriteableContextObject(SqueakImageContext img, ReadOnlyContextObject actualContext) {
        this(img);
        int size = actualContext.size();
        pointers = new Object[size];
        for (int i = 0; i < size; i++) {
            pointers[i] = actualContext.at0(i);
        }
    }

    public void atContextPut0(int i, Object obj) {
        atput0(i, obj);
    }

    public Object getFrameMarker() {
        return this;
    }
}
