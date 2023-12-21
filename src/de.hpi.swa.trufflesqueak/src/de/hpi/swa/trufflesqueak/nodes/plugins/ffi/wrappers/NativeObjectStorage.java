package de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import de.hpi.swa.trufflesqueak.model.NativeObject;

@ExportLibrary(InteropLibrary.class)
public abstract class NativeObjectStorage implements PostPrimitiveCleanup, TruffleObject {
    protected long nativeAddress;
    private boolean isAllocated = false;

    public static NativeObjectStorage from(NativeObject object) {
        if (object.isByteType()) {
            return new ByteStorage(object.getByteStorage());
        } else if (object.isIntType()) {
            return new IntStorage(object.getIntStorage());
        } else if (object.isLongType()) {
            return new LongStorage(object.getLongStorage());
        } else if (object.isShortType()) {
            return new ShortStorage(object.getShortStorage());
        } else {
            throw new IllegalArgumentException("Object storage type is not supported.");
        }
    }

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
        if (isAllocated)
            return;
        nativeAddress = allocate();
        isAllocated = true;
    }

    public abstract int byteSizeOf();

    protected abstract long allocate();
}