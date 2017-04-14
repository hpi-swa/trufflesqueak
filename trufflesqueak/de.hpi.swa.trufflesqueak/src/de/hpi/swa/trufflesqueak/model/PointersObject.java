package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.Chunk;
import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.InvalidIndex;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;

public class PointersObject extends SqueakObject implements TruffleObject {
    private BaseSqueakObject[] pointers;

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

    public BaseSqueakObject at0(int i) throws InvalidIndex {
        if (i < pointers.length) {
            return pointers[i];
        }
        throw new InvalidIndex();
    }

    public void atput0(int i, BaseSqueakObject obj) throws InvalidIndex {
        if (i < pointers.length) {
            pointers[i] = obj;
            return;
        }
        throw new InvalidIndex();
    }

    @Override
    public void become(BaseSqueakObject other) throws PrimitiveFailed {
        if (other instanceof PointersObject) {
            super.become(other);

            BaseSqueakObject[] pointers2 = ((PointersObject) other).pointers;
            ((PointersObject) other).pointers = this.pointers;
            this.pointers = pointers2;
        }
        throw new PrimitiveFailed();
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
}
