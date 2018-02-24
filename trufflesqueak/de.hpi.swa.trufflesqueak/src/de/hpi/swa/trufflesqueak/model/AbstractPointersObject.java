package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

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
        Arrays.fill(pointers, img.nil); // initialize all with nil
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        super.fillin(chunk);
        pointers = chunk.getPointers();
    }

    @Override
    public Object at0(long i) {
        return pointers[(int) i];
    }

    @Override
    public void atput0(long i, Object obj) {
        assert obj != null; // null indicates a problem
        pointers[(int) i] = obj;
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
    public void pointersBecomeOneWay(Object[] from, Object[] to) {
        super.pointersBecomeOneWay(from, to);
        int index;
        Object[] newPointers = pointers.clone();
        for (int i = 0; i < from.length; i++) {
            Object fromPointer = from[i];
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
            Object toPointer = to[i];
            newPointers[index] = toPointer;
            if (fromPointer instanceof BaseSqueakObject && toPointer instanceof SqueakObject) {
                ((SqueakObject) toPointer).setSqueakHash(((BaseSqueakObject) fromPointer).squeakHash());
            }
        }
        pointers = newPointers;
    }

    @Override
    public final int size() {
        return pointers.length;
    }

    @Override
    public final int instsize() {
        return getSqClass().getBasicInstanceSize();
    }

    public Object[] getPointers() {
        return pointers;
    }
}