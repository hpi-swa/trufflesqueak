package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public final class PointersObject extends AbstractSqueakObject {
    protected Object[] pointers;

    public PointersObject(final SqueakImageContext img) {
        super(img, -1, null); // for special PointersObjects only
    }

    public PointersObject(final SqueakImageContext img, final long hash, final ClassObject klass) {
        super(img, hash, klass);
    }

    public PointersObject(final SqueakImageContext img, final ClassObject sqClass, final Object[] ptrs) {
        super(img, sqClass);
        pointers = ptrs;
    }

    public PointersObject(final SqueakImageContext img, final ClassObject classObject, final int size) {
        this(img, classObject, ArrayUtils.withAll(size, img.nil));
    }

    public Object at0(final long i) {
        return pointers[(int) i];
    }

    public void atput0(final long i, final Object obj) {
        assert obj != null; // null indicates a problem
        pointers[(int) i] = obj;
    }

    public void become(final PointersObject other) {
        becomeOtherClass(other);
        final Object[] otherPointers = other.pointers;
        other.pointers = this.pointers;
        pointers = otherPointers;
    }

    public int size() {
        return pointers.length;
    }

    public int instsize() {
        return getSqClass().getBasicInstanceSize();
    }

    public Object[] getPointers() {
        return pointers;
    }

    public void setPointers(final Object[] pointers) {
        this.pointers = pointers;
    }

    public AbstractSqueakObject shallowCopy() {
        return new PointersObject(image, getSqClass(), pointers.clone());
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
