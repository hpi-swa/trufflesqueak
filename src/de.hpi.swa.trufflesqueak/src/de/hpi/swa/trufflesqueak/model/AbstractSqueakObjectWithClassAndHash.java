/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
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
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.interop.LookupMethodByStringNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchUneagerlyNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public abstract class AbstractSqueakObjectWithClassAndHash extends AbstractSqueakObject {
    public static final int IDENTITY_HASH_MASK = 0x400000 - 1;
    /* Generate new hash if hash is 0 (see SpurMemoryManager>>#hashBitsOf:). */
    public static final long HASH_UNINITIALIZED = 0;

    private long squeakHash;
    private ClassObject squeakClass;
    private boolean markingFlag;

    // For special/well-known objects only.
    protected AbstractSqueakObjectWithClassAndHash(final SqueakImageContext image) {
        squeakHash = HASH_UNINITIALIZED;
        markingFlag = image.getCurrentMarkingFlag();
    }

    protected AbstractSqueakObjectWithClassAndHash(final SqueakImageContext image, final long hash, final ClassObject klass) {
        this(image.getCurrentMarkingFlag(), hash, klass);
        assert hash >= 0 : "Squeak hashes should not be negative (will mess up object headers)";
    }

    protected AbstractSqueakObjectWithClassAndHash(final SqueakImageContext image, final ClassObject klass) {
        this(image.getCurrentMarkingFlag(), HASH_UNINITIALIZED, klass);
    }

    protected AbstractSqueakObjectWithClassAndHash(final boolean markingFlag, final long hash, final ClassObject klass) {
        squeakHash = hash;
        squeakClass = klass;
        this.markingFlag = markingFlag;
    }

    protected AbstractSqueakObjectWithClassAndHash(final AbstractSqueakObjectWithClassAndHash original) {
        squeakHash = HASH_UNINITIALIZED;
        squeakClass = original.squeakClass;
        markingFlag = original.markingFlag;
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
        if (this instanceof ClassObject) {
            return getClassName() + " class";
        } else {
            return getSqueakClass().getClassName();
        }
    }

    public final void setSqueakClass(final ClassObject newClass) {
        assert !(this instanceof AbstractPointersObject) || getSqueakClass() == null ||
                        ((AbstractPointersObject) this).getLayout().getSqueakClass() == newClass : "Layout should be migrated before class change";
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

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "a " + getSqueakClassName() + " @" + Integer.toHexString(hashCode());
    }

    @TruffleBoundary
    public final Object send(final SqueakImageContext image, final String selector, final Object... arguments) {
        final Object methodObject = LookupMethodByStringNode.getUncached().executeLookup(getSqueakClass(), selector);
        if (methodObject instanceof CompiledCodeObject) {
            final boolean wasActive = image.interrupt.isActive();
            image.interrupt.deactivate();
            try {
                return DispatchUneagerlyNode.getUncached().executeDispatch((CompiledCodeObject) methodObject, ArrayUtils.copyWithFirst(arguments, this), NilObject.SINGLETON);
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
        if (markingFlag == currentMarkingFlag) {
            return false;
        } else {
            markingFlag = currentMarkingFlag;
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
