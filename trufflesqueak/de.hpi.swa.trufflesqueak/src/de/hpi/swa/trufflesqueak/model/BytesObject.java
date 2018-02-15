package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class BytesObject extends NativeObject {
    @CompilationFinal(dimensions = 1) protected byte[] bytes;

    public BytesObject(SqueakImageContext image) {
        super(image);
    }

    public BytesObject(SqueakImageContext image, ClassObject classObject, int size) {
        super(image, classObject);
        bytes = new byte[size];
    }

    public BytesObject(SqueakImageContext image, ClassObject classObject, byte[] bytes) {
        super(image, classObject);
        this.bytes = bytes;
    }

    private BytesObject(BytesObject original) {
        this(original.image, original.getSqClass(), original.bytes);
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
        bytes[(int) longIndex] = (byte) value;
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof BytesObject) {
            if (super.become(other)) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                byte[] otherBytes = ((BytesObject) other).bytes;
                ((BytesObject) other).bytes = this.bytes;
                this.bytes = otherBytes;
                return true;
            }
        }
        return false;
    }

    @Override
    public final int size() {
        return bytes.length;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public byte getElementSize() {
        return 1;
    }
}
