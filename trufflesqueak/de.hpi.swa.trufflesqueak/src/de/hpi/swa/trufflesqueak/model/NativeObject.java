package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.SqueakImageContext;

public abstract class NativeObject extends SqueakObject {

    public NativeObject(SqueakImageContext img) {
        super(img);
    }

    public NativeObject(SqueakImageContext image, ClassObject classObject) {
        super(image, classObject);
    }

    @TruffleBoundary
    @Override
    public String toString() {
        return new String(getBytes());
    }

    @Override
    public Object at0(long index) {
        return getNativeAt0(index);
    }

    @Override
    public void atput0(long index, Object object) {
        if (object instanceof LargeIntegerObject) {
            long longValue;
            try {
                longValue = ((LargeIntegerObject) object).reduceToLong();
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException(e.toString());
            }
            setNativeAt0(index, longValue);
        } else {
            setNativeAt0(index, (long) object);
        }
    }

    public abstract long getNativeAt0(long longIndex);

    public abstract void setNativeAt0(long longIndex, long value);

    public abstract long shortAt0(long longIndex);

    public abstract void shortAtPut0(long longIndex, long value);

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
