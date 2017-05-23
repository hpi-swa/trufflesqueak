package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.interop.ForeignAccess;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.Chunk;

public abstract class AbstractPointersObject extends SqueakObject {

    protected Object[] pointers;

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

    public ForeignAccess getForeignAccess() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void fillin(Chunk chunk) {
        super.fillin(chunk);
        pointers = chunk.getPointers();
    }

    @Override
    public Object at0(int i) {
        return getPointers()[i];
    }

    @Override
    public void atput0(int i, Object obj) {
        getPointers()[i] = obj;
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
    public int size() {
        return getPointers().length;
    }

    @Override
    public int instsize() {
        return getSqClass().getBasicInstanceSize();
    }

    public Object[] getPointers() {
        return pointers;
    }

}