package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.SqueakImageContext;

public class ListObject extends AbstractPointersObject {
    public ListObject(final SqueakImageContext img) {
        super(img);
    }

    public ListObject(final SqueakImageContext image, final ClassObject sqClass) {
        super(image, sqClass);
    }

    public ListObject(final SqueakImageContext image, final ClassObject sqClass, final Object[] objects) {
        super(image, sqClass, objects);
    }

    public ListObject(final SqueakImageContext image, final ClassObject sqClass, final int size) {
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
        final Object[] result = new Object[1 + size()];
        result[0] = firstValue;
        for (int i = 1; i < result.length; i++) {
            result[i] = at0(i - 1);
        }
        return result;
    }
}
