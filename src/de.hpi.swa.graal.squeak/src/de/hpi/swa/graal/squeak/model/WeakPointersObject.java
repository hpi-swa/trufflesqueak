package de.hpi.swa.graal.squeak.model;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public final class WeakPointersObject extends AbstractPointersObject {
    public static final ReferenceQueue<Object> weakPointersQueue = new ReferenceQueue<>();

    public WeakPointersObject(final SqueakImageContext img, final long hash, final ClassObject sqClass) {
        super(img, hash, sqClass);
    }

    public WeakPointersObject(final SqueakImageContext img, final ClassObject classObject, final int size) {
        super(img, classObject);
        setPointers(ArrayUtils.withAll(size, img.nil));
    }

    private WeakPointersObject(final WeakPointersObject original) {
        super(original.image, original.getSqueakClass());
        setPointersUnsafe(original.getPointers().clone());
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "WeakPointersObject: " + getSqueakClass();
    }

    public Object at0(final long index) {
        final Object value = getPointer((int) index);
        if (value instanceof WeakReference) {
            final Object wrappedValue = ((WeakReference<?>) value).get();
            if (wrappedValue == null) {
                return image.nil;
            } else {
                return wrappedValue;
            }
        } else {
            return value;
        }
    }

    public void atput0(final long index, final Object obj) {
        assert obj != null : "`null` indicates a problem";
        if (isStoreIntoVariablePart(index, obj)) {
            setPointer((int) index, newWeakReferenceFor(obj));
        } else {
            setPointer((int) index, obj);
        }
    }

    private boolean isStoreIntoVariablePart(final long index, final Object obj) {
        return obj instanceof AbstractSqueakObject && index >= instsize();
    }

    @TruffleBoundary
    private static WeakReference<Object> newWeakReferenceFor(final Object pointer) {
        return new WeakReference<>(pointer, weakPointersQueue);
    }

    public void setWeakPointers(final Object[] pointers) {
        final int length = pointers.length;
        setPointers(new Object[length]);
        for (int i = 0; i < length; i++) {
            atput0(i, pointers[i]);
        }
    }

    public AbstractSqueakObject shallowCopy() {
        return new WeakPointersObject(this);
    }
}
