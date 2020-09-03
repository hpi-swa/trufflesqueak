/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageConstants;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public abstract class AbstractSqueakObjectWithHash extends AbstractSqueakObject {
    public static final int IDENTITY_HASH_MASK = 0x400000 - 1;
    /* Generate new hash if hash is 0 (see SpurMemoryManager>>#hashBitsOf:). */
    public static final long HASH_UNINITIALIZED = 0;

    public final SqueakImageContext image;
    private long squeakHash;
    private boolean markingFlag;

    // For special/well-known objects only.
    protected AbstractSqueakObjectWithHash(final SqueakImageContext image) {
        this.image = image;
        squeakHash = HASH_UNINITIALIZED;
        markingFlag = image.getCurrentMarkingFlag();
    }

    protected AbstractSqueakObjectWithHash(final SqueakImageContext image, final long hash) {
        this.image = image;
        assert hash >= 0 : "Squeak hashes should not be negative (will mess up object headers)";
        squeakHash = hash;
        markingFlag = image.getCurrentMarkingFlag();
    }

    protected AbstractSqueakObjectWithHash(final AbstractSqueakObjectWithHash original) {
        image = original.image;
        markingFlag = original.markingFlag;
        squeakHash = HASH_UNINITIALIZED;
    }

    @Override
    public int getNumSlots() {
        CompilerAsserts.neverPartOfCompilation();
        return size();
    }

    public abstract ClassObject getSqueakClass();

    public final boolean needsSqueakClass() {
        return getSqueakClass() == null;
    }

    public void setSqueakClass(@SuppressWarnings("unused") final ClassObject classObject) {
        // Do nothing by default.
    }

    public final boolean hasFormatOf(final ClassObject other) {
        return getSqueakClass().getFormat() == other.getFormat();
    }

    public abstract void fillin(SqueakImageChunk chunk);

    @Override
    public final long getSqueakHash() {
        if (needsSqueakHash()) {
            /** Lazily initialize squeakHash and derive value from hashCode. */
            squeakHash = hashCode() & IDENTITY_HASH_MASK;
        }
        return squeakHash;
    }

    public final long getSqueakHash(final BranchProfile needsHashProfile) {
        if (needsSqueakHash()) {
            /** Lazily initialize squeakHash and derive value from hashCode. */
            needsHashProfile.enter();
            squeakHash = hashCode() & IDENTITY_HASH_MASK;
        }
        return squeakHash;
    }

    public final boolean needsSqueakHash() {
        return squeakHash == HASH_UNINITIALIZED;
    }

    public String getClassName() {
        return "???NotAClass";
    }

    public final void setSqueakHash(final long newHash) {
        squeakHash = newHash;
    }

    public final boolean getMarkingFlag() {
        return markingFlag;
    }

    public final boolean isMarked(final boolean currentMarkingFlag) {
        return markingFlag == currentMarkingFlag;
    }

    /**
     * @return <tt>false</tt> if already marked, <tt>true</tt> otherwise
     */
    public final boolean tryToMark(final boolean currentMarkingFlag) {
        if (markingFlag == currentMarkingFlag) {
            return false;
        } else {
            markingFlag = currentMarkingFlag;
            return true;
        }
    }

    @SuppressWarnings("unused")
    public void pointersBecomeOneWay(final Object[] from, final Object[] to, final boolean copyHash) {
        // Do nothing by default.
    }

    protected static final void pointersBecomeOneWay(final Object[] target, final Object[] from, final Object[] to, final boolean copyHash) {
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            for (int j = 0; j < target.length; j++) {
                final Object newPointer = target[j];
                if (newPointer == fromPointer) {
                    final Object toPointer = to[i];
                    target[j] = toPointer;
                    copyHash(fromPointer, toPointer, copyHash);
                }
            }
        }
    }

    public static final void copyHash(final Object from, final Object to, final boolean copyHash) {
        if (copyHash && from instanceof AbstractSqueakObjectWithHash && to instanceof AbstractSqueakObjectWithHash) {
            ((AbstractSqueakObjectWithHash) to).setSqueakHash(((AbstractSqueakObjectWithHash) from).getSqueakHash());
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
        final long numSlots = getNumSlots();
        if (numSlots >= SqueakImageConstants.OVERFLOW_SLOTS) {
            writer.writeLong(numSlots | SqueakImageConstants.SLOTS_MASK);
            writer.writeObjectHeader(SqueakImageConstants.OVERFLOW_SLOTS, getSqueakHash(), getSqueakClass(), formatOffset);
        } else {
            writer.writeObjectHeader(numSlots, getSqueakHash(), getSqueakClass(), formatOffset);
        }
        if (numSlots == 0) {
            writer.writePadding(SqueakImageConstants.WORD_SIZE); /* Write alignment word. */
            return false;
        } else {
            return true;
        }
    }
}
