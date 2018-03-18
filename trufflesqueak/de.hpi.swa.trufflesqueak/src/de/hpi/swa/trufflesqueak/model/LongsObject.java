package de.hpi.swa.trufflesqueak.model;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions;
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class LongsObject extends NativeObject {
    @CompilationFinal(dimensions = 1) private long[] longs;

    public LongsObject(SqueakImageContext image) {
        super(image);
    }

    public LongsObject(SqueakImageContext image, ClassObject classObject, int size) {
        super(image, classObject);
        longs = new long[size];
    }

    public LongsObject(SqueakImageContext image, ClassObject classObject, long[] longs) {
        super(image, classObject);
        this.longs = longs;
    }

    private LongsObject(LongsObject original) {
        this(original.image, original.getSqClass(), Arrays.copyOf(original.longs, original.longs.length));
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new LongsObject(this);
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        super.fillin(chunk);
        CompilerDirectives.transferToInterpreterAndInvalidate();
        longs = chunk.getLongs();
    }

    @Override
    public long getNativeAt0(long longIndex) {
        return longs[(int) longIndex];
    }

    @Override
    public void setNativeAt0(long longIndex, long value) {
        longs[(int) longIndex] = (int) value;
    }

    @Override
    public long shortAt0(long index) {
        throw new SqueakException.SqueakTestException(image, "Not yet implemented"); // TODO: implement
    }

    @Override
    public void shortAtPut0(long longIndex, long value) {
        throw new SqueakException.SqueakTestException(image, "Not yet implemented"); // TODO: implement
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (!(other instanceof LongsObject)) {
            throw new PrimitiveExceptions.PrimitiveFailed();
        }
        if (!super.become(other)) {
            throw new SqueakException("Should not fail");
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        LongsObject otherLongsObject = ((LongsObject) other);
        long[] otherBytes = otherLongsObject.longs;
        otherLongsObject.longs = this.longs;
        this.longs = otherBytes;
        return true;
    }

    @Override
    public final int size() {
        return longs.length;
    }

    @Override
    public byte[] getBytes() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(longs.length * 4);
        LongBuffer longBuffer = byteBuffer.asLongBuffer();
        longBuffer.put(longs);
        return byteBuffer.array();
    }

    @Override
    public byte getElementSize() {
        return 4;
    }
}
