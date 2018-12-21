package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.interop.SqueakObjectMessageResolutionForeign;
import de.hpi.swa.graal.squeak.nodes.DispatchSendNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public abstract class AbstractSqueakObject implements TruffleObject {
    public static final int IDENTITY_HASH_MASK = 0x400000 - 1;
    private static final byte PINNED_BIT_SHIFT = 30;

    public final SqueakImageContext image;
    private long squeakHash;
    private ClassObject squeakClass;

    public static final boolean isInstance(final TruffleObject obj) {
        return obj instanceof AbstractSqueakObject;
    }

    // For special/well-known objects only.
    protected AbstractSqueakObject(final SqueakImageContext image) {
        this.image = image;
        this.squeakHash = -1;
        this.squeakClass = null;
    }

    protected AbstractSqueakObject(final SqueakImageContext image, final ClassObject klass) {
        this.image = image;
        this.squeakHash = hashCode() & IDENTITY_HASH_MASK;
        this.squeakClass = klass;
    }

    protected AbstractSqueakObject(final SqueakImageContext image, final int hash) {
        this.image = image;
        // Generate new hash if hash is `0`. This might have something to do with compact classes?
        this.squeakHash = hash != 0 ? hash : hashCode() & IDENTITY_HASH_MASK;
        this.squeakClass = null;
    }

    protected AbstractSqueakObject(final SqueakImageContext image, final long hash, final ClassObject klass) {
        this.image = image;
        this.squeakHash = hash;
        this.squeakClass = klass;
    }

    public final void becomeOtherClass(final AbstractSqueakObject other) {
        final ClassObject otherSqClass = other.squeakClass;
        other.setSqueakClass(this.squeakClass);
        this.setSqueakClass(otherSqClass);
    }

    @Override
    public final ForeignAccess getForeignAccess() {
        return SqueakObjectMessageResolutionForeign.ACCESS;
    }

    public final ClassObject getSqueakClass() {
        return squeakClass;
    }

    public final String getSqueakClassName() {
        if (isClass()) {
            return nameAsClass() + " class";
        } else {
            return getSqueakClass().nameAsClass();
        }
    }

    public final long getSqueakHash() {
        return squeakHash;
    }

    public final boolean hasSqueakClass() {
        return squeakClass != null;
    }

    public final boolean hasSqueakHash() {
        return squeakHash >= 0;
    }

    public final boolean isBitmap() {
        return getSqueakClass() == image.bitmapClass;
    }

    public final boolean isClass() {
        assert !(this instanceof ClassObject) || (image.metaclass == getSqueakClass() || image.metaclass == getSqueakClass().getSqueakClass());
        CompilerAsserts.neverPartOfCompilation();
        return this instanceof ClassObject;
    }

    public final boolean isCompiledMethodClass() {
        return this == image.compiledMethodClass;
    }

    public final boolean isMessage() {
        return getSqueakClass() == image.messageClass;
    }

    public final boolean isNil() {
        return this == image.nil;
    }

    public final boolean isPinned() {
        return ((squeakHash >> PINNED_BIT_SHIFT) & 1) == 1;
    }

    public final boolean isSemaphore() {
        return getSqueakClass() == image.semaphoreClass;
    }

    public String nameAsClass() {
        return "???NotAClass";
    }

    public final void setPinned() {
        setSqueakHash(squeakHash | (1 << PINNED_BIT_SHIFT));
    }

    public final void setSqueakClass(final ClassObject newClass) {
        squeakClass = newClass;
    }

    public final void setSqueakHash(final long newHash) {
        squeakHash = newHash;
    }

    @Override
    public String toString() {
        return "a " + getSqueakClassName();
    }

    public final void unsetPinned() {
        setSqueakHash(squeakHash & ~(1 << PINNED_BIT_SHIFT));
    }

    public final Object send(final String selector, final Object... arguments) {
        CompilerAsserts.neverPartOfCompilation("For testing or instrumentation only.");
        final CompiledMethodObject method = (CompiledMethodObject) this.getSqueakClass().lookup(selector);
        final MaterializedFrame frame = Truffle.getRuntime().createMaterializedFrame(new Object[0], method.getFrameDescriptor());
        return DispatchSendNode.create(image).executeSend(frame, method.getCompiledInSelector(), method, getSqueakClass(), ArrayUtils.copyWithFirst(arguments, this), image.nil);
    }
}
