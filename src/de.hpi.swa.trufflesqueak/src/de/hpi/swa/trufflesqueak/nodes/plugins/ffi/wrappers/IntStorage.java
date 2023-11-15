package de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

@ExportLibrary(InteropLibrary.class)
public class IntStorage extends NativeObjectStorage {
    int[] storage;

    public IntStorage(int[] storage) {
        this.storage = storage;
    }

    @Override
    protected void allocate() {
        nativeAddress = UnsafeUtils.allocateNativeInts(storage);
    }

    @Override
    public void cleanup() {
        UnsafeUtils.copyNativeIntsBackAndFree(nativeAddress, storage);
    }
}
