/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions;
import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageConstants;
import de.hpi.swa.trufflesqueak.image.SqueakImageConstants.ObjectHeader;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.interop.LookupMethodByStringNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchUneagerlyNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public abstract class AbstractSqueakObjectWithClassAndHash extends AbstractSqueakObject {
    public static final int SQUEAK_HASH_MASK = ObjectHeader.HASH_AND_CLASS_INDEX_SIZE - 1;
    private static final int MARK_BIT = 1 << 24;
    /* Generate new hash if hash is 0 (see SpurMemoryManager>>#hashBitsOf:). */
    private static final int HASH_UNINITIALIZED = 0;

    /**
     * Spur uses an 64-bit object header (see {@link ObjectHeader}). In TruffleSqueak, we only care
     * about the hash, the class, and a few bits (e.g., isImmutable). Instead of storing the
     * original object header, we directly reference the class, which avoids additional class table
     * lookups. The 22-bit hash is stored in an {@code int} field, the remaining 10 bits are more
     * than enough to encode additional information (e.g., marking state for {@code #allInstances}
     * et al.). The JVM and GraalVM Native Image compress pointers by default, so these two fields
     * can be represented by just one 64-bit word.
     */
    private ClassObject squeakClass;
    private int squeahHashAndBits;

    // For special/well-known objects only.
    protected AbstractSqueakObjectWithClassAndHash() {
        this(HASH_UNINITIALIZED, null);
    }

    protected AbstractSqueakObjectWithClassAndHash(final long header, final ClassObject klass) {
        squeahHashAndBits = ObjectHeader.getHash(header);
        squeakClass = klass;
        // mark bit zero when loading image
    }

    protected AbstractSqueakObjectWithClassAndHash(final SqueakImageContext image, final ClassObject klass) {
        this(image.getCurrentMarkingFlag(), HASH_UNINITIALIZED, klass);
    }

    private AbstractSqueakObjectWithClassAndHash(final boolean markingFlag, final int hash, final ClassObject klass) {
        squeahHashAndBits = hash;
        squeakClass = klass;
        if (markingFlag) {
            toggleMarkingFlag();
        }
    }

    protected AbstractSqueakObjectWithClassAndHash(final AbstractSqueakObjectWithClassAndHash original) {
        squeahHashAndBits = original.squeahHashAndBits;
        setSqueakHash(HASH_UNINITIALIZED);
        squeakClass = original.squeakClass;
    }

    @Override
    public int getNumSlots() {
        CompilerAsserts.neverPartOfCompilation();
        return size();
    }

    public final ClassObject getSqueakClass() {
        return squeakClass;
    }

    public final boolean needsSqueakClass() {
        return squeakClass == null;
    }

    public final String getSqueakClassName() {
        return getSqueakClass().getClassName();
    }

    public final void setSqueakClass(final ClassObject newClass) {
        squeakClass = newClass;
    }

    public final void becomeOtherClass(final AbstractSqueakObjectWithClassAndHash other) {
        final ClassObject otherSqClass = other.squeakClass;
        other.setSqueakClass(squeakClass);
        setSqueakClass(otherSqClass);
    }

    public final boolean hasFormatOf(final ClassObject other) {
        return getSqueakClass().getFormat() == other.getFormat();
    }

    public abstract void fillin(SqueakImageChunk chunk);

    @Override
    public final long getOrCreateSqueakHash() {
        return getOrCreateSqueakHash(BranchProfile.getUncached());
    }

    public final long getOrCreateSqueakHash(final BranchProfile needsHashProfile) {
        if (needsSqueakHash()) {
            /** Lazily initialize squeakHash and derive value from hashCode. */
            needsHashProfile.enter();
            setSqueakHash(System.identityHashCode(this) & SQUEAK_HASH_MASK);
        }
        return getSqueakHash();
    }

    public long getSqueakHash() {
        return squeahHashAndBits & SQUEAK_HASH_MASK;
    }

    public final boolean needsSqueakHash() {
        return getSqueakHash() == HASH_UNINITIALIZED;
    }

    public final void setSqueakHash(final int newHash) {
        assert newHash <= SQUEAK_HASH_MASK;
        squeahHashAndBits = (squeahHashAndBits & ~SQUEAK_HASH_MASK) + newHash;
    }

    public final boolean getMarkingFlag() {
        return (squeahHashAndBits & MARK_BIT) != 0;
    }

    private void toggleMarkingFlag() {
        squeahHashAndBits ^= MARK_BIT;
    }

    public final boolean isMarked(final boolean currentMarkingFlag) {
        return getMarkingFlag() == currentMarkingFlag;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "a " + getSqueakClassName() + " @" + Integer.toHexString(hashCode());
    }

    @TruffleBoundary
    public final Object send(final SqueakImageContext image, final String selector, final Object... arguments) {
        final Object methodObject = LookupMethodByStringNode.getUncached().executeLookup(getSqueakClass(), selector);
        if (methodObject instanceof final CompiledCodeObject method) {
            final boolean wasActive = image.interrupt.isActive();
            image.interrupt.deactivate();
            try {
                return DispatchUneagerlyNode.getUncached().executeDispatch(method, ArrayUtils.copyWithFirst(arguments, this), NilObject.SINGLETON);
            } finally {
                if (wasActive) {
                    image.interrupt.activate();
                }
            }
        } else {
            throw SqueakExceptions.SqueakException.create("CompiledMethodObject expected, got: " + methodObject);
        }
    }

    /**
     * @return <tt>false</tt> if already marked, <tt>true</tt> otherwise
     */
    public final boolean tryToMark(final boolean currentMarkingFlag) {
        if (getMarkingFlag() == currentMarkingFlag) {
            return false;
        } else {
            toggleMarkingFlag();
            return true;
        }
    }

    @SuppressWarnings("unused")
    public void pointersBecomeOneWay(final Object[] from, final Object[] to) {
        // Do nothing by default.
    }

    protected static final void pointersBecomeOneWay(final Object[] target, final Object[] from, final Object[] to) {
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            for (int j = 0; j < target.length; j++) {
                final Object newPointer = target[j];
                if (newPointer == fromPointer) {
                    final Object toPointer = to[i];
                    target[j] = toPointer;
                }
            }
        }
    }

    public void tracePointers(@SuppressWarnings("unused") final ObjectTracer objectTracer) {
        // Nothing to trace by default.
    }

    public void trace(final SqueakImageWriter writer) {
        writer.traceIfNecessary(getSqueakClass());
    }

    public abstract void write(SqueakImageWriter writer);

    /* Returns true if more content is following. */
    protected final boolean writeHeader(final SqueakImageWriter writer) {
        return writeHeader(writer, 0);
    }

    protected final boolean writeHeader(final SqueakImageWriter writer, final int formatOffset) {
        long numSlots = getNumSlots();
        if (numSlots >= SqueakImageConstants.OVERFLOW_SLOTS) {
            writer.writeLong(numSlots | SqueakImageConstants.SLOTS_MASK);
            numSlots = SqueakImageConstants.OVERFLOW_SLOTS;
        }
        writer.writeObjectHeader(numSlots, getSqueakHash(), getSqueakClass(), formatOffset);
        if (numSlots == 0) {
            writer.writePadding(SqueakImageConstants.WORD_SIZE); /* Write alignment word. */
            return false;
        } else {
            return true;
        }
    }
}
