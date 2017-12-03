package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.SqueakImageContext;

public class ListObject extends AbstractPointersObject implements TruffleObject {
    public ListObject(SqueakImageContext img) {
        super(img);
    }

    public ListObject(SqueakImageContext img, ClassObject klass, Object[] objects) {
        super(img, klass, objects);
    }

    public ListObject(SqueakImageContext image, ClassObject classObject, int size) {
        super(image, classObject, size);
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new ListObject(image, getSqClass(), getPointers().clone());
    }

    @Override
    public String toString() {
        return String.format("ListObject: %s", getSqClass());
    }
}
