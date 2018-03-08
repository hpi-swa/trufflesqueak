package de.hpi.swa.trufflesqueak.model;

import java.lang.ref.WeakReference;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

// TODO: Validate that weak objects are working correctly
public class WeakPointersObject extends ListObject {

    public WeakPointersObject(SqueakImageContext img) {
        super(img);
    }

    public WeakPointersObject(SqueakImageContext img, ClassObject sqClass, Object[] ptrs) {
        super(img, sqClass, ptrs);
        convertToWeakReferences();
    }

    public WeakPointersObject(SqueakImageContext img, ClassObject classObject, int size) {
        super(img, classObject, size);
    }

    public WeakPointersObject(WeakPointersObject original) {
        super(original.image, original.getSqClass());
        this.pointers = original.pointers.clone();
    }

    @Override
    public String toString() {
        return "WeakPointersObject: " + getSqClass();
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        super.fillin(chunk);
        convertToWeakReferences();
    }

    @Override
    public Object at0(long i) {
        Object value = super.at0(i);
        if (value instanceof WeakReference) {
            Object wrappedValue = ((WeakReference<?>) value).get();
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
    public void atput0(long i, Object obj) {
        assert obj != null; // null indicates a problem
        if (obj instanceof BaseSqueakObject) {
            super.atput0(i, new WeakReference<>(obj));
        } else {
            super.atput0(i, obj);
        }
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new WeakPointersObject(this);
    }

    private void convertToWeakReferences() {
        for (int i = 0; i < pointers.length; i++) {
            Object pointer = pointers[i];
            if (pointer instanceof BaseSqueakObject) {
                pointers[i] = new WeakReference<>(pointer);
            } else {
                pointers[i] = pointer;
            }
        }
    }
}
