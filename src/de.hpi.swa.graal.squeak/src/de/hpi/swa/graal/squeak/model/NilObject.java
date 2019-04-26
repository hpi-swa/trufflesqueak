package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public final class NilObject implements TruffleObject {
    public static final NilObject SINGLETON = new NilObject();

    public static TruffleObject convert(final AbstractSqueakObject object) {
        return object == null ? SINGLETON : object;
    }

    public static long getSqueakHash() {
        return 1L;
    }

    public static int instsize() {
        return 0;
    }

    public static int size() {
        return 0;
    }

    public static NilObject shallowCopy() {
        return SINGLETON;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "nil";
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isNull() {
        return true;
    }
}
