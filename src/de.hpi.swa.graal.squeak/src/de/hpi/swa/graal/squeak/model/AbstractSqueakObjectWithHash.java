package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.reading.SqueakImageChunk;

public abstract class AbstractSqueakObjectWithHash extends AbstractSqueakObject {
    public static final int IDENTITY_HASH_MASK = 0x400000 - 1;
    private static final long HASH_UNINITIALIZED = -1;
    private static final int PINNED_BIT_SHIFT = 30;
    private static final int PINNED_BIT_MASK = 1 << PINNED_BIT_SHIFT;

    private long squeakHash;
    public final SqueakImageContext image;

    // For special/well-known objects only.
    protected AbstractSqueakObjectWithHash(final SqueakImageContext image) {
        this.image = image;
        squeakHash = HASH_UNINITIALIZED;
    }

    protected AbstractSqueakObjectWithHash(final SqueakImageContext image, final long hash) {
        this.image = image;
        // TODO: Generate new hash if `0`. This might have something to do with compact classes?
        squeakHash = hash != 0 ? hash : HASH_UNINITIALIZED;
    }

    public abstract ClassObject getSqueakClass();

    public final boolean needsSqueakClass() {
        return getSqueakClass() == null;
    }

    public void setSqueakClass(@SuppressWarnings("unused") final ClassObject classObject) {
        // Do nothing by default.
    }

    public abstract void fillin(SqueakImageChunk chunk);

    public final long getSqueakHash() {
        if (needsSqueakHash()) {
            /** Lazily initialize squeakHash and derive value from hashCode. */
            squeakHash = hashCode() & IDENTITY_HASH_MASK;
        }
        return squeakHash;
    }

    public final boolean needsSqueakHash() {
        return squeakHash == HASH_UNINITIALIZED;
    }

    public final boolean isPinned() {
        return (squeakHash >> PINNED_BIT_SHIFT & 1) == 1;
    }

    public String getClassName() {
        return "???NotAClass";
    }

    public final void setPinned() {
        setSqueakHash(getSqueakHash() | PINNED_BIT_MASK);
    }

    public final void setSqueakHash(final long newHash) {
        squeakHash = newHash;
    }

    public final void unsetPinned() {
        setSqueakHash(getSqueakHash() & ~PINNED_BIT_MASK);
    }
}
