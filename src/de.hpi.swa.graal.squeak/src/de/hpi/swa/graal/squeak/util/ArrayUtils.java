package de.hpi.swa.graal.squeak.util;

import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;

public class ArrayUtils {

    public static final Object[] allButFirst(final Object[] values) {
        return Arrays.copyOfRange(values, 1, values.length);
    }

    public static final byte[] swapOrderCopy(final byte[] bytes) {
        return swapOrderInPlace(Arrays.copyOf(bytes, bytes.length));
    }

    public static final byte[] swapOrderInPlace(final byte[] bytes) {
        for (int i = 0; i < bytes.length / 2; i++) {
            final byte b = bytes[i];
            bytes[i] = bytes[bytes.length - 1 - i];
            bytes[bytes.length - 1 - i] = b;
        }
        return bytes;
    }

    public static final Object[] withAll(final int size, final Object element) {
        final Object[] array = new Object[size];
        Arrays.fill(array, element);
        return array;
    }

    public static final Object[] fillWith(final Object[] input, final int newSize, final Object fill) {
        final int inputSize = input.length;
        if (inputSize >= newSize) {
            return input;
        } else {
            final Object[] array = Arrays.copyOf(input, newSize);
            Arrays.fill(array, inputSize, newSize, fill);
            return array;
        }
    }

    @TruffleBoundary
    public static final Object[] toArray(final List<AbstractSqueakObject> list) {
        return list.toArray(); // needs to be behind a TruffleBoundary
    }
}
