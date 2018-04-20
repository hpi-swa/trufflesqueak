package de.hpi.swa.graal.squeak.util;

public final class BitSplitter {
    public static int[] splitter(final long param, final int[] lengths) {
        long integer = param;
        final int[] out = new int[lengths.length];
        for (int i = 0; i < lengths.length; i++) {
            final int length = lengths[i];
            out[i] = (int) (integer & ((1 << length) - 1));
            integer = integer >> length;
        }
        return out;
    }
}
