package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.interop.InteropArray;
import de.hpi.swa.graal.squeak.nodes.DispatchSendNode;
import de.hpi.swa.graal.squeak.nodes.NewObjectNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

@ExportLibrary(InteropLibrary.class)
public abstract class AbstractSqueakObject implements TruffleObject {
    public static final int IDENTITY_HASH_MASK = 0x400000 - 1;
    private static final byte PINNED_BIT_SHIFT = 30;

    public final SqueakImageContext image;
    private long squeakHash;
    private ClassObject squeakClass;

    // For special/well-known objects only.
    protected AbstractSqueakObject(final SqueakImageContext image) {
        this.image = image;
        squeakHash = -1;
        squeakClass = null;
    }

    protected AbstractSqueakObject(final SqueakImageContext image, final ClassObject klass) {
        this.image = image;
        squeakHash = hashCode() & IDENTITY_HASH_MASK;
        squeakClass = klass;
    }

    protected AbstractSqueakObject(final SqueakImageContext image, final int hash) {
        this.image = image;
        // Generate new hash if hash is `0`. This might have something to do with compact classes?
        squeakHash = hash != 0 ? hash : hashCode() & IDENTITY_HASH_MASK;
        squeakClass = null;
    }

    protected AbstractSqueakObject(final SqueakImageContext image, final long hash, final ClassObject klass) {
        this.image = image;
        squeakHash = hash;
        squeakClass = klass;
    }

    public static final boolean isInstance(final TruffleObject obj) {
        return obj instanceof AbstractSqueakObject;
    }

    public final void becomeOtherClass(final AbstractSqueakObject other) {
        final ClassObject otherSqClass = other.squeakClass;
        other.setSqueakClass(squeakClass);
        setSqueakClass(otherSqClass);
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

    public abstract int instsize();

    public abstract int size();

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
        assert !(this instanceof ClassObject) || getSqueakClass().isMetaClass() || getSqueakClass().getSqueakClass().isMetaClass();
        CompilerAsserts.neverPartOfCompilation();
        return this instanceof ClassObject;
    }

    public final boolean isLargeInteger() {
        return getSqueakClass() == image.largePositiveIntegerClass || getSqueakClass() == image.largeNegativeIntegerClass;
    }

    public final boolean isMessage() {
        return getSqueakClass() == image.messageClass;
    }

    public final boolean isMetaClass() {
        return this == image.metaClass;
    }

    public final boolean isNil() {
        return this == image.nil;
    }

    public final boolean isPinned() {
        return (squeakHash >> PINNED_BIT_SHIFT & 1) == 1;
    }

    public final boolean isSemaphore() {
        return getSqueakClass() == image.semaphoreClass;
    }

    public String nameAsClass() {
        return "???NotAClass";
    }

    public final void setPinned() {
        setSqueakHash(squeakHash | 1 << PINNED_BIT_SHIFT);
    }

    public final void setSqueakClass(final ClassObject newClass) {
        squeakClass = newClass;
    }

    public final void setSqueakHash(final long newHash) {
        squeakHash = newHash;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "a " + getSqueakClassName();
    }

    public final void unsetPinned() {
        setSqueakHash(squeakHash & ~(1 << PINNED_BIT_SHIFT));
    }

    public final Object send(final String selector, final Object... arguments) {
        CompilerAsserts.neverPartOfCompilation("For testing or instrumentation only.");
        final CompiledMethodObject method = (CompiledMethodObject) getSqueakClass().lookup(selector);
        final MaterializedFrame frame = Truffle.getRuntime().createMaterializedFrame(ArrayUtils.EMPTY_ARRAY, method.getFrameDescriptor());
        return DispatchSendNode.create(image).executeSend(frame, method.getCompiledInSelector(), method, getSqueakClass(), ArrayUtils.copyWithFirst(arguments, this), image.nil);
    }

    /*
     * INTEROPERABILITY
     */

    @SuppressWarnings("static-method")
    @ExportMessage
    protected final boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    protected final long getArraySize(@Shared("sizeNode") @Cached(value = "create()", allowUncached = true) final SqueakObjectSizeNode sizeNode) {
        return sizeNode.execute(this);
    }

    @SuppressWarnings("static-method")
    @ExportMessage(name = "isArrayElementReadable")
    @ExportMessage(name = "isArrayElementModifiable")
    @ExportMessage(name = "isArrayElementInsertable")
    protected final boolean isArrayElementReadable(final long index, @Shared("sizeNode") @Cached(value = "create()", allowUncached = true) final SqueakObjectSizeNode sizeNode) {
        return 0 <= index && index < sizeNode.execute(this);
    }

    @ExportMessage
    protected final Object readArrayElement(final long index, @Cached(value = "create()", allowUncached = true) final SqueakObjectAt0Node at0Node) {
        return at0Node.execute(this, index);
    }

    @ExportMessage
    protected final void writeArrayElement(final long index, final Object value, @Cached(value = "create()", allowUncached = true) final SqueakObjectAtPut0Node atput0Node) {
        // TODO: throw InvalidArrayIndexException correctly.
        atput0Node.execute(this, index, value);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    protected final boolean hasMembers() {
        return true;
    }

    @ExportMessage
    protected final Object getMembers(@SuppressWarnings("unused") final boolean includeInternal) {
        return new InteropArray(getSqueakClass().listMethods());
    }

    @ExportMessage
    public boolean isMemberReadable(@SuppressWarnings("unused") final String key) {
        return true;
    }

    @ExportMessage
    public Object readMember(final String key) {
        return getSqueakClass().lookup(toSelector(key));
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    protected final boolean isInstantiable() {
        return this instanceof ClassObject;
    }

    @ExportMessage
    protected final Object instantiate(final Object[] arguments, @Cached(value = "create(this.image)", allowUncached = true) final NewObjectNode newObjectNode)
                    throws UnsupportedTypeException, ArityException, UnsupportedMessageException {
        if (!(this instanceof ClassObject)) {
            throw UnsupportedMessageException.create();
        }
        final int numArguments = arguments.length;
        switch (numArguments) {
            case 0:
                return newObjectNode.execute((ClassObject) this);
            case 1:
                if (arguments[0] instanceof Integer) {
                    return newObjectNode.execute((ClassObject) this, (int) arguments[0]);
                } else {
                    throw UnsupportedTypeException.create(arguments, "Second argument must be the size as an integer.");
                }
            default:
                throw ArityException.create(1, numArguments);
        }
    }

    /**
     * Converts an interop identifier to a Smalltalk selector. Most languages do not allow colons in
     * identifiers, so treat underscores as colons as well.
     *
     * @param identifier for interop
     * @return Smalltalk selector
     */
    private static String toSelector(final String identifier) {
        return identifier.replace('_', ':');
    }
}
