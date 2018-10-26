package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public final class PointersObject extends AbstractPointersObject {

    public PointersObject(final SqueakImageContext image) {
        super(image); // for special PointersObjects only
    }

    public PointersObject(final SqueakImageContext image, final long hash, final ClassObject klass) {
        super(image, hash, klass);
    }

    public PointersObject(final SqueakImageContext image, final ClassObject sqClass, final Object[] pointers) {
        super(image, sqClass);
        setPointersUnsafe(pointers);
    }

    public PointersObject(final SqueakImageContext image, final ClassObject classObject, final int size) {
        this(image, classObject, ArrayUtils.withAll(size, image.nil));
    }

    public Object at0(final long i) {
        return getPointer((int) i);
    }

    public void atput0(final long i, final Object obj) {
        assert obj != null; // null indicates a problem
        setPointer((int) i, obj);
    }

    public void become(final PointersObject other) {
        becomeOtherClass(other);
        final Object[] otherPointers = other.getPointers();
        other.setPointers(this.getPointers());
        setPointers(otherPointers);
    }

    public int instsize() {
        return getSqueakClass().getBasicInstanceSize();
    }

    public PointersObject shallowCopy() {
        return new PointersObject(image, getSqueakClass(), getPointers().clone());
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
