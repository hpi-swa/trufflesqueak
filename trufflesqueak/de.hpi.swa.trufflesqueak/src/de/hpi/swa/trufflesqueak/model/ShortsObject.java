package de.hpi.swa.trufflesqueak.model;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class ShortsObject extends NativeObject {
    @CompilationFinal(dimensions = 1) private short[] shorts;

    public ShortsObject(SqueakImageContext image) {
        super(image);
    }

    public ShortsObject(SqueakImageContext image, ClassObject classObject, int size) {
        super(image, classObject);
        shorts = new short[size];
    }

    public ShortsObject(SqueakImageContext image, ClassObject classObject, short[] shorts) {
        super(image, classObject);
        this.shorts = shorts;
    }

    private ShortsObject(ShortsObject original) {
        this(original.image, original.getSqClass(), Arrays.copyOf(original.shorts, original.shorts.length));
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new ShortsObject(this);
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        super.fillin(chunk);
        CompilerDirectives.transferToInterpreterAndInvalidate();
        shorts = chunk.getShorts();
    }

    @Override
    public long getNativeAt0(long longIndex) {
        return Short.toUnsignedLong(shorts[(int) longIndex]);
    }

    @Override
    public void setNativeAt0(long longIndex, long value) {
        shorts[(int) longIndex] = (short) value;
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof ShortsObject) {
            if (super.become(other)) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                ShortsObject otherShortsObject = ((ShortsObject) other);
                short[] otherShorts = otherShortsObject.shorts;
                otherShortsObject.shorts = this.shorts;
                this.shorts = otherShorts;
                return true;
            }
        }
        return false;
    }

    @Override
    public final int size() {
        return shorts.length;
    }

    @Override
    public byte[] getBytes() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(shorts.length * 4);
        ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
        shortBuffer.put(shorts);
        return byteBuffer.array();
    }

    @Override
    public byte getElementSize() {
        return 2;
    }
}
