package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.graal.squeak.SqueakImageContext;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.instrumentation.BaseSqueakObjectMessageResolutionForeign;
import de.hpi.swa.graal.squeak.util.AbstractImageChunk;

public abstract class BaseSqueakObject implements TruffleObject {
    private static final int IDENTITY_HASH_MASK = 0x400000 - 1;
    public final SqueakImageContext image;

    public BaseSqueakObject(final SqueakImageContext img) {
        image = img;
    }

    public static boolean isInstance(final TruffleObject obj) {
        return obj instanceof BaseSqueakObject;
    }

    public abstract void fillin(AbstractImageChunk chunk);

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
    public void setSqClass(final ClassObject newCls) {
        throw new SqueakException("cannot do this");
    }

    public boolean isClass() {
        return false;
    }

    public String nameAsClass() {
        return "???NotAClass";
    }

    public final String getSqClassName() {
        if (isClass()) {
            return nameAsClass() + " class";
        } else {
            return getSqClass().nameAsClass();
        }
    }

    public boolean become(@SuppressWarnings("unused") final BaseSqueakObject other) {
        return false;
    }

    public abstract Object at0(long l);

    public abstract void atput0(long idx, Object object);

    public abstract int size();

    public abstract int instsize();

    public abstract BaseSqueakObject shallowCopy();

    public long squeakHash() {
        return hashCode() & IDENTITY_HASH_MASK;
    }

    public final int varsize() {
        return size() - instsize();
    }

    public boolean isNil() {
        return false;
    }

    public boolean isSpecialKindAt(final long index) {
        return getSqClass().equals(image.specialObjectsArray.at0(index));
    }

    public boolean isSpecialClassAt(final long index) {
        return this.equals(image.specialObjectsArray.at0(index));
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return BaseSqueakObjectMessageResolutionForeign.ACCESS;
    }

    @SuppressWarnings("unused")
    public void pointersBecomeOneWay(final Object[] from, final Object[] to, final boolean copyHash) {
        // do nothing by default
    }
}
