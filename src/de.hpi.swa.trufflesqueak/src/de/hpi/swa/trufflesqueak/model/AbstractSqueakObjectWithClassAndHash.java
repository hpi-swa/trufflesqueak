/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import org.graalvm.collections.UnmodifiableEconomicMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions;
import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageConstants;
import de.hpi.swa.trufflesqueak.image.SqueakImageConstants.ObjectHeader;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.interop.LookupMethodByStringNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchIndirectNaryNode.TryPrimitiveNaryNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public abstract class AbstractSqueakObjectWithClassAndHash extends AbstractSqueakObject {
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
    private int squeakHashAndBits;

    /**
     * Support for atomically accessing the flags contained within squeakHashAndBits.
     */
    private static final VarHandle HASH_AND_BITS_HANDLE;

    static {
        try {
            HASH_AND_BITS_HANDLE = MethodHandles.lookup().findVarHandle(AbstractSqueakObjectWithClassAndHash.class, "squeakHashAndBits", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw CompilerDirectives.shouldNotReachHere("Unable to find a VarHandle for squeakHashAndBits", e);
        }
    }

    // For special/well-known objects only.
    protected AbstractSqueakObjectWithClassAndHash() {
        this(HASH_UNINITIALIZED, null);
    }

    protected AbstractSqueakObjectWithClassAndHash(final long header, final ClassObject klass) {
        squeakHashAndBits = ObjectHeader.getHash(header);
        squeakClass = klass;
        // mark bit zero when loading image
    }

    protected AbstractSqueakObjectWithClassAndHash(final SqueakImageContext image, final ClassObject klass) {
        this(image.getCurrentMarkingFlag(), klass);
    }

    private AbstractSqueakObjectWithClassAndHash(final boolean markingFlag, final ClassObject klass) {
        squeakHashAndBits = AbstractSqueakObjectWithClassAndHash.HASH_UNINITIALIZED;
        squeakClass = klass;
        if (markingFlag) {
            squeakHashAndBits |= MARK_BIT; // set MARK_BIT
        }
    }

    protected AbstractSqueakObjectWithClassAndHash(final AbstractSqueakObjectWithClassAndHash original) {
        squeakHashAndBits = original.squeakHashAndBits;
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
        return getOrCreateSqueakHash(InlinedBranchProfile.getUncached(), null);
    }

    public final long getOrCreateSqueakHash(final InlinedBranchProfile needsHashProfile, final Node node) {
        if (needsSqueakHash()) {
            /* Lazily initialize squeakHash and derive value from hashCode. */
            needsHashProfile.enter(node);
            setSqueakHash(System.identityHashCode(this) % SqueakImageConstants.IDENTITY_HASH_HALF_WORD_MASK);
        }
        return getSqueakHash();
    }

    public long getSqueakHash() {
        return squeakHashAndBits & SqueakImageConstants.IDENTITY_HASH_HALF_WORD_MASK;
    }

    public final boolean needsSqueakHash() {
        return getSqueakHash() == HASH_UNINITIALIZED;
    }

    public final void setSqueakHash(final int newHash) {
        assert newHash <= SqueakImageConstants.IDENTITY_HASH_HALF_WORD_MASK;
        squeakHashAndBits = (squeakHashAndBits & ~SqueakImageConstants.IDENTITY_HASH_HALF_WORD_MASK) + newHash;
    }

    /**
     * @return <tt>true</tt> if marked with <tt>currentMarkingFlag</tt>, <tt>false</tt> otherwise;
     *         thread safe
     */
    public final boolean isMarkedWith(final boolean currentMarkingFlag) {
        return (((int) HASH_AND_BITS_HANDLE.getAcquire(this) & MARK_BIT) != 0) == currentMarkingFlag;
    }

    /**
     * Mark this object; thread safe.
     *
     * @return <tt>false</tt> if already marked, <tt>true</tt> otherwise
     */
    public final boolean tryToMarkWith(final boolean currentMarkingFlag) {
        int oldValue, newValue;
        /*
         * Atomically set the new value with release semantics. If another thread modified 'flags'
         * since our getAcquire, CAS will fail and we retry.
         */
        do {
            oldValue = (int) HASH_AND_BITS_HANDLE.getAcquire(this);
            if (((oldValue & MARK_BIT) != 0) == currentMarkingFlag) {
                return false; // Already marked
            }
            newValue = oldValue ^ MARK_BIT; // Flip MARK_BIT
        } while (!HASH_AND_BITS_HANDLE.compareAndSet(this, oldValue, newValue));
        return true; // Successfully marked
    }

    /**
     * Unmark this object; thread safe.
     */
    public final void unmarkWith(final boolean currentMarkingFlag) {
        tryToMarkWith(!currentMarkingFlag);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "a " + getSqueakClassName() + " @" + Integer.toHexString(hashCode());
    }

    @TruffleBoundary
    public final Object send(final SqueakImageContext image, final String selector, final Object... arguments) {
        final Object methodObject = LookupMethodByStringNode.executeUncached(getSqueakClass(), selector);
        if (methodObject instanceof final CompiledCodeObject method) {
            final boolean wasActive = image.interrupt.isActive();
            image.interrupt.deactivate();
            try {
                final Object result = TryPrimitiveNaryNode.executeUncached(image.externalSenderFrame, method, this, arguments);
                if (result != null) {
                    return result;
                } else {
                    return IndirectCallNode.getUncached().call(method.getCallTarget(), FrameAccess.newWith(NilObject.SINGLETON, null, this, arguments));
                }
            } finally {
                if (wasActive) {
                    image.interrupt.activate();
                }
            }
        } else {
            throw SqueakExceptions.SqueakException.create("CompiledMethodObject expected, got: " + methodObject);
        }
    }

    public void pointersBecomeOneWay(@SuppressWarnings("unused") final Object fromPointer, @SuppressWarnings("unused") final Object toPointer) {
        // Do nothing by default.
    }

    public void pointersBecomeOneWay(@SuppressWarnings("unused") final UnmodifiableEconomicMap<Object, Object> fromToMap) {
        // Do nothing by default.
    }

    public void tracePointers(@SuppressWarnings("unused") final ObjectTracer objectTracer) {
        // Do nothing by default.
    }

    public void trace(final SqueakImageWriter writer) {
        writer.traceIfNecessary(getSqueakClass());
    }

    public abstract void write(SqueakImageWriter writer);

    /* Returns true if more content is following. */
    protected final boolean writeHeader(final SqueakImageWriter writer) {
        return writeHeader(writer, getNumSlots(), 0);
    }

    protected final boolean writeHeader(final SqueakImageWriter writer, final int thisNumSlots, final int formatOffset) {
        long numSlots = thisNumSlots;
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
