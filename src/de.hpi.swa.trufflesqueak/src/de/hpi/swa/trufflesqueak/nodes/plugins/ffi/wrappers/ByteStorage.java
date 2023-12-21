package de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

@ExportLibrary(InteropLibrary.class)
public class ByteStorage extends NativeObjectStorage {
    byte[] storage;

    public ByteStorage(byte[] storage) {
        this.storage = storage;
    }

    @Override
    public int byteSizeOf() {
        return storage.length * Byte.BYTES;
    }

    @Override
    protected long allocate() {
        return UnsafeUtils.allocateNativeBytes(storage);
    }

    @Override
    public void cleanup() {
        UnsafeUtils.copyNativeBytesBackAndFree(nativeAddress, storage);
    }
}
