package de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers;

import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class ShortStorage extends NativeObjectStorage {
    final short[] storage;

    public ShortStorage(final short[] storage) {
        this.storage = storage;
    }

    @Override
    public int byteSizeOf() {
        return storage.length * Short.BYTES;
    }

    @Override
    protected long allocate() {
        return UnsafeUtils.allocateNativeShorts(storage);
    }

    @Override
    public void cleanup() {
        UnsafeUtils.copyNativeShortsBackAndFree(nativeAddress, storage);
    }
}
