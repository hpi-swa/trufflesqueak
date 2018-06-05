package de.hpi.swa.graal.squeak.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.AbstractImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class NativeObject extends AbstractSqueakObject {
    @CompilationFinal public static final int SHORT_BYTE_SIZE = 2;
    @CompilationFinal public static final int INTEGER_BYTE_SIZE = 4;
    @CompilationFinal public static final int LONG_BYTE_SIZE = 8;
    @CompilationFinal public static final long BYTE_MAX = (long) (Math.pow(2, Byte.SIZE) - 1);
    @CompilationFinal public static final long SHORT_MAX = (long) (Math.pow(2, Short.SIZE) - 1);
    @CompilationFinal public static final long INTEGER_MAX = (long) (Math.pow(2, Integer.SIZE) - 1);

    @CompilationFinal protected Object storage;

    public static NativeObject newNativeBytes(final SqueakImageContext img, final ClassObject klass, final int size) {
        return newNativeBytes(img, klass, new byte[size]);
    }

    public static NativeObject newNativeBytes(final SqueakImageContext img, final ClassObject klass, final byte[] bytes) {
        return new NativeObject(img, klass, bytes);
    }

    public static NativeObject newNativeShorts(final SqueakImageContext img, final ClassObject klass, final int size) {
        return newNativeShorts(img, klass, new short[size]);
    }

    public static NativeObject newNativeShorts(final SqueakImageContext img, final ClassObject klass, final short[] shorts) {
        return new NativeObject(img, klass, shorts);
    }

    public static NativeObject newNativeInts(final SqueakImageContext img, final ClassObject klass, final int size) {
        return newNativeInts(img, klass, new int[size]);
    }

    public static NativeObject newNativeInts(final SqueakImageContext img, final ClassObject klass, final int[] words) {
        return new NativeObject(img, klass, words);
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

    public NativeObject(final SqueakImageContext image) { // constructor for special selectors
        super(image, null);
        this.storage = new byte[0];
    }

    @TruffleBoundary
    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation("");
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

    @Override
    public void fillin(final AbstractImageChunk chunk) {
        super.fillin(chunk);
        CompilerDirectives.transferToInterpreterAndInvalidate();
        if (isByteType()) {
            storage = chunk.getBytes();
        } else if (isShortType()) {
            storage = chunk.getShorts();
        } else if (isIntType()) {
            storage = chunk.getWords();
        } else if (isLongType()) {
            storage = chunk.getLongs();
        } else {
            throw new SqueakException("Unsupported storage type");
        }
    }

    @Override
    public boolean become(final AbstractSqueakObject other) {
        if (!(other instanceof NativeObject)) {
            throw new PrimitiveExceptions.PrimitiveFailed();
        }
        if (!super.become(other)) {
            throw new SqueakException("Should not fail");
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final NativeObject otherNativeObject = (NativeObject) other;
        final Object otherStorage = otherNativeObject.storage;
        otherNativeObject.storage = this.storage;
        this.storage = otherStorage;
        return true;
    }

    public LargeIntegerObject normalize(final ValueProfile storageType) {
        return new LargeIntegerObject(image, getSqClass(), getByteStorage(storageType));
    }

    public byte[] getByteStorage(final ValueProfile storageType) {
        assert isByteType();
        return (byte[]) storageType.profile(storage);
    }

    public short[] getShortStorage(final ValueProfile storageType) {
        assert isShortType();
        return (short[]) storageType.profile(storage);
    }

    public int[] getIntStorage(final ValueProfile storageType) {
        assert isIntType();
        return (int[]) storageType.profile(storage);
    }

    public long[] getLongStorage(final ValueProfile storageType) {
        assert isLongType();
        return (long[]) storageType.profile(storage);
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
        CompilerDirectives.transferToInterpreterAndInvalidate();
        storage = bytes;
    }

    public void convertToShortsStorage(final byte[] bytes) {
        assert storage.getClass() != bytes.getClass() : "Converting storage of same type unnecessary";
        CompilerDirectives.transferToInterpreterAndInvalidate();
        storage = shortsFromBytes(bytes);
    }

    public void convertToIntsStorage(final byte[] bytes) {
        assert storage.getClass() != bytes.getClass() : "Converting storage of same type unnecessary";
        CompilerDirectives.transferToInterpreterAndInvalidate();
        storage = intsFromBytes(bytes);
    }

    public void convertToLongsStorage(final byte[] bytes) {
        assert storage.getClass() != bytes.getClass() : "Converting storage of same type unnecessary";
        CompilerDirectives.transferToInterpreterAndInvalidate();
        storage = longsFromBytes(bytes);
    }

    public void setStorageForTesting(final byte[] bytes) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        storage = bytes;
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
        final ByteBuffer byteBuffer = ByteBuffer.allocate(shorts.length * SHORT_BYTE_SIZE);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        final ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
        shortBuffer.put(shorts);
        return byteBuffer.array();
    }

    public static byte[] bytesFromInts(final int[] ints) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(ints.length * INTEGER_BYTE_SIZE);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        final IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(ints);
        return byteBuffer.array();
    }

    public static byte[] bytesFromLongs(final long[] longs) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(longs.length * LONG_BYTE_SIZE);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        final LongBuffer longBuffer = byteBuffer.asLongBuffer();
        longBuffer.put(longs);
        return byteBuffer.array();
    }
}
