package de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers;

import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public class LongStorage extends NativeObjectStorage {
    long[] storage;

    public LongStorage(long[] storage) {
        this.storage = storage;
    }

    @Override
    public int byteSizeOf() {
        return storage.length * Long.BYTES;
    }

    @Override
    protected long allocate() {
        return UnsafeUtils.allocateNativeLongs(storage);
    }

    @Override
    public void cleanup() {
        UnsafeUtils.copyNativeLongsBackAndFree(nativeAddress, storage);
    }
}
