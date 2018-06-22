package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public final class PointersObject extends AbstractSqueakObject {
    protected Object[] pointers;

    public PointersObject(final SqueakImageContext img) {
        super(img);
    }

    public PointersObject(final SqueakImageContext img, final ClassObject klass) {
        super(img, klass);
    }

    public PointersObject(final SqueakImageContext img, final ClassObject sqClass, final Object[] ptrs) {
        this(img, sqClass);
        pointers = ptrs;
    }

    public PointersObject(final SqueakImageContext img, final ClassObject classObject, final int size) {
        this(img, classObject, ArrayUtils.withAll(size, img.nil));
    }

    public void fillin(final SqueakImageChunk chunk) {
        super.fillinHashAndClass(chunk);
        pointers = chunk.getPointers();
    }

    public Object at0(final long i) {
        return pointers[(int) i];
    }

    public void atput0(final long i, final Object obj) {
        assert obj != null; // null indicates a problem
        pointers[(int) i] = obj;
    }

    @Override
    public boolean become(final AbstractSqueakObject other) {
        if (!(other instanceof PointersObject)) {
            throw new PrimitiveExceptions.PrimitiveFailed();
        }
        if (!super.become(other)) {
            throw new SqueakException("Should not fail");
        }
        final Object[] pointers2 = ((PointersObject) other).pointers;
        ((PointersObject) other).pointers = this.pointers;
        pointers = pointers2;
        return true;
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to, final boolean copyHash) {
        // TODO: super.pointersBecomeOneWay(from, to); ?
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            for (int j = 0; j < size(); j++) {
                final Object newPointer = at0(j);
                if (newPointer == fromPointer) {
                    final Object toPointer = to[i];
                    atput0(j, toPointer);
                    if (copyHash && fromPointer instanceof AbstractSqueakObject && toPointer instanceof AbstractSqueakObject) {
                        ((AbstractSqueakObject) toPointer).setSqueakHash(((AbstractSqueakObject) fromPointer).squeakHash());
                    }
                }
            }
        }
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
