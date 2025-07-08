/*
 * Copyright (c) 2023-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2023-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.trufflesqueak.model.NativeObject;

@ExportLibrary(InteropLibrary.class)
public abstract class NativeObjectStorage implements TruffleObject {
    protected long nativeAddress;
    private boolean isAllocated;

    public static NativeObjectStorage from(final NativeObject object) {
        if (object.isTruffleStringType()) {
            // TODO handle TruffleString type
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
        if (isAllocated) {
            return;
        }
        nativeAddress = allocate();
        isAllocated = true;
    }

    public abstract int byteSizeOf();

    protected abstract long allocate();

    public abstract void cleanup();
}
