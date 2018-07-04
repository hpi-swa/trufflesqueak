package de.hpi.swa.graal.squeak.model;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.AbstractImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

// TODO: Validate that weak objects are working correctly
public final class WeakPointersObject extends AbstractSqueakObject {
    public static final ReferenceQueue<Object> weakPointersQueue = new ReferenceQueue<>();
    protected Object[] pointers;

    public WeakPointersObject(final SqueakImageContext img) {
        super(img);
    }

    public WeakPointersObject(final SqueakImageContext img, final ClassObject sqClass, final Object[] ptrs) {
        super(img, sqClass);
        pointers = ptrs;
        convertToWeakReferences();
    }

    public WeakPointersObject(final SqueakImageContext img, final ClassObject classObject, final int size) {
        this(img, classObject, ArrayUtils.withAll(size, img.nil));
    }

    public WeakPointersObject(final WeakPointersObject original) {
        super(original.image, original.getSqClass());
        this.pointers = original.pointers.clone();
    }

    @Override
    public String toString() {
        return "WeakPointersObject: " + getSqClass();
    }

    @Override
    public void fillin(final AbstractImageChunk chunk) {
        super.fillin(chunk);
        pointers = chunk.getPointers();
        convertToWeakReferences();
    }

    public Object at0(final long index) {
        final Object value = pointers[(int) index];
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
        assert obj != null; // null indicates a problem
        if (obj instanceof AbstractSqueakObject && index >= instsize()) {
            // store into variable part
            weakReferenceAtPut((int) index, obj);
        } else {
            pointers[(int) index] = obj;
        }
    }

    @TruffleBoundary
    private void weakReferenceAtPut(final int index, final Object pointer) {
        pointers[index] = new WeakReference<>(pointer, weakPointersQueue);
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
        return new WeakPointersObject(this);
    }

    @Override
    public boolean become(final AbstractSqueakObject other) {
        // TODO: implement or remove?
        throw new SqueakException("become not implemented for WeakPointerObjects");
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

    private void convertToWeakReferences() {
        for (int i = 0; i < pointers.length; i++) {
            final Object pointer = pointers[i];
            if (pointer instanceof AbstractSqueakObject) {
                weakReferenceAtPut(i, pointer);
            } else {
                pointers[i] = pointer;
            }
        }
    }
}
