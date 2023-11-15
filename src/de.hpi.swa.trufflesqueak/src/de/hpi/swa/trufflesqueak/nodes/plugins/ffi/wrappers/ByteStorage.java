package de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import de.hpi.swa.trufflesqueak.nodes.plugins.ffi.InterpreterProxy.PostPrimitiveCleanup;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

@ExportLibrary(InteropLibrary.class)
public class ByteStorage implements PostPrimitiveCleanup, TruffleObject {
    byte[] storage;
    public long nativeAddress;

    public ByteStorage(byte[] storage) {
        this.storage = storage;
        nativeAddress = UnsafeUtils.allocateNativeBytes(storage);
    }

    @ExportMessage
    public boolean isPointer() {
        return true;
    }

    @ExportMessage
    public long asPointer() {
        return nativeAddress;
    }

    @Override
    public void cleanup() {
        UnsafeUtils.copyNativeBytesBackAndFree(nativeAddress, storage);
    }
}
