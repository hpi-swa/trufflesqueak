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
    public boolean isClass() {
        return image.metaclass == getSqClass().getSqClass();
    }

    @Override
    public BaseSqueakObject at0(int i) {
        return pointers[i];
    }

    @Override
    public void atput0(int i, BaseSqueakObject obj) {
        pointers[i] = obj;
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof PointersObject) {
            if (super.become(other)) {
                BaseSqueakObject[] pointers2 = ((PointersObject) other).pointers;
                ((PointersObject) other).pointers = this.pointers;
                this.pointers = pointers2;
                return true;
            }
        }
        return false;
    }

    @Override
    public String nameAsClass() {
        if (isClass() && size() > 6) {
            BaseSqueakObject nameObj = pointers[6];
            if (nameObj instanceof NativeObject) {
                return nameObj.toString();
            }
        }
        return "UnknownClass";
    }

    @Override
    public int size() {
        return pointers.length;
    }

    @Override
    public int instsize() {
        return size();
    }
}
