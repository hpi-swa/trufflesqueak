package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class NativeObject extends AbstractSqueakObject {
    public static final int SHORT_BYTE_SIZE = 2;
    public static final int INTEGER_BYTE_SIZE = 4;
    public static final int LONG_BYTE_SIZE = 8;
    public static final short BYTE_MAX = (short) (Math.pow(2, Byte.SIZE) - 1);
    public static final int SHORT_MAX = (int) (Math.pow(2, Short.SIZE) - 1);
    public static final long INTEGER_MAX = (long) (Math.pow(2, Integer.SIZE) - 1);

    @CompilationFinal private Object storage;

    public static NativeObject newNativeBytes(final SqueakImageChunk chunk) {
        return new NativeObject(chunk.image, chunk.getHash(), chunk.getSqClass(), chunk.getBytes());
    }

    public static NativeObject newNativeBytes(final SqueakImageContext img, final ClassObject klass, final int size) {
        return newNativeBytes(img, klass, new byte[size]);
    }

    public static NativeObject newNativeBytes(final SqueakImageContext img, final ClassObject klass, final byte[] bytes) {
        return new NativeObject(img, klass, bytes);
    }

    public static NativeObject newNativeShorts(final SqueakImageChunk chunk) {
        return new NativeObject(chunk.image, chunk.getHash(), chunk.getSqClass(), chunk.getShorts());
    }

    public static NativeObject newNativeShorts(final SqueakImageContext img, final ClassObject klass, final int size) {
        return newNativeShorts(img, klass, new short[size]);
    }

    public static NativeObject newNativeShorts(final SqueakImageContext img, final ClassObject klass, final short[] shorts) {
        return new NativeObject(img, klass, shorts);
    }

    public static NativeObject newNativeInts(final SqueakImageChunk chunk) {
        return new NativeObject(chunk.image, chunk.getHash(), chunk.getSqClass(), chunk.getWords());
    }

    public static NativeObject newNativeInts(final SqueakImageContext img, final ClassObject klass, final int size) {
        return newNativeInts(img, klass, new int[size]);
    }

    public static NativeObject newNativeInts(final SqueakImageContext img, final ClassObject klass, final int[] words) {
        return new NativeObject(img, klass, words);
    }

    public static NativeObject newNativeLongs(final SqueakImageChunk chunk) {
        return new NativeObject(chunk.image, chunk.getHash(), chunk.getSqClass(), chunk.getLongs());
    }

    public static NativeObject newNativeLongs(final SqueakImageContext img, final ClassObject klass, final int size) {
        return newNativeLongs(img, klass, new long[size]);
    }

    public static NativeObject newNativeLongs(final SqueakImageContext img, final ClassObject klass, final long[] longs) {
        return new NativeObject(img, klass, longs);
    }

    protected NativeObject(final SqueakImageContext image, final ClassObject classObject, final Object storage) {
        super(image, classObject);
        assert storage != null;
        this.storage = storage;
    }

    protected NativeObject(final SqueakImageContext image, final long hash, final ClassObject classObject, final Object storage) {
        super(image, hash, classObject);
        assert storage != null;
        this.storage = storage;
    }

    public NativeObject(final SqueakImageContext image) { // constructor for special selectors
        super(image, -1, null);
        storage = new byte[0];
    }

    @TruffleBoundary
    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation("toString should not be part of compilation");
        if (isByteType()) {
            return new String((byte[]) storage);
        } else if (isShortType()) {
            return "ShortArray(size=" + ((short[]) storage).length + ")";
        } else if (isIntType()) {
            return "IntArray(size=" + ((int[]) storage).length + ")";
        } else if (isLongType()) {
            return "LongArray(size=" + ((long[]) storage).length + ")";
        } else {
            throw new SqueakException("Unexpected native object type");
        }
    }

    @TruffleBoundary
    public String asString() {
        assert isByteType();
        return new String((byte[]) storage);
    }

    public void become(final NativeObject other) {
        super.becomeOtherClass(other);
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final Object otherStorage = other.storage;
        other.setStorage(this.storage);
        this.setStorage(otherStorage);
    }

    public LargeIntegerObject normalize() {
        return new LargeIntegerObject(image, getSqClass(), getByteStorage());
    }

    public byte[] getByteStorage() {
        assert isByteType();
        return (byte[]) storage;
    }

    public short[] getShortStorage() {
        assert isShortType();
        return (short[]) storage;
    }

    public int[] getIntStorage() {
        assert isIntType();
        return (int[]) storage;
    }

    public long[] getLongStorage() {
        assert isLongType();
        return (long[]) storage;
    }

    public boolean isByteType() {
        return storage.getClass() == byte[].class;
    }

    public boolean isShortType() {
        return storage.getClass() == short[].class;
    }

    public boolean isIntType() {
        return storage.getClass() == int[].class;
    }

    public boolean isLongType() {
        return storage.getClass() == long[].class;
    }

    public boolean haveSameStorageType(final NativeObject other) {
        return storage.getClass() == other.storage.getClass();
    }

    public void convertToBytesStorage(final byte[] bytes) {
        assert storage.getClass() != bytes.getClass() : "Converting storage of same type unnecessary";
        setStorage(bytes);
    }

    public void convertToShortsStorage(final byte[] bytes) {
        assert storage.getClass() != bytes.getClass() : "Converting storage of same type unnecessary";
        setStorage(shortsFromBytes(bytes));
    }

    public void convertToIntsStorage(final byte[] bytes) {
        assert storage.getClass() != bytes.getClass() : "Converting storage of same type unnecessary";
        setStorage(intsFromBytes(bytes));
    }

    public void convertToLongsStorage(final byte[] bytes) {
        assert storage.getClass() != bytes.getClass() : "Converting storage of same type unnecessary";
        setStorage(longsFromBytes(bytes));
    }

    public void setStorage(final Object storage) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.storage = storage;
    }

    private static short[] shortsFromBytes(final byte[] bytes) {
        final int size = bytes.length / SHORT_BYTE_SIZE;
        final short[] shorts = new short[size];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = (short) (((bytes[i] & 0xFF) << 8) | (bytes[i + 1] & 0xFF));
        }
        return shorts;
    }

    private static int[] intsFromBytes(final byte[] bytes) {
        final int size = bytes.length / INTEGER_BYTE_SIZE;
        final int[] ints = new int[size];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = ((bytes[i]) << 24) | ((bytes[i + 1]) << 16) | ((bytes[i + 2]) << 8) | bytes[i + 3];
        }
        return ints;
    }

    private static long[] longsFromBytes(final byte[] bytes) {
        final int size = bytes.length / LONG_BYTE_SIZE;
        final long[] longs = new long[size];
        for (int i = 0; i < size; i++) {
            //@formatter:off
            longs[i] = (((long) bytes[i    ]) << 56) | (((long) bytes[i + 1]) << 48) | (((long) bytes[i + 2]) << 40) | (((long) bytes[i + 3]) << 32)
                     | (((long) bytes[i + 4]) << 24) | (((long) bytes[i + 5]) << 16) | (((long) bytes[i + 6]) << 8)  | bytes[i + 7];
            //@formatter:on
        }
        return longs;
    }

    public static byte[] bytesFromShorts(final short[] shorts) {
        final int shortLength = shorts.length;
        final byte[] bytes = new byte[shortLength * SHORT_BYTE_SIZE];
        for (int i = 0; i < shortLength; i++) {
            final int offset = i * SHORT_BYTE_SIZE;
            final short shortValue = shorts[i];
            bytes[offset] = (byte) (shortValue >> 8);
            bytes[offset + 1] = (byte) shortValue;
        }
        return bytes;
    }

    public static byte[] bytesFromInts(final int[] ints) {
        final int intsLength = ints.length;
        final byte[] bytes = new byte[intsLength * INTEGER_BYTE_SIZE];
        for (int i = 0; i < intsLength; i++) {
            final int offset = i * INTEGER_BYTE_SIZE;
            final int intValue = ints[i];
            bytes[offset] = (byte) (intValue >> 24);
            bytes[offset + 1] = (byte) (intValue >> 16);
            bytes[offset + 2] = (byte) (intValue >> 8);
            bytes[offset + 3] = (byte) intValue;
        }
        return bytes;
    }

    public static byte[] bytesFromLongs(final long[] longs) {
        final int longsLength = longs.length;
        final byte[] bytes = new byte[longsLength * LONG_BYTE_SIZE];
        for (int i = 0; i < longsLength; i++) {
            final int offset = i * LONG_BYTE_SIZE;
            final long longValue = longs[i];
            bytes[offset] = (byte) (longValue >> 56);
            bytes[offset + 1] = (byte) (longValue >> 48);
            bytes[offset + 2] = (byte) (longValue >> 40);
            bytes[offset + 3] = (byte) (longValue >> 32);
            bytes[offset + 4] = (byte) (longValue >> 24);
            bytes[offset + 5] = (byte) (longValue >> 16);
            bytes[offset + 6] = (byte) (longValue >> 8);
            bytes[offset + 7] = (byte) longValue;
        }
        return bytes;
    }
}
