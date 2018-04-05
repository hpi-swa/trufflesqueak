package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions;
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
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
        this(img, classObject, ArrayUtils.withAll(size, img.nil));
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
        if (!(other instanceof AbstractPointersObject)) {
            throw new PrimitiveExceptions.PrimitiveFailed();
        }
        if (!super.become(other)) {
            throw new SqueakException("Should not fail");
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        Object[] pointers2 = ((PointersObject) other).getPointers();
        ((PointersObject) other).pointers = this.getPointers();
        pointers = pointers2;
        return true;
    }

    @Override
    public void pointersBecomeOneWay(Object[] from, Object[] to, boolean copyHash) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        // TODO: super.pointersBecomeOneWay(from, to); ?
        for (int i = 0; i < from.length; i++) {
            Object fromPointer = from[i];
            for (int j = 0; j < pointers.length; j++) {
                Object newPointer = at0(j);
                if (newPointer == fromPointer) {
                    Object toPointer = to[i];
                    atput0(j, toPointer);
                    if (copyHash && fromPointer instanceof BaseSqueakObject && toPointer instanceof SqueakObject) {
                        ((SqueakObject) toPointer).setSqueakHash(((BaseSqueakObject) fromPointer).squeakHash());
                    }
                }
            }
        }
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