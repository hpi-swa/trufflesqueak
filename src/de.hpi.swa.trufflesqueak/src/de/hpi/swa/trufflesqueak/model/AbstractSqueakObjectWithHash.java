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
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public abstract class AbstractSqueakObjectWithHash extends AbstractSqueakObject {
    private static final int MARK_BIT = 1 << 24;
    private static final int FORWARDED_BIT = 1 << 25;
    /* Generate new hash if hash is 0 (see SpurMemoryManager>>#hashBitsOf:). */
    protected static final int HASH_UNINITIALIZED = 0;

    private int squeakHashAndBits;

    /**
     * Support for atomically accessing the flags contained within squeakHashAndBits.
     */
    private static final VarHandle HASH_AND_BITS_HANDLE;

    static {
        try {
            HASH_AND_BITS_HANDLE = MethodHandles.lookup().findVarHandle(AbstractSqueakObjectWithHash.class, "squeakHashAndBits", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw CompilerDirectives.shouldNotReachHere("Unable to find a VarHandle for squeakHashAndBits", e);
        }
    }

    protected AbstractSqueakObjectWithHash(final long header) {
        // mark bit zero when loading image
        squeakHashAndBits = ObjectHeader.getHash(header);
    }

    protected AbstractSqueakObjectWithHash(final SqueakImageContext image) {
        this(image.getCurrentMarkingFlag());
    }

    protected AbstractSqueakObjectWithHash(final boolean markingFlag) {
        squeakHashAndBits = AbstractSqueakObjectWithHash.HASH_UNINITIALIZED;
        if (markingFlag) {
            squeakHashAndBits |= MARK_BIT; // set MARK_BIT
        }
    }

    @SuppressWarnings("this-escape")
    protected AbstractSqueakObjectWithHash(final AbstractSqueakObjectWithHash original) {
        squeakHashAndBits = original.squeakHashAndBits;
        setSqueakHash(HASH_UNINITIALIZED);
    }

    @Override
    public int getNumSlots() {
        CompilerAsserts.neverPartOfCompilation();
        return size();
    }

    public abstract ClassObject getSqueakClass();

    /**
     * Helper for accessing the class of an object from other threads (for example in
     * {@link de.hpi.swa.trufflesqueak.util.ObjectGraphUtils}). Those threads cannot access the
     * current context via {@link SqueakImageContext#get(Node)}.
     */
    public abstract ClassObject getSqueakClass(SqueakImageContext image);

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
        return getSqueakHashInt();
    }

    public final int getSqueakHashInt() {
        assert assertNotForwarded();
        return squeakHashAndBits & SqueakImageConstants.IDENTITY_HASH_HALF_WORD_MASK;
    }

    public final boolean needsSqueakHash() {
        return getSqueakHashInt() == HASH_UNINITIALIZED;
    }

    @SuppressWarnings("this-escape")
    public final void setSqueakHash(final int newHash) {
        assert assertNotForwarded();
        assert (newHash & SqueakImageConstants.IDENTITY_HASH_HALF_WORD_MASK) == newHash : "Invalid hash: " + newHash;
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

    protected final void setForwardedBit() {
        squeakHashAndBits |= FORWARDED_BIT;
    }

    public final boolean isNotForwarded() {
        return (squeakHashAndBits & FORWARDED_BIT) == 0;
    }

    @SuppressWarnings("this-escape")
    public final boolean assertNotForwarded() {
        assert isNotForwarded() : MiscUtils.toObjectString(this) + " was unexpectedly forwarded to " + MiscUtils.toObjectString(getForwardingPointer());
        return true;
    }

    public static final boolean assertNotForwarded(final Object object) {
        return !(object instanceof final AbstractSqueakObjectWithClassAndHash o) || o.assertNotForwarded();
    }

    protected abstract AbstractSqueakObjectWithHash getForwardingPointer();

    public abstract AbstractSqueakObjectWithHash resolveForwardingPointer();

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "a " + getSqueakClass().getClassName() + " @" + Integer.toHexString(hashCode());
    }

    @TruffleBoundary
    public final Object send(final SqueakImageContext image, final String selector, final Object... arguments) {
        final Object methodObject = LookupMethodByStringNode.executeUncached(getSqueakClass(), selector);
        if (methodObject instanceof final CompiledCodeObject method) {
            final boolean wasActive = image.interrupt.deactivate();
            try {
                final Object result = TryPrimitiveNaryNode.executeUncached(image.externalSenderFrame, method, this, arguments);
                if (result != null) {
                    return result;
                } else {
                    return IndirectCallNode.getUncached().call(method.getCallTarget(), FrameAccess.newWith(NilObject.SINGLETON, null, this, arguments));
                }
            } finally {
                image.interrupt.reactivate(wasActive);
            }
        } else {
            throw SqueakExceptions.SqueakException.create("CompiledMethodObject expected, got: " + methodObject);
        }
    }

    public abstract void pointersBecomeOneWay(UnmodifiableEconomicMap<Object, Object> fromToMap);

    public void tracePointers(final ObjectTracer objectTracer) {
        objectTracer.addIfUnmarked(getSqueakClass(objectTracer.image));
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
