package de.hpi.swa.trufflesqueak.util;

public class BitSplitter {
    public static int[] splitter(long param, int[] lengths) {
        long integer = param;
        int[] out = new int[lengths.length];
        for (int i = 0; i < lengths.length; i++) {
            int length = lengths[i];
            out[i] = (int) (integer & ((1 << length) - 1));
            integer = integer >> length;
        }
        return out;
    }
}
