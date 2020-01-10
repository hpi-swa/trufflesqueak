/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.reading.SqueakImageChunk;
import de.hpi.swa.graal.squeak.interop.WrapToSqueakNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeObjectWriteNode;
import de.hpi.swa.graal.squeak.util.ArrayConversionUtils;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.SqueakMessageInterceptor;
import de.hpi.swa.graal.squeak.util.UnsafeUtils;

@ExportLibrary(InteropLibrary.class)
public final class NativeObject extends AbstractSqueakObjectWithClassAndHash {
    public static final String REPLACEMENT_CHAR = StandardCharsets.UTF_8.newDecoder().replacement();
    public static final short BYTE_MAX = (short) (Math.pow(2, Byte.SIZE) - 1);
    public static final int SHORT_MAX = (int) (Math.pow(2, Short.SIZE) - 1);
    public static final long INTEGER_MAX = (long) (Math.pow(2, Integer.SIZE) - 1);

    @CompilationFinal private Object storage;

    public NativeObject(final SqueakImageContext image) { // constructor for special selectors
        super(image, AbstractSqueakObjectWithHash.HASH_UNINITIALIZED, null);
        storage = ArrayUtils.EMPTY_ARRAY;
    }

    private NativeObject(final SqueakImageContext image, final ClassObject classObject, final Object storage) {
        super(image, classObject);
        assert storage != null : "Unexpected `null` value";
        this.storage = storage;
    }

    private NativeObject(final SqueakImageContext image, final long hash, final ClassObject classObject, final Object storage) {
        super(image, hash, classObject);
        assert storage != null : "Unexpected `null` value";
        this.storage = storage;
    }

    private NativeObject(final NativeObject original, final Object storageCopy) {
        super(original);
        storage = storageCopy;
    }

    public static NativeObject newNativeBytes(final SqueakImageChunk chunk) {
        return new NativeObject(chunk.getImage(), chunk.getHash(), chunk.getSqClass(), chunk.getBytes());
    }

    public static NativeObject newNativeBytes(final SqueakImageContext img, final ClassObject klass, final byte[] bytes) {
        return new NativeObject(img, klass, bytes);
    }

    public static NativeObject newNativeBytes(final SqueakImageContext img, final ClassObject klass, final int size) {
        return newNativeBytes(img, klass, new byte[size]);
    }

    public static NativeObject newNativeInts(final SqueakImageChunk chunk) {
        return new NativeObject(chunk.getImage(), chunk.getHash(), chunk.getSqClass(), ArrayConversionUtils.intsFromBytes(chunk.getBytes()));
    }

    public static NativeObject newNativeInts(final SqueakImageContext img, final ClassObject klass, final int size) {
        return newNativeInts(img, klass, new int[size]);
    }

    public static NativeObject newNativeInts(final SqueakImageContext img, final ClassObject klass, final int[] words) {
        return new NativeObject(img, klass, words);
    }

    public static NativeObject newNativeLongs(final SqueakImageChunk chunk) {
        return new NativeObject(chunk.getImage(), chunk.getHash(), chunk.getSqClass(), ArrayConversionUtils.longsFromBytes(chunk.getBytes()));
    }

    public static NativeObject newNativeLongs(final SqueakImageContext img, final ClassObject klass, final int size) {
        return newNativeLongs(img, klass, new long[size]);
    }

    public static NativeObject newNativeLongs(final SqueakImageContext img, final ClassObject klass, final long[] longs) {
        return new NativeObject(img, klass, longs);
    }

    public static NativeObject newNativeShorts(final SqueakImageChunk chunk) {
        return new NativeObject(chunk.getImage(), chunk.getHash(), chunk.getSqClass(), ArrayConversionUtils.shortsFromBytes(chunk.getBytes()));
    }

    public static NativeObject newNativeShorts(final SqueakImageContext img, final ClassObject klass, final int size) {
        return newNativeShorts(img, klass, new short[size]);
    }

