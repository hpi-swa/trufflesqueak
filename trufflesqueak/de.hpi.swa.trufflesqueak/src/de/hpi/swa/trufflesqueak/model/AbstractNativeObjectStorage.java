package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

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
    public void setByte(int index, byte value) {
        throw new SqueakException("Needs to be overidden by subclass");
    }

    @SuppressWarnings("unused")
    public int getInt(int index) {
        throw new SqueakException("Needs to be overidden by subclass");
    }

    @SuppressWarnings("unused")
    public void setInt(int index, int value) {
        throw new SqueakException("Needs to be overidden by subclass");
    }
}
