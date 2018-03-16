package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions;
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class BytesObject extends NativeObject {
    @CompilationFinal(dimensions = 1) protected byte[] bytes;
    @CompilationFinal private static final long BYTE_MAX = (long) (Math.pow(2, Byte.SIZE) - 1);

    public BytesObject(SqueakImageContext image) {
        super(image);
    }

    public BytesObject(SqueakImageContext image, ClassObject classObject) {
        super(image, classObject);
    }

    public BytesObject(SqueakImageContext image, ClassObject classObject, int size) {
        this(image, classObject);
        bytes = new byte[size];
    }

    public BytesObject(SqueakImageContext image, ClassObject classObject, byte[] bytes) {
        super(image, classObject);
        this.bytes = bytes;
    }

    protected BytesObject(BytesObject original) {
        this(original.image, original.getSqClass(), Arrays.copyOf(original.bytes, original.bytes.length));
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new BytesObject(this);
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        super.fillin(chunk);
        CompilerDirectives.transferToInterpreterAndInvalidate();
        bytes = chunk.getBytes();
    }

    @Override
    public long getNativeAt0(long longIndex) {
        return Byte.toUnsignedLong(bytes[(int) longIndex]);
    }

    @Override
    public void setNativeAt0(long longIndex, long value) {
        if (value < 0 || value > BYTE_MAX) { // check for overflow
            throw new IllegalArgumentException("Illegal value for BytesObject: " + value);
        }
        bytes[(int) longIndex] = (byte) value;
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (!(other instanceof BytesObject)) {
            throw new PrimitiveExceptions.PrimitiveFailed();
        }
        if (!super.become(other)) {
            throw new SqueakException("Should not fail");
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        BytesObject otherBytesObject = (BytesObject) other;
        byte[] otherBytes = otherBytesObject.bytes;
        otherBytesObject.bytes = this.bytes;
        this.bytes = otherBytes;
        return true;
    }

    @Override
    public final int size() {
        return bytes.length;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    public void setByte(int index, byte value) {
        bytes[index] = value;
    }

    @Override
    public byte getElementSize() {
        return 1;
    }
}
