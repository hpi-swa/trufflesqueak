package de.hpi.swa.graal.squeak.model;

import java.lang.ref.WeakReference;

import de.hpi.swa.graal.squeak.SqueakImageContext;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.util.SqueakImageChunk;

// TODO: Validate that weak objects are working correctly
public final class WeakPointersObject extends AbstractPointersObject {

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
    public void fillin(final SqueakImageChunk chunk) {
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
        if (index < instsize()) { // store into instance variable
            super.atput0(index, obj);
        } else {
            super.atput0(index, new WeakReference<>(obj));
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
                pointers[i] = new WeakReference<>(pointer);
            } else {
                pointers[i] = pointer;
            }
        }
    }
}
