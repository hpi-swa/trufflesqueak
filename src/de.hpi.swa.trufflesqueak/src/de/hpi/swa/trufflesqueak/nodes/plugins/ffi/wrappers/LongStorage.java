package de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

@ExportLibrary(InteropLibrary.class)
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
    protected void allocate() {
        nativeAddress = UnsafeUtils.allocateNativeLongs(storage);
    }

    @Override
    public void cleanup() {
        UnsafeUtils.copyNativeLongsBackAndFree(nativeAddress, storage);
    }
}
