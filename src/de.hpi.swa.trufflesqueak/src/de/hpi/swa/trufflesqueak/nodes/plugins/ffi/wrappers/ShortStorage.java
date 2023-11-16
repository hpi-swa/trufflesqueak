package de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

@ExportLibrary(InteropLibrary.class)
public class ShortStorage extends NativeObjectStorage {
    short[] storage;

    public ShortStorage(short[] storage) {
        this.storage = storage;
    }

    @Override
    public int byteSizeOf() {
        return storage.length * Short.BYTES;
    }

    @Override
    protected void allocate() {
        nativeAddress = UnsafeUtils.allocateNativeShorts(storage);
    }

    @Override
    public void cleanup() {
        UnsafeUtils.copyNativeShortsBackAndFree(nativeAddress, storage);
    }
}
