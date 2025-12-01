/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageConstants;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.nodes.LookupMethodNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodes.NativeObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.GetOrCreateContextWithFrameNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchIndirectNaryNode.TryPrimitiveNaryNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.LargeIntegers;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class NativeObject extends AbstractSqueakObjectWithClassAndHash {
    public static final short BYTE_MAX = (short) (Math.pow(2, Byte.SIZE) - 1);
    public static final int SHORT_MAX = (int) (Math.pow(2, Short.SIZE) - 1);
    public static final long INTEGER_MAX = (long) (Math.pow(2, Integer.SIZE) - 1);
    public static final int BYTE_TO_WORD = Long.SIZE / Byte.SIZE;
    public static final int SHORT_TO_WORD = Long.SIZE / Short.SIZE;
    public static final int INTEGER_TO_WORD = Long.SIZE / Integer.SIZE;

    @CompilationFinal private Object storage;

    public NativeObject() { // constructor for special selectors
        super();
        storage = ArrayUtils.EMPTY_ARRAY;
    }

    private NativeObject(final long header, final ClassObject classObject, final Object storage) {
        super(header, classObject);
        assert storage != null : "Unexpected `null` value";
        this.storage = storage;
    }

    private NativeObject(final SqueakImageContext image, final ClassObject classObject, final Object storage) {
        super(image, classObject);
        assert storage != null : "Unexpected `null` value";
        this.storage = storage;
    }

    private NativeObject(final NativeObject original, final Object storageCopy) {
        super(original);
        storage = storageCopy;
    }

    public static NativeObject newNativeBytes(final SqueakImageChunk chunk) {
        return new NativeObject(chunk.getHeader(), chunk.getSqueakClass(), chunk.getBytes());
    }

    public static NativeObject newNativeBytes(final SqueakImageContext img, final ClassObject klass, final byte[] bytes) {
        return new NativeObject(img, klass, bytes);
    }

    public static NativeObject newNativeBytes(final SqueakImageContext img, final ClassObject klass, final int size) {
        return newNativeBytes(img, klass, new byte[size]);
    }

    public static NativeObject newNativeInts(final SqueakImageChunk chunk) {
        return new NativeObject(chunk.getHeader(), chunk.getSqueakClass(), UnsafeUtils.toInts(chunk.getBytes()));
    }

    public static NativeObject newNativeInts(final SqueakImageContext img, final ClassObject klass, final int size) {
        return newNativeInts(img, klass, new int[size]);
    }

    public static NativeObject newNativeInts(final SqueakImageContext img, final ClassObject klass, final int[] words) {
        return new NativeObject(img, klass, words);
    }

    public static NativeObject newNativeLongs(final SqueakImageChunk chunk) {
        return new NativeObject(chunk.getHeader(), chunk.getSqueakClass(), UnsafeUtils.toLongs(chunk.getBytes()));
    }

    public static NativeObject newNativeLongs(final SqueakImageContext img, final ClassObject klass, final int size) {
        return newNativeLongs(img, klass, new long[size]);
    }

    public static NativeObject newNativeLongs(final SqueakImageContext img, final ClassObject klass, final long[] longs) {
        return new NativeObject(img, klass, longs);
    }

    public static NativeObject newNativeShorts(final SqueakImageChunk chunk) {
        return new NativeObject(chunk.getHeader(), chunk.getSqueakClass(), UnsafeUtils.toShorts(chunk.getBytes()));
    }

    public static NativeObject newNativeShorts(final SqueakImageContext img, final ClassObject klass, final int size) {
        return newNativeShorts(img, klass, new short[size]);
    }

    public static NativeObject newNativeShorts(final SqueakImageContext img, final ClassObject klass, final short[] shorts) {
        return new NativeObject(img, klass, shorts);
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        if (storage == ArrayUtils.EMPTY_ARRAY) { /* Fill in special selectors. */
            setStorage(chunk.getBytes());
        }
    }

    @Override
    public int getNumSlots() {
        CompilerAsserts.neverPartOfCompilation();
        if (isByteType()) {
            return getNumSlots(getByteLength(), BYTE_TO_WORD);
        } else if (isShortType()) {
            return getNumSlots(getShortLength(), SHORT_TO_WORD);
        } else if (isIntType()) {
            return getNumSlots(getIntLength(), INTEGER_TO_WORD);
        } else if (isLongType()) {
            return getLongLength();
        } else {
            throw SqueakException.create("Unexpected NativeObject");
        }
    }

    private static int getNumSlots(final int length, final int size) {
        return length / size + (length % size == 0 ? 0 : 1);
    }

    @Override
    public int instsize() {
        return 0;
    }

    @Override
    public int size() {
        CompilerAsserts.neverPartOfCompilation();
        return NativeObjectSizeNode.executeUncached(this);
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

    public NativeObject shallowCopyBytes() {
        return new NativeObject(this, getByteStorage());
    }

    public void convertToBytesStorage(final byte[] bytes) {
        assert storage.getClass() != byte[].class : "Converting storage of same type unnecessary";
        setStorage(bytes);
    }

    public void convertToIntsStorage(final int[] ints) {
        assert storage.getClass() != int[].class : "Converting storage of same type unnecessary";
        setStorage(ints);
    }

    public void convertToLongsStorage(final long[] longs) {
        assert storage.getClass() != long[].class : "Converting storage of same type unnecessary";
        setStorage(longs);
    }

    public void convertToShortsStorage(final short[] shorts) {
        assert storage.getClass() != short[].class : "Converting storage of same type unnecessary";
        setStorage(shorts);
    }

    public byte getByte(final long index) {
        assert isByteType();
        return UnsafeUtils.getByte((byte[]) storage, (int) index);
    }

    public int getByteUnsigned(final long index) {
        return Byte.toUnsignedInt(getByte(index));
    }

    public void setByte(final long index, final byte value) {
        assert isByteType();
        UnsafeUtils.putByte((byte[]) storage, (int) index, value);
    }

    public void setByte(final long index, final int value) {
        assert value < 256;
        setByte(index, (byte) value);
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
        if (!isNotForwarded()) {
            return "forward to " + getForwardingPointer().toString();
        }
        final ClassObject squeakClass = getSqueakClass();
        /*
         * This may be accessed from outside a context (when Truffle accesses sources), so we cannot
         * look up the context here.
         */
        final SqueakImageContext image = squeakClass.getImage();
        if (isByteType()) {
            if (image.isByteStringClass(squeakClass)) {
                final String fullString = asStringUnsafe();
                final int fullLength = fullString.length();
                /* Split at first non-printable character. */
                final String displayString = fullString.split("[^\\p{Print}]", 2)[0];
                if (fullLength <= 40 && fullLength == displayString.length()) {
                    /* fullString is short and has printable characters only. */
                    return "'" + fullString + "'";
                }
                return String.format("'%.30s...' (string length: %s)", displayString, fullLength);
            } else if (image.isByteSymbolClass(squeakClass)) {
                return "#" + asStringUnsafe();
            } else if (image.isLargeIntegerClass(squeakClass)) {
                return LargeIntegers.toBigInteger(image, this).toString();
            } else {
                return "byte[" + getByteLength() + "]";
            }
        } else if (isShortType()) {
            return "short[" + getShortLength() + "]";
        } else if (isIntType()) {
            if (image.isWideStringClass(squeakClass)) {
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

    public boolean isDoesNotUnderstand(final SqueakImageContext image) {
        return this == image.doesNotUnderstand;
    }

    public Object executeAsSymbolSlow(final SqueakImageContext image, final VirtualFrame frame, final Object receiver, final Object... arguments) {
        CompilerAsserts.neverPartOfCompilation();
        assert SqueakImageContext.getSlow().isByteSymbolClass(getSqueakClass());
        final Object lookupResult = LookupMethodNode.executeUncached(SqueakObjectClassNode.executeUncached(receiver), this);
        if (lookupResult instanceof CompiledCodeObject method) {
            final Object result = TryPrimitiveNaryNode.executeUncached(image.externalSenderFrame, method, this, arguments);
            if (result != null) {
                return result;
            } else {
                return IndirectCallNode.getUncached().call(method.getCallTarget(), FrameAccess.newWith(GetOrCreateContextWithFrameNode.executeUncached(frame), null, receiver, arguments));
            }
        } else {
            throw SqueakException.create("Illegal uncached message send");
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

    @Override
    public void write(final SqueakImageWriter writer) {
        if (isByteType()) {
            final int byteLength = getByteLength();
            final int numSlots = getNumSlots(byteLength, BYTE_TO_WORD);
            final int formatOffset = numSlots * BYTE_TO_WORD - byteLength;
            assert 0 <= formatOffset && formatOffset <= 7 : "too many odd bits (see instSpec)";
            if (writeHeader(writer, numSlots, formatOffset)) {
                writer.writeBytes(getByteStorage());
                writePaddingIfAny(writer, byteLength);
            }
        } else if (isShortType()) {
            final int shortLength = getShortLength();
            final int numSlots = getNumSlots(shortLength, SHORT_TO_WORD);
            final int formatOffset = numSlots * SHORT_TO_WORD - shortLength;
            assert 0 <= formatOffset && formatOffset <= 3 : "too many odd bits (see instSpec)";
            if (writeHeader(writer, numSlots, formatOffset)) {
                for (final short value : getShortStorage()) {
                    writer.writeShort(value);
                }
                writePaddingIfAny(writer, shortLength * Short.BYTES);
            }
        } else if (isIntType()) {
            final int intLength = getIntLength();
            final int numSlots = getNumSlots(intLength, INTEGER_TO_WORD);
            final int formatOffset = numSlots * INTEGER_TO_WORD - intLength;
            assert 0 <= formatOffset && formatOffset <= 1 : "too many odd bits (see instSpec)";
            if (writeHeader(writer, numSlots, formatOffset)) {
                for (final int value : getIntStorage()) {
                    writer.writeInt(value);
                }
                writePaddingIfAny(writer, intLength * Integer.BYTES);
            }
        } else if (isLongType()) {
            if (!writeHeader(writer)) {
                return;
            }
            for (final long value : getLongStorage()) {
                writer.writeLong(value);
            }
            /* Padding not required. */
        } else {
            throw SqueakException.create("Unexpected object");
        }
    }

    private static void writePaddingIfAny(final SqueakImageWriter writer, final int numberOfBytes) {
        final int offset = numberOfBytes % SqueakImageConstants.WORD_SIZE;
        if (offset > 0) {
            writer.writePadding(SqueakImageConstants.WORD_SIZE - offset);
        }
    }

    public void writeAsFreeList(final SqueakImageWriter writer) {
        if (isLongType()) {
            /* Write header. */
            final int numSlots = getLongLength();
            assert numSlots < SqueakImageConstants.OVERFLOW_SLOTS;
            /* Free list is of format 9 and pinned. */
            writer.writeLong(SqueakImageConstants.ObjectHeader.getHeader(numSlots, getOrCreateSqueakHash(), 9, SqueakImageConstants.WORD_SIZE_CLASS_INDEX_PUN, true));
            /* Write content. */
            for (final long value : getLongStorage()) {
                writer.writeLong(value);
            }
        } else {
            throw SqueakException.create("Trying to write unexpected hidden native object");
        }
    }
}
