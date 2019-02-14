package de.hpi.swa.graal.squeak.util;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public final class MiscUtils {

    private MiscUtils() {
    }

    public static int[] bitSplitter(final long param, final int[] lengths) {
        long integer = param;
        final int[] out = new int[lengths.length];
        for (int i = 0; i < lengths.length; i++) {
            final int length = lengths[i];
            out[i] = (int) (integer & (1 << length) - 1);
            integer = integer >> length;
        }
        return out;
    }

    @TruffleBoundary
    public static String format(final String format, final Object... args) {
        return String.format(format, args);
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
