package de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers;

import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class IntStorage extends NativeObjectStorage {
    final int[] storage;

    public IntStorage(final int[] storage) {
        this.storage = storage;
    }

    @Override
    public int byteSizeOf() {
        return storage.length * Integer.BYTES;
    }

    @Override
    protected long allocate() {
        return UnsafeUtils.allocateNativeInts(storage);
    }

    @Override
    public void cleanup() {
        UnsafeUtils.copyNativeIntsBackAndFree(nativeAddress, storage);
    }
}