package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.SqueakImageContext;

public class ListObject extends AbstractPointersObject {
    public ListObject(SqueakImageContext img) {
        super(img);
    }

    public ListObject(SqueakImageContext image, ClassObject sqClass) {
        super(image, sqClass);
    }

    public ListObject(SqueakImageContext image, ClassObject sqClass, Object[] objects) {
        super(image, sqClass, objects);
    }

    public ListObject(SqueakImageContext image, ClassObject sqClass, int size) {
        super(image, sqClass, size);
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new ListObject(image, getSqClass(), getPointers().clone());
    }

    @Override
    public String toString() {
        return "ListObject: " + getSqClass();
    }

    public Object[] unwrappedWithFirst(final Object firstValue) {
        Object[] result = new Object[1 + size()];
        result[0] = firstValue;
        for (int i = 1; i < result.length; i++) {
            result[i] = at0(i - 1);
        }
        return result;
    }
}
