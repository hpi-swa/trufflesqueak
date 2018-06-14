package de.hpi.swa.graal.squeak.model;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.AbstractImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

// TODO: Validate that weak objects are working correctly
public final class WeakPointersObject extends AbstractSqueakObject {
    @CompilationFinal(dimensions = 1) protected Object[] pointers;
    @CompilationFinal public static final ReferenceQueue<Object> weakPointersQueue = new ReferenceQueue<>();

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

    public void fillin(final AbstractImageChunk chunk) {
        super.fillinHashAndClass(chunk);
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
            pointers[(int) index] = new WeakReference<>(obj, weakPointersQueue);
        } else {
            pointers[(int) index] = obj;
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
        return new WeakPointersObject(this);
    }

    @Override
    public boolean become(final AbstractSqueakObject other) {
        // TODO: implement or remove?
        throw new SqueakException("become not implemented for WeakPointerObjects");
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to, final boolean copyHash) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
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
                pointers[i] = new WeakReference<>(pointer, weakPointersQueue);
            } else {
                pointers[i] = pointer;
            }
        }
    }

}
