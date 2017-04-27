package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.Chunk;

public class PointersObject extends SqueakObject implements TruffleObject {
    private BaseSqueakObject[] pointers;

    public PointersObject() {
    }

    public PointersObject(BaseSqueakObject[] ptrs) {
        pointers = ptrs;
    }

    public PointersObject(BaseSqueakObject[] ptrs, BaseSqueakObject sqClass) {
        pointers = ptrs;
        setSqClass(sqClass);
    }

    public ForeignAccess getForeignAccess() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void fillin(Chunk chunk, SqueakImageContext img) {
        super.fillin(chunk, img);
        pointers = chunk.getPointers();
    }

    @Override
    public BaseSqueakObject at0(int i) {
        return getPointers()[i];
    }

    @Override
    public void atput0(int i, BaseSqueakObject obj) {
        getPointers()[i] = obj;
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof PointersObject) {
            if (super.become(other)) {
                BaseSqueakObject[] pointers2 = ((PointersObject) other).getPointers();
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
        return size();
    }

    public BaseSqueakObject[] getPointers() {
        return pointers;
    }
}
