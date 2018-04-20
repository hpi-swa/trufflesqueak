package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.util.SqueakImageChunk;

public abstract class AbstractNativeObjectStorage {

    public abstract void fillin(SqueakImageChunk chunk);

    public abstract long getNativeAt0(long longIndex);

    public abstract void setNativeAt0(long longIndex, long value);

    public abstract long shortAt0(long longIndex);

    public abstract void shortAtPut0(long longIndex, long value);

    public abstract byte[] getBytes();

    public abstract void setBytes(byte[] bytes);

    public int[] getWords() {
        throw new SqueakException("Needs to be overidden by subclass");
    }

    public abstract void fillWith(Object value);

    public abstract int size();

    public abstract byte getElementSize();

    public abstract AbstractNativeObjectStorage shallowCopy();

    @SuppressWarnings("unused")
    public void setByte(final int index, final byte value) {
        throw new SqueakException("Needs to be overidden by subclass");
    }

    @SuppressWarnings("unused")
    public int getInt(final int index) {
        throw new SqueakException("Needs to be overidden by subclass");
    }

    @SuppressWarnings("unused")
    public void setInt(final int index, final int value) {
        throw new SqueakException("Needs to be overidden by subclass");
    }
}
