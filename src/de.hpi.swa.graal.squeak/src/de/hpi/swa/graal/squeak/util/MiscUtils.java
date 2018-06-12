package de.hpi.swa.graal.squeak.util;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public final class MiscUtils {

    private MiscUtils() {
    }

    @TruffleBoundary
    public static void systemGC() {
        System.gc();
    }

    @TruffleBoundary
    public static long runtimeFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }
}
