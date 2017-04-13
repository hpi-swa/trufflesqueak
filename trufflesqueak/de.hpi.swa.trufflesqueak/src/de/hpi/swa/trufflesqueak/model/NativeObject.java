package de.hpi.swa.trufflesqueak.model;

import java.nio.ByteBuffer;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.Chunk;
import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;

public class NativeObject extends SqueakObject implements TruffleObject {
    private ByteBuffer content;
    private int size;

    @Override
    public void fillin(Chunk chunk, SqueakImageContext img) {
        super.fillin(chunk, img);
        content = ByteBuffer.allocate((int) (chunk.size() * 4));
        byte[] bytes = chunk.getBytes();
        content.put(bytes);
        size = bytes.length - chunk.getPadding();
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
    public void become(BaseSqueakObject other) throws PrimitiveFailed {
        if (other instanceof NativeObject) {
            super.become(other);

            int size2 = ((NativeObject) other).size;
            ((NativeObject) other).size = this.size;
            this.size = size2;

            ByteBuffer content2 = ((NativeObject) other).content;
            ((NativeObject) other).content = this.content;
            this.content = content2;
        }
        throw new PrimitiveFailed();
    }

    @Override
    public int size() {
        return size;
    }
}
