package de.hpi.swa.graal.squeak.model;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.AbstractImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;

// TODO: Validate that weak objects are working correctly
public final class WeakPointersObject extends AbstractPointersObject {
    @CompilationFinal public static final ReferenceQueue<Object> weakPointersQueue = new ReferenceQueue<>();

    public WeakPointersObject(final SqueakImageContext img) {
        super(img);
    }

    public WeakPointersObject(final SqueakImageContext img, final ClassObject sqClass, final Object[] ptrs) {
        super(img, sqClass, ptrs);
        convertToWeakReferences();
    }

    public WeakPointersObject(final SqueakImageContext img, final ClassObject classObject, final int size) {
        super(img, classObject, size);
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
        convertToWeakReferences();
    }

    @Override
    public Object at0(final long i) {
        final Object value = super.at0(i);
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

    @Override
    public void atput0(final long index, final Object obj) {
        assert obj != null; // null indicates a problem
        if (obj instanceof BaseSqueakObject && index >= instsize()) { // store into variable part
            super.atput0(index, new WeakReference<>(obj, weakPointersQueue));
        } else {
            super.atput0(index, obj);
        }
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new WeakPointersObject(this);
    }

    @Override
    public boolean become(final BaseSqueakObject other) {
        // TODO: implement or remove?
        throw new SqueakException("become not implemented for WeakPointerObjects");
    }

    private void convertToWeakReferences() {
        for (int i = 0; i < pointers.length; i++) {
            final Object pointer = pointers[i];
            if (pointer instanceof BaseSqueakObject) {
                pointers[i] = new WeakReference<>(pointer, weakPointersQueue);
            } else {
                pointers[i] = pointer;
            }
        }
    }
}
