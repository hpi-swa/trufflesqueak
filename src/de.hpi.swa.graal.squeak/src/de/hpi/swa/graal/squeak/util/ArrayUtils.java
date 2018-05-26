package de.hpi.swa.graal.squeak.util;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;

public final class ArrayUtils {
    @CompilationFinal private static final Random RANDOM = new Random();

    public static Object[] allButFirst(final Object[] values) {
        return Arrays.copyOfRange(values, 1, values.length);
    }

    @TruffleBoundary
    public static void fillRandomly(final byte[] bytes) {
        RANDOM.nextBytes(bytes);
    }

    public static Object[] fillWith(final Object[] input, final int newSize, final Object fill) {
        final int inputSize = input.length;
        if (inputSize >= newSize) {
            return input;
        } else {
            final Object[] array = Arrays.copyOf(input, newSize);
            Arrays.fill(array, inputSize, newSize, fill);
            return array;
        }
    }

    public static byte[] swapOrderCopy(final byte[] bytes) {
        return swapOrderInPlace(Arrays.copyOf(bytes, bytes.length));
    }

    public static byte[] swapOrderInPlace(final byte[] bytes) {
        for (int i = 0; i < bytes.length / 2; i++) {
            final byte b = bytes[i];
            bytes[i] = bytes[bytes.length - 1 - i];
            bytes[bytes.length - 1 - i] = b;
        }
        return bytes;
    }

    @TruffleBoundary
    public static Object[] toArray(final List<AbstractSqueakObject> list) {
        return list.toArray(); // needs to be behind a TruffleBoundary
    }

    public static Object[] withAll(final int size, final Object element) {
        final Object[] array = new Object[size];
        Arrays.fill(array, element);
        return array;
    }
}
