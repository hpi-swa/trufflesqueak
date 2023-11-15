package de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import de.hpi.swa.trufflesqueak.nodes.plugins.ffi.InterpreterProxy.PostPrimitiveCleanup;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

@ExportLibrary(InteropLibrary.class)
public abstract class NativeObjectStorage implements PostPrimitiveCleanup, TruffleObject {
    protected long nativeAddress;
    private boolean isAllocated = false;

    @ExportMessage
    public boolean isPointer() {
        return isAllocated;
    }

    @ExportMessage
    public long asPointer() {
        return nativeAddress;
    }

    @ExportMessage
    public void toNative() {
        if (isAllocated) return;
        allocate();
        isAllocated = true;
    }

    protected abstract void allocate();
}
