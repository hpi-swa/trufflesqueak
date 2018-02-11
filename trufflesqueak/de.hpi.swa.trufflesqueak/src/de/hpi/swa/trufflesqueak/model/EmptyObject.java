package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.SqueakImageContext;

public class EmptyObject extends SqueakObject {
    public EmptyObject(SqueakImageContext img) {
        super(img);
    }

    public EmptyObject(SqueakImageContext image, ClassObject classObject) {
        super(image, classObject);
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof EmptyObject) {
            return super.become(other);
        }
        return false;
    }

    @Override
    public final int size() {
        return 0;
    }

    @Override
    public final int instsize() {
        return 0;
    }

    @Override
    public Object at0(long idx) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public void atput0(long idx, Object obj) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new EmptyObject(image, getSqClass());
    }
}
