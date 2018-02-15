package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.SqueakImageContext;

public abstract class NativeObject extends SqueakObject {

    public NativeObject(SqueakImageContext img) {
        super(img);
    }

    public NativeObject(SqueakImageContext image, ClassObject classObject) {
        super(image, classObject);
    }

    @Override
    public final String toString() {
        return new String(getBytes());
    }

    @Override
    public Object at0(long index) {
        return getNativeAt0(index);
    }

    @Override
    public void atput0(long index, Object object) {
        setNativeAt0(index, (long) object);
    }

    public abstract long getNativeAt0(long longIndex);

    public abstract void setNativeAt0(long longIndex, long value);

    @Override
    public final int instsize() {
        return 0;
    }

    public abstract byte[] getBytes();

    public abstract byte getElementSize();

    public LargeIntegerObject normalize() {
        return new LargeIntegerObject(image, getSqClass(), getBytes());
    }
}
