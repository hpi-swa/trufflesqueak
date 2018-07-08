package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.graal.squeak.image.AbstractImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.instrumentation.SqueakObjectMessageResolutionForeign;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;

public abstract class AbstractSqueakObject implements TruffleObject {
    private static final int IDENTITY_HASH_MASK = 0x400000 - 1;
    private static final byte PINNED_BIT_SHIFT = 30;

    public final SqueakImageContext image;

    @CompilationFinal private long hash;
    @CompilationFinal private ClassObject sqClass;

    protected AbstractSqueakObject(final SqueakImageContext image) {
        this(image, null);
    }

    protected AbstractSqueakObject(final SqueakImageContext image, final ClassObject klass) {
        this.image = image;
        this.hash = hashCode() & IDENTITY_HASH_MASK;
        this.sqClass = klass;
    }

    public void fillin(final AbstractImageChunk chunk) {
        setSqueakHash(chunk.getHash());
        setSqClass(chunk.getSqClass());
    }

    @Override
    public String toString() {
        return "a " + getSqClassName();
    }

    public final long squeakHash() {
        return hash;
    }

    public final void setSqueakHash(final long hash) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.hash = hash;
    }

    public final ClassObject getSqClass() {
        return sqClass;
    }

    public final void setSqClass(final ClassObject newCls) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.sqClass = newCls;
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

    public final boolean isClass() {
        assert !(this instanceof ClassObject) || (image.metaclass == getSqClass() || image.metaclass == getSqClass().getSqClass());
        return this instanceof ClassObject;
    }

    public final boolean isNil() {
        return this instanceof NilObject;
    }

    public final boolean isSpecialKindAt(final long index) {
        return getSqClass() == image.specialObjectsArray.at0(index);
    }

    public final boolean isSpecialClassAt(final long index) {
        return this == image.specialObjectsArray.at0(index);
    }

    public final boolean isSemaphore() {
        return isSpecialKindAt(SPECIAL_OBJECT_INDEX.ClassSemaphore);
    }

    public final void becomeOtherClass(final AbstractSqueakObject other) {
        final ClassObject otherSqClass = other.sqClass;
        other.setSqClass(this.sqClass);
        this.setSqClass(otherSqClass);
    }

    public final boolean isPinned() {
        return ((hash >> PINNED_BIT_SHIFT) & 1) == 1;
    }

    public final void setPinned() {
        setSqueakHash(hash | (1 << PINNED_BIT_SHIFT));
    }

    public final void unsetPinned() {
        setSqueakHash(hash & ~(1 << PINNED_BIT_SHIFT));
    }

    /*
     * Methods for Truffle.
     */

    @Override
    public final ForeignAccess getForeignAccess() {
        return SqueakObjectMessageResolutionForeign.ACCESS;
    }

    public static final boolean isInstance(final TruffleObject obj) {
        return obj instanceof AbstractSqueakObject;
    }
}
