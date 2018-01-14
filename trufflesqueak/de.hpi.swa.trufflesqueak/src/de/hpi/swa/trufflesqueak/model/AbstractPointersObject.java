package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public abstract class AbstractPointersObject extends SqueakObject {
    @CompilationFinal(dimensions = 1) protected Object[] pointers;

    public AbstractPointersObject(SqueakImageContext img) {
        super(img);
    }

    public AbstractPointersObject(SqueakImageContext img, ClassObject klass) {
        super(img, klass);
    }

    public AbstractPointersObject(SqueakImageContext img, ClassObject sqClass, Object[] ptrs) {
        this(img, sqClass);
        pointers = ptrs;
    }

    public AbstractPointersObject(SqueakImageContext img, ClassObject classObject, int size) {
        this(img, classObject, new Object[size]);
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        super.fillin(chunk);
        pointers = chunk.getPointers();
    }

    @Override
    public Object at0(int i) {
        Object pointer = pointers[i];
        if (pointer == null) { // null indicates uninitialized pointer
            return image.nil;
        }
        return pointer;
    }

    @Override
    public void atput0(int i, Object obj) {
        pointers[i] = obj;
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof AbstractPointersObject) {
            if (super.become(other)) {
                Object[] pointers2 = ((PointersObject) other).getPointers();
                ((PointersObject) other).pointers = this.getPointers();
                pointers = pointers2;
                return true;
            }
        }
        return false;
    }

    @Override
    public void pointersBecomeOneWay(Object[] fromPointers, Object[] toPointers) {
        int index;
        Object[] newPointers = pointers.clone();
        for (int i = 0; i < fromPointers.length; i++) {
            Object fromPointer = fromPointers[i];
            index = -1;
            for (int j = 0; j < pointers.length; j++) {
                if (pointers[j].equals(fromPointer)) {
                    index = j;
                    break;
                }
            }
            if (index < 0) {
                continue;
            }
            Object toPointer = toPointers[i];
            newPointers[index] = toPointer;
            if (fromPointer instanceof BaseSqueakObject && toPointer instanceof SqueakObject) {
                ((SqueakObject) toPointer).setSqueakHash(((BaseSqueakObject) fromPointer).squeakHash());
            }
        }
        pointers = newPointers;
    }

    @Override
    public int size() {
        return pointers.length;
    }

    @Override
    public int instsize() {
        return getSqClass().getBasicInstanceSize();
    }

    public Object[] getPointers() {
        return pointers;
    }
}