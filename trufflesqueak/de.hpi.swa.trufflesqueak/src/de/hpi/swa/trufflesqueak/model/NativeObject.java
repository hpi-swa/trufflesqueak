package de.hpi.swa.trufflesqueak.model;

import java.nio.ByteBuffer;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.UnwrappingError;
import de.hpi.swa.trufflesqueak.util.Chunk;

public class NativeObject extends SqueakObject implements TruffleObject {
    private ByteBuffer content;
    private byte elementSize;

    public NativeObject(SqueakImageContext img) {
        super(img);
    }

    public NativeObject(SqueakImageContext image, ClassObject classObject, int size, int elementSz) {
        super(image, classObject);
        if (elementSz == 1) {
            setBytes(new String(new byte[size]));
        } else {
            assert elementSz == 4;
            setWords(new int[size]);
        }
    }

    public NativeObject(SqueakImageContext img, ClassObject klass, byte[] bytes) {
        this(img, klass, bytes.length, 1);
        content.put(bytes);
    }

    public void setBytes(String s) {
        byte[] bytes = s.getBytes();
        content = ByteBuffer.allocate(bytes.length);
        content.put(bytes);
        elementSize = 1;
    }

    public void setWords(int[] words) {
        content = ByteBuffer.allocate(words.length * 4);
        content.asIntBuffer().put(words);
        elementSize = 4;
    }

    @Override
    public void fillin(Chunk chunk) {
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

    public ForeignAccess getForeignAccess() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BaseSqueakObject at0(int index) {
        return image.wrapInt(getNativeAt0(index));
    }

    @Override
    public void atput0(int index, BaseSqueakObject object) throws UnwrappingError {
        setNativeAt0(index, (int) object.unwrapInt());
    }

    public void atput0(int index, int value) {
        setNativeAt0(index, value);
    }

    public int getNativeAt0(int index) {
        if (elementSize == 1) {
            return content.get(index);
        } else {
            assert elementSize == 4;
            return content.asIntBuffer().get(index);
        }
    }

    public void setNativeAt0(int index, int value) {
        if (elementSize == 1) {
            content.put(index, (byte) value);
        } else {
            assert elementSize == 4;
            content.asIntBuffer().put(index, value);
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
        return content.capacity() / elementSize;
    }

    @Override
    public int instsize() {
        return 0;
    }
}
