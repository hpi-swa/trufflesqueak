package de.hpi.swa.trufflesqueak.model;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.Chunk;

public class NativeObject extends SqueakObject implements TruffleObject {
    private ByteBuffer content;
    private int size;

    @Override
    public void fillin(Chunk chunk) {
        super.fillin(chunk);
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
}
