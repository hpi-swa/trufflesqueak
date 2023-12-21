package de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers;

import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public class ByteStorage extends NativeObjectStorage {
    byte[] storage;

    public ByteStorage(final byte[] storage) {
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
