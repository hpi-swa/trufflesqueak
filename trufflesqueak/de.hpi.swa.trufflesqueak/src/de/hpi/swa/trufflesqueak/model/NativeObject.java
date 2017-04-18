package de.hpi.swa.trufflesqueak.model;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.InvalidIndex;
import de.hpi.swa.trufflesqueak.util.Chunk;

public class NativeObject extends SqueakObject implements TruffleObject {
    private ByteBuffer content;
    private int size;
    private byte elementSize;

    @Override
    public void fillin(Chunk chunk, SqueakImageContext img) {
        super.fillin(chunk, img);
        content = ByteBuffer.allocate((int) (chunk.size() * 4));
        byte[] bytes = chunk.getBytes();
        content.put(bytes);
        size = bytes.length - chunk.getPadding();
        elementSize = chunk.getElementSize();
    }

    @Override
    public String toString() {
        return new String(Arrays.copyOfRange(content.array(), 0, size));
    }

    public ForeignAccess getForeignAccess() {
        // TODO Auto-generated method stub
        return null;
    }

    public int at0(int index) throws InvalidIndex {
        if (index * elementSize >= content.capacity()) {
            throw new InvalidIndex();
        }
        if (elementSize == 1) {
            return content.get(index);
        } else {
            assert elementSize == 4;
            return content.asIntBuffer().get(index);
        }
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof NativeObject) {
            if (super.become(other)) {

                int size2 = ((NativeObject) other).size;
                ((NativeObject) other).size = this.size;
                this.size = size2;

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
        return size;
    }
}
