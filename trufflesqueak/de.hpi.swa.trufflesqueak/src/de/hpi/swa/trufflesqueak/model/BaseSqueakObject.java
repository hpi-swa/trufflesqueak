package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.instrumentation.BaseSqueakObjectMessageResolutionForeign;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public abstract class BaseSqueakObject implements TruffleObject {
    private static final int IDENTITY_HASH_MASK = 0x400000 - 1;
    public final SqueakImageContext image;

    public BaseSqueakObject(SqueakImageContext img) {
        image = img;
    }

    public static boolean isInstance(TruffleObject obj) {
        return obj instanceof BaseSqueakObject;
    }

    public abstract void fillin(SqueakImageChunk chunk);

    @Override
    public String toString() {
        return "a " + getSqClassName();
    }

    public abstract ClassObject getSqClass();

    /**
     * Set the class of this object to newCls. Not possible for all objects.
     *
     * @param newCls
     */
    public void setSqClass(ClassObject newCls) {
        throw new RuntimeException("cannot do this");
    }

    public boolean isClass() {
        return false;
    }

    public String nameAsClass() {
        return "???NotAClass";
    }

    public String getSqClassName() {
        if (isClass()) {
            return nameAsClass() + " class";
        } else {
            return getSqClass().nameAsClass();
        }
    }

    /**
     * @param other The object to swap identities with
     */
    public boolean become(BaseSqueakObject other) {
        return false;
    }

    public abstract Object at0(int l);

    public abstract void atput0(int idx, Object object);

    public abstract int size();

    public abstract int instsize();

    public abstract BaseSqueakObject shallowCopy();

    public int squeakHash() {
        return hashCode() & IDENTITY_HASH_MASK;
    }

    public int varsize() {
        return size() - instsize();
    }

    public boolean isSpecialKindAt(int index) {
        return getSqClass().equals(image.specialObjectsArray.at0(index));
    }

    public boolean isSpecialClassAt(int index) {
        return this.equals(image.specialObjectsArray.at0(index));
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return BaseSqueakObjectMessageResolutionForeign.ACCESS;
    }
}
