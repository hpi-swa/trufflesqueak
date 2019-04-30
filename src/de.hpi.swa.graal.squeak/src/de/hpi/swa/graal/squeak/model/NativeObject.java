package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.reading.SqueakImageChunk;
import de.hpi.swa.graal.squeak.util.ArrayConversionUtils;

@ExportLibrary(InteropLibrary.class)
public final class NativeObject extends AbstractSqueakObjectWithClassAndHash {
    public static final short BYTE_MAX = (short) (Math.pow(2, Byte.SIZE) - 1);
    public static final int SHORT_MAX = (int) (Math.pow(2, Short.SIZE) - 1);
    public static final long INTEGER_MAX = (long) (Math.pow(2, Integer.SIZE) - 1);

    @CompilationFinal private Object storage;

    public NativeObject(final SqueakImageContext image) { // constructor for special selectors
        super(image, -1, null);
        storage = new byte[0];
    }

    private NativeObject(final SqueakImageContext image, final ClassObject classObject, final Object storage) {
        super(image, classObject);
        assert storage != null;
        this.storage = storage;
    }

    private NativeObject(final SqueakImageContext image, final long hash, final ClassObject classObject, final Object storage) {
        super(image, hash, classObject);
        assert storage != null;
        this.storage = storage;
    }

    public static NativeObject newNativeBytes(final SqueakImageChunk chunk) {
        return new NativeObject(chunk.image, chunk.getHash(), chunk.getSqClass(), chunk.getBytes());
    }

    public static NativeObject newNativeBytes(final SqueakImageContext img, final ClassObject klass, final byte[] bytes) {
        return new NativeObject(img, klass, bytes);
    }

    public static NativeObject newNativeBytes(final SqueakImageContext img, final ClassObject klass, final int size) {
        return newNativeBytes(img, klass, new byte[size]);
    }

    public static NativeObject newNativeInts(final SqueakImageChunk chunk) {
        return new NativeObject(chunk.image, chunk.getHash(), chunk.getSqClass(), chunk.getInts());
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

    public static NativeObject newNativeShorts(final SqueakImageChunk chunk) {
        return new NativeObject(chunk.image, chunk.getHash(), chunk.getSqClass(), chunk.getShorts());
    }

    public static NativeObject newNativeShorts(final SqueakImageContext img, final ClassObject klass, final int size) {
        return newNativeShorts(img, klass, new short[size]);
    }

    public static NativeObject newNativeShorts(final SqueakImageContext img, final ClassObject klass, final short[] shorts) {
        return new NativeObject(img, klass, shorts);
    }

    @Override
    public int instsize() {
        return 0;
    }

    @Override
    public int size() {
        throw SqueakException.create("Use NativeObjectSizeNode");
    }

    public void become(final NativeObject other) {
        super.becomeOtherClass(other);
        final Object otherStorage = other.storage;
        other.setStorage(storage);
        setStorage(otherStorage);
    }

    public void convertToBytesStorage(final byte[] bytes) {
        assert storage.getClass() != bytes.getClass() : "Converting storage of same type unnecessary";
        setStorage(bytes);
    }

    public void convertToIntsStorage(final byte[] bytes) {
        assert storage.getClass() != bytes.getClass() : "Converting storage of same type unnecessary";
        setStorage(ArrayConversionUtils.intsFromBytes(bytes));
    }

    public void convertToLongsStorage(final byte[] bytes) {
        assert storage.getClass() != bytes.getClass() : "Converting storage of same type unnecessary";
        setStorage(ArrayConversionUtils.longsFromBytes(bytes));
    }

    public void convertToShortsStorage(final byte[] bytes) {
        assert storage.getClass() != bytes.getClass() : "Converting storage of same type unnecessary";
        setStorage(ArrayConversionUtils.shortsFromBytes(bytes));
    }

    public int getByteLength() {
        return getByteStorage().length;
    }

    public byte[] getByteStorage() {
        assert isByteType();
        return (byte[]) storage;
    }

    public int getIntLength() {
        return getIntStorage().length;
    }

    public int[] getIntStorage() {
        assert isIntType();
        return (int[]) storage;
    }

    public int getLongLength() {
        return getLongStorage().length;
    }

    public long[] getLongStorage() {
        assert isLongType();
        return (long[]) storage;
    }

    public int getShortLength() {
        return getShortStorage().length;
    }

    public short[] getShortStorage() {
        assert isShortType();
        return (short[]) storage;
    }

    public boolean hasSameFormat(final ClassObject other) {
        return getSqueakClass().getFormat() == other.getFormat();
    }

    public boolean hasSameStorageType(final NativeObject other) {
        return storage.getClass() == other.storage.getClass();
    }

    public boolean isByteType() {
        return storage.getClass() == byte[].class;
    }

    public boolean isIntType() {
        return storage.getClass() == int[].class;
    }

    public boolean isLongType() {
        return storage.getClass() == long[].class;
    }

    public boolean isShortType() {
        return storage.getClass() == short[].class;
    }

    public void setStorage(final Object storage) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.storage = storage;
    }

    public String asStringUnsafe() {
        return ArrayConversionUtils.bytesToString(getByteStorage());
    }

    @TruffleBoundary
    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        if (isByteType()) {
            return asStringUnsafe();
        } else if (isShortType()) {
            return "ShortArray(size=" + getShortLength() + ")";
        } else if (isIntType()) {
            return "IntArray(size=" + getIntLength() + ")";
        } else if (isLongType()) {
            return "LongArray(size=" + getLongLength() + ")";
        } else {
            throw SqueakException.create("Unexpected native object type");
        }
    }

    public boolean isDebugErrorSelector() {
        return this == image.getDebugErrorSelector();
    }

    public boolean isDebugSyntaxErrorSelector() {
        return this == image.getDebugSyntaxErrorSelector();
    }

    public boolean isDoesNotUnderstand() {
        return this == image.doesNotUnderstand;
    }

    /*
     * INTEROPERABILITY
     */

    @ExportMessage
    public boolean isString() {
        return getSqueakClass().isStringOrSymbolClass();
    }

    @ExportMessage
    public String asString() throws UnsupportedMessageException {
        if (isString()) {
            return asStringUnsafe();
        } else {
            throw UnsupportedMessageException.create();
        }
    }
}