    public static NativeObject newNativeShorts(final SqueakImageContext img, final ClassObject klass, final short[] shorts) {
        return new NativeObject(img, klass, shorts);
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        final byte[] bytes = chunk.getBytes();
        if (storage == ArrayUtils.EMPTY_ARRAY) { /* Fill in special selectors. */
            setStorage(bytes);
        } else if (isByteType()) {
            if (image.isHeadless()) {
                if (image.getDebugErrorSelector() == null && Arrays.equals(SqueakImageContext.DEBUG_ERROR_SELECTOR_NAME, (byte[]) storage)) {
                    image.setDebugErrorSelector(this);
                } else if (image.getDebugSyntaxErrorSelector() == null && Arrays.equals(SqueakImageContext.DEBUG_SYNTAX_ERROR_SELECTOR_NAME, (byte[]) storage)) {
                    image.setDebugSyntaxErrorSelector(this);
                }
            }
            SqueakMessageInterceptor.notifyLoadedSymbol(this, (byte[]) storage);
        }
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

    public NativeObject shallowCopy(final Object storageCopy) {
        return new NativeObject(this, storageCopy);
    }

    public void convertToBytesStorage(final byte[] bytes) {
        assert storage.getClass() != bytes.getClass() : "Converting storage of same type unnecessary";
        setStorage(bytes);
    }

    public void convertToIntsStorage(final byte[] bytes) {
        assert storage.getClass() != bytes.getClass() : "Converting storage of same type unnecessary";
        setStorage(ArrayConversionUtils.intsFromBytesReversed(bytes));
    }

    public void convertToLongsStorage(final byte[] bytes) {
        assert storage.getClass() != bytes.getClass() : "Converting storage of same type unnecessary";
        setStorage(ArrayConversionUtils.longsFromBytesReversed(bytes));
    }

    public void convertToShortsStorage(final byte[] bytes) {
        assert storage.getClass() != bytes.getClass() : "Converting storage of same type unnecessary";
        setStorage(ArrayConversionUtils.shortsFromBytesReversed(bytes));
    }

    public byte getByte(final long index) {
        assert isByteType();
        return UnsafeUtils.getByte((byte[]) storage, index);
    }

    public void setByte(final long index, final byte value) {
        assert isByteType();
        UnsafeUtils.putByte((byte[]) storage, index, value);
    }

    public int getByteLength() {
        return getByteStorage().length;
    }

    public byte[] getByteStorage() {
        assert isByteType();
        return (byte[]) storage;
    }

    public int getInt(final long index) {
        assert isIntType();
        return UnsafeUtils.getInt((int[]) storage, index);
    }

    public void setInt(final long index, final int value) {
        assert isIntType();
        UnsafeUtils.putInt((int[]) storage, index, value);
    }

    public int getIntLength() {
        return getIntStorage().length;
    }

    public int[] getIntStorage() {
        assert isIntType();
        return (int[]) storage;
    }

    public long getLong(final long index) {
        assert isLongType();
        return UnsafeUtils.getLong((long[]) storage, index);
    }

    public void setLong(final long index, final long value) {
        assert isLongType();
        UnsafeUtils.putLong((long[]) storage, index, value);
    }

    public int getLongLength() {
        return getLongStorage().length;
    }

    public long[] getLongStorage() {
        assert isLongType();
        return (long[]) storage;
    }

    public short getShort(final long index) {
        assert isShortType();
        return UnsafeUtils.getShort((short[]) storage, index);
    }

    public void setShort(final long index, final short value) {
        assert isShortType();
        UnsafeUtils.putShort((short[]) storage, index, value);
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

    public boolean isByteType() {
        return storage instanceof byte[];
    }

    public boolean isIntType() {
        return storage instanceof int[];
    }

    public boolean isLongType() {
        return storage instanceof long[];
    }

    public boolean isShortType() {
        return storage instanceof short[];
    }

    public void setStorage(final Object storage) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.storage = storage;
    }

    @TruffleBoundary
    public String asStringUnsafe() {
        return StandardCharsets.UTF_8.decode(ByteBuffer.wrap((byte[]) storage)).toString();
    }

    @TruffleBoundary
    public String asStringFromWideString() {
        final int[] ints = getIntStorage();
        return new String(ints, 0, ints.length);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        if (isByteType()) {
            final ClassObject squeakClass = getSqueakClass();
            if (squeakClass.isStringClass()) {
                final String fullString = asStringUnsafe();
                final int fullLength = fullString.length();
                /* Split at first non-printable character. */
                final String displayString = fullString.split("[^\\p{Print}]", 2)[0];
                if (fullLength <= 40 && fullLength == displayString.length()) {
                    /* fullString is short and has printable characters only. */
                    return "'" + fullString + "'";
                }
                return String.format("'%.30s...' (string length: %s)", displayString, fullLength);
            } else if (squeakClass.isSymbolClass()) {
                return "#" + asStringUnsafe();
            } else {
                return "byte[" + getByteLength() + "]";
            }
        } else if (isShortType()) {
            return "short[" + getShortLength() + "]";
        } else if (isIntType()) {
            if (getSqueakClass().isWideStringClass()) {
                return asStringFromWideString();
            } else {
                return "int[" + getIntLength() + "]";
            }
        } else if (isLongType()) {
            return "long[" + getLongLength() + "]";
        } else {
            throw SqueakException.create("Unexpected native object type");
        }
    }

    public boolean isAllowedInHeadlessMode() {
        return !isDebugErrorSelector() && !isDebugSyntaxErrorSelector();
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

    @SuppressWarnings("static-method")
    @ExportMessage
    protected boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    protected long getArraySize(@Shared("sizeNode") @Cached final NativeObjectSizeNode sizeNode) {
        return sizeNode.execute(this);
    }

    @SuppressWarnings("static-method")
    @ExportMessage(name = "isArrayElementReadable")
    @ExportMessage(name = "isArrayElementModifiable")
    @ExportMessage(name = "isArrayElementInsertable")
    protected boolean isArrayElementReadable(final long index, @Shared("sizeNode") @Cached final NativeObjectSizeNode sizeNode) {
        return 0 <= index && index < sizeNode.execute(this);
    }

    @ExportMessage
    public abstract static class ReadArrayElement {
        @Specialization(guards = "obj.isByteType()")
        protected static final byte doNativeBytes(final NativeObject obj, final long index) {
            return obj.getByte(index);
        }

        @Specialization(guards = "obj.isShortType()")
        protected static final short doNativeShorts(final NativeObject obj, final long index) {
            return obj.getShort(index);
        }

        @Specialization(guards = "obj.isIntType()")
        protected static final int doNativeInts(final NativeObject obj, final long index) {
            return obj.getInt(index);
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final long doNativeLongs(final NativeObject obj, final long index) {
            return obj.getLong(index);
        }
    }

    @ExportMessage
    protected void writeArrayElement(final long index, final Object value,
                    @Exclusive @Cached final WrapToSqueakNode wrapNode,
                    @Cached final NativeObjectWriteNode writeNode) throws InvalidArrayIndexException, UnsupportedTypeException {
        try {
            writeNode.execute(this, index, wrapNode.executeWrap(value));
        } catch (final PrimitiveFailed e) {
            /* NativeObjectWriteNode may throw PrimitiveFailed if value cannot be stored. */
            throw UnsupportedTypeException.create(new Object[]{value}, "Cannot store value in NativeObject");
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw InvalidArrayIndexException.create(index);
        }
    }

    @ExportMessage
    public boolean isString() {
        final ClassObject squeakClass = getSqueakClass();
        return squeakClass.isStringClass() || squeakClass.isSymbolClass() || squeakClass.isWideStringClass();
    }

    @ExportMessage
    public String asString(@Cached("createBinaryProfile()") final ConditionProfile byteStringOrSymbolProfile,
                    @Cached final BranchProfile errorProfile) throws UnsupportedMessageException {
        final ClassObject squeakClass = getSqueakClass();
        if (byteStringOrSymbolProfile.profile(squeakClass.isStringClass() || squeakClass.isSymbolClass())) {
            return asStringUnsafe();
        } else if (squeakClass.isWideStringClass()) {
            return asStringFromWideString();
        } else {
            errorProfile.enter();
            throw UnsupportedMessageException.create();
        }
    }

    public static boolean needsWideString(final String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 255) {
                return true;
            }
        }
        return false;
    }
}
