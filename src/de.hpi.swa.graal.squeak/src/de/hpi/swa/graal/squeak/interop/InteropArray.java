package de.hpi.swa.graal.squeak.interop;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public final class InteropArray implements TruffleObject {

    private final Object[] keys;

    public InteropArray(final Object[] keys) {
        this.keys = keys;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    protected boolean isArrayElementReadable(final long index) {
        return index >= 0 && index < keys.length;
    }

    @ExportMessage
    protected long getArraySize() {
        return keys.length;
    }

    @ExportMessage
    protected Object readArrayElement(final long index) throws InvalidArrayIndexException {
        if (!isArrayElementReadable(index)) {
            throw InvalidArrayIndexException.create(index);
        }
        return keys[(int) index];
    }
}
