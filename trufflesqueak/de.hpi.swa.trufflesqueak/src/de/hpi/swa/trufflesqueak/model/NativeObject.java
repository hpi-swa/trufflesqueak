package de.hpi.swa.trufflesqueak.model;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class NativeObject extends SqueakObject {
    private ByteBuffer content;
    private byte elementSize;

    public NativeObject(SqueakImageContext img, byte elementSize) {
        super(img);
        this.elementSize = elementSize;
    }

    public NativeObject(SqueakImageContext image, ClassObject classObject, int size, int elementSize) {
        super(image, classObject);
        if (elementSize == 1) {
            setBytes(new byte[size]);
        } else {
            assert elementSize == 4;
            setWords(new int[size]);
        }
    }

    private NativeObject(NativeObject original) {
        this(original.image, original.elementSize);
        setSqClass(original.getSqClass());
        setBytes(original.getBytes());
        elementSize = original.elementSize;
    }

    public NativeObject(SqueakImageContext img, ClassObject klass, byte[] bytes) {
        this(img, klass, bytes.length, 1);
        content.rewind();
        content.put(bytes);
    }

    public void setBytes(byte[] bytes) {
        content = ByteBuffer.allocate(bytes.length);
        content.order(ByteOrder.nativeOrder());
        content.put(bytes);
        elementSize = 1;
    }

    public void setWords(int[] words) {
        content = ByteBuffer.allocate(words.length * 4);
        content.asIntBuffer().put(words);
        elementSize = 4;
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        super.fillin(chunk);
        byte[] bytes = chunk.getBytes();
        content = ByteBuffer.allocate(bytes.length);
        content.put(bytes);
        elementSize = chunk.getElementSize();
    }

    @Override
    public String toString() {
        return new String(content.array());
    }

    @Override
    public Object at0(long index) {
        return getNativeAt0(index);
    }

    @Override
    public void atput0(long index, Object object) {
        setNativeAt0(index, (long) object);
    }

    @TruffleBoundary
    public final long getNativeAt0(long longIndex) {
        int index = (int) longIndex;
        switch (elementSize) {
            case 1:
                return content.get(index) & 0xFF;
            case 2:
                return content.asShortBuffer().get(index) & 0xFFFF;
            case 4:
                return content.asIntBuffer().get(index) & 0xFFFFFFFF;
            case 8:
                return content.asLongBuffer().get(index);
            default:
                throw new SqueakException("invalid native object size");
        }
    }

    @TruffleBoundary
    public final void setNativeAt0(long longIndex, long value) {
        int index = (int) longIndex;
        switch (elementSize) {
            case 1:
                content.put(index, (byte) value);
                break;
            case 2:
                content.asShortBuffer().put(index, (short) value);
                break;
            case 4:
                content.asIntBuffer().put(index, ((Long) value).intValue());
                break;
            case 8:
                content.asLongBuffer().put(index, value);
                break;
            default:
                throw new SqueakException("invalid native object size");
        }
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof NativeObject) {
            if (super.become(other)) {
                ByteBuffer content2 = ((NativeObject) other).content;
                ((NativeObject) other).content = this.content;
                this.content = content2;
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return content.limit() / elementSize;
    }

    @Override
    public int instsize() {
        return 0;
    }

    public byte[] getBytes() {
        return content.array();
    }

    public CharBuffer getCharBuffer() {
        return content.asCharBuffer();
    }

    public byte getElementSize() {
        return elementSize;
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new NativeObject(this);
    }

    public BigInteger normalize() {
        return new LargeIntegerObject(image, getSqClass(), getBytes()).getValue();
    }
}
