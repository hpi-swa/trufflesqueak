/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import org.graalvm.collections.UnmodifiableEconomicMap;

import com.oracle.truffle.api.CompilerDirectives;

import de.hpi.swa.trufflesqueak.image.SqueakImageConstants.ObjectHeader;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

public abstract class AbstractSqueakObjectWithClassAndHash extends AbstractSqueakObjectWithHash {
    /**
     * Spur uses an 64-bit object header (see {@link ObjectHeader}). In TruffleSqueak, we only care
     * about the hash, the class, and a few bits (e.g., isImmutable). Instead of storing the
     * original object header, we directly reference the class, which avoids additional class table
     * lookups. The 22-bit hash is stored in an {@code int} field, the remaining 10 bits are more
     * than enough to encode additional information (e.g., marking state for {@code #allInstances}
     * et al.). The JVM and GraalVM Native Image compress pointers by default, so these two fields
     * can be represented by just one 64-bit word.
     */
    private AbstractSqueakObjectWithClassAndHash squeakClass;

    // For special/well-known objects only.
    protected AbstractSqueakObjectWithClassAndHash() {
        this(HASH_UNINITIALIZED, null);
    }

    protected AbstractSqueakObjectWithClassAndHash(final long header, final ClassObject klass) {
        super(header);
        squeakClass = klass;
    }

    protected AbstractSqueakObjectWithClassAndHash(final SqueakImageContext image, final ClassObject klass) {
        this(image.getCurrentMarkingFlag(), klass);
    }

    private AbstractSqueakObjectWithClassAndHash(final boolean markingFlag, final ClassObject klass) {
        super(markingFlag);
        squeakClass = klass;
    }

    @SuppressWarnings("this-escape")
    protected AbstractSqueakObjectWithClassAndHash(final AbstractSqueakObjectWithClassAndHash original) {
        super(original);
        assert original.assertNotForwarded() && original.squeakClass.assertNotForwarded();
        squeakClass = original.squeakClass;
        setSqueakHash(HASH_UNINITIALIZED);
    }

    @Override
    public final ClassObject getSqueakClass() {
        assert assertNotForwarded();
        return (ClassObject) squeakClass;
    }

    @Override
    public final ClassObject getSqueakClass(final SqueakImageContext image) {
        return getSqueakClass();
    }

    public final boolean needsSqueakClass() {
        return squeakClass == null;
    }

    public final String getSqueakClassName() {
        if (!isNotForwarded()) {
            return "forward to " + getForwardingPointer().getSqueakClassName();
        }
        return getSqueakClass().getClassName();
    }

    public final void setSqueakClass(final ClassObject newClass) {
        assert assertNotForwarded();
        squeakClass = newClass;
    }

    public final void becomeOtherClass(final AbstractSqueakObjectWithClassAndHash other) {
        final ClassObject otherSqClass = other.getSqueakClass();
        other.setSqueakClass(getSqueakClass());
        setSqueakClass(otherSqClass);
    }

    public final boolean hasFormatOf(final ClassObject other) {
        return getSqueakClass().getFormat() == other.getFormat();
    }

    public void forwardTo(final AbstractSqueakObjectWithClassAndHash pointer) {
        assert this != pointer && pointer.isNotForwarded() : "Forwarding pointer should neither be the same nor a forwarded object itself (" + this + "->" + pointer + ")";
        setForwardedBit();
        squeakClass = pointer;
    }

    @Override
    public final AbstractSqueakObjectWithHash resolveForwardingPointer() {
        if (isNotForwarded()) {
            return this;
        } else {
            CompilerDirectives.transferToInterpreter();
            assert squeakClass.isNotForwarded() : "Forwarding pointer should not be a forwarded object (" + MiscUtils.toObjectString(this) + "->" + MiscUtils.toObjectString(squeakClass) + ")";
            return squeakClass;
        }
    }

    @Override
    public final AbstractSqueakObjectWithClassAndHash getForwardingPointer() {
        assert !isNotForwarded();
        return squeakClass;
    }

    public static final Object resolveForwardingPointer(final Object pointer) {
        return pointer instanceof final AbstractSqueakObjectWithClassAndHash p ? p.resolveForwardingPointer() : pointer;
    }

    @Override
    public void pointersBecomeOneWay(final UnmodifiableEconomicMap<Object, Object> fromToMap) {
        final Object replacement = fromToMap.get(getSqueakClass());
        if (replacement != null) {
            setSqueakClass((ClassObject) replacement);
        }
    }
}
