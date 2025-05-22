/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;

import com.oracle.truffle.api.strings.AbstractTruffleString;
import com.oracle.truffle.api.strings.MutableTruffleString;
import com.oracle.truffle.api.strings.TruffleString;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageConstants;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.nodes.LookupMethodNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodes.NativeObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchIndirectNaryNode.TryPrimitiveNaryNode;
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

    public static NativeObject newNativeString(final SqueakImageChunk chunk) {
        final ClassObject klass = chunk.getSqueakClass();
        final byte[] bytes = chunk.getBytes();
        final TruffleString.Encoding encoding = getTruffleStringEncoding(klass);
        return new NativeObject(chunk.getHeader(), klass, MutableTruffleString.fromByteArrayUncached(bytes,0, bytes.length, encoding, false));
    }

    public static NativeObject newNativeByteString(final SqueakImageContext img, final String string) {
        final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        final TruffleString.Encoding encoding = getTruffleStringEncoding(img.byteStringClass);
        final MutableTruffleString truffleString = MutableTruffleString.fromByteArrayUncached(bytes, 0, bytes.length, encoding, false);
        return new NativeObject(img, img.byteStringClass, truffleString);
    }

    public static NativeObject newNativeByteString(final SqueakImageContext img, final TruffleString string) {
        final TruffleString.Encoding encoding = getTruffleStringEncoding(img.byteStringClass);
        return new NativeObject(img, img.byteStringClass, string.asMutableTruffleStringUncached(encoding));
    }

    public static NativeObject newNativeWideString(final SqueakImageContext img, final String string) {
        final ClassObject wideStringClass = img.getWideStringClass();
        final TruffleString.Encoding encoding = getTruffleStringEncoding(wideStringClass);
        final TruffleString immuntableString = TruffleString.fromJavaStringUncached(string, encoding);
        return newNativeWideString(img, immuntableString);
    }

    public static NativeObject newNativeWideString(final SqueakImageContext img, final TruffleString string) {
        final ClassObject wideStringClass = img.getWideStringClass();
        final TruffleString.Encoding encoding = getTruffleStringEncoding(wideStringClass);
        return new NativeObject(img, wideStringClass, string.asMutableTruffleStringUncached(encoding));
    }

    public static NativeObject newNativeString(final SqueakImageContext img, final ClassObject klass, final MutableTruffleString string) {
        return new NativeObject(img, klass, string);
    }



    @Override
    public void fillin(final SqueakImageChunk chunk) {
        if (storage == ArrayUtils.EMPTY_ARRAY) { /* Fill in special selectors. */
            setStorage(chunk.getBytes());
        } else if (chunk.getImage().isHeadless() && isByteType()) {
            final SqueakImageContext image = chunk.getImage();
            if (image.getDebugErrorSelector() == null && Arrays.equals(SqueakImageContext.DEBUG_ERROR_SELECTOR_NAME, getByteStorage())) {
                image.setDebugErrorSelector(this);
            } else if (image.getDebugSyntaxErrorSelector() == null && Arrays.equals(SqueakImageContext.DEBUG_SYNTAX_ERROR_SELECTOR_NAME, getByteStorage())) {
                image.setDebugSyntaxErrorSelector(this);
            }
        }
    }

    @Override
    public int getNumSlots() {
        CompilerAsserts.neverPartOfCompilation();
        if (isByteType()) {
            return (int) Math.ceil((double) getByteLength() / BYTE_TO_WORD);
        } else if (isShortType()) {
            return (int) Math.ceil((double) getShortLength() / SHORT_TO_WORD);
        } else if (isIntType()) {
            return (int) Math.ceil((double) getIntLength() / INTEGER_TO_WORD);
        } else if (isLongType()) {
            return getLongLength();
        } else if (isTruffleStringType()){
            TruffleString.Encoding encoding = getTruffleStringEncoding();
            return (int) Math.ceil((double) getTruffleStringStorage().byteLength(encoding) / BYTE_TO_WORD);
        } else {
            throw SqueakException.create("Unexpected NativeObject");
        }
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

    public void convertToTruffleStringStorage(final MutableTruffleString string) {
        assert storage.getClass() != MutableTruffleString.class : "Converting storage of same type unnecessary";
        setStorage(string);
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

    public MutableTruffleString getTruffleStringStorage() {
        assert isTruffleStringType();
        return (MutableTruffleString) storage;
    }

    public void setTruffleStringStorage(final MutableTruffleString string) {
        assert isTruffleStringType();
        setStorage(string);
    }

    public TruffleString getImmutableTruffleStringStorage() {
        return getTruffleStringStorage().asTruffleStringUncached(getTruffleStringEncoding());
    }

    public int getTruffleStringLength() {
        return getTruffleStringStorage().byteLength(getTruffleStringEncoding());
    }

    public byte[] getTruffleStringAsBytes() {
        assert isTruffleStringType();
        return getTruffleStringStorage().copyToByteArrayUncached(getTruffleStringEncoding());
    }


    private static TruffleString.Encoding getTruffleStringEncoding(final ClassObject squeakClass) {
        final SqueakImageContext image = squeakClass.getImage();
        if (squeakClass == image.byteStringClass) {
            return TruffleString.Encoding.UTF_8;
        } else if (squeakClass == image.getWideStringClass()) {
            return TruffleString.Encoding.UTF_32;
        } else {
            throw SqueakException.create("Unexpected string class");
        }
    }

    public TruffleString.Encoding getTruffleStringEncoding() {
        assert isTruffleStringType();
        final ClassObject squeakClass = getSqueakClass();
        return getTruffleStringEncoding(squeakClass);
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

    public boolean isTruffleStringType() {
        return storage instanceof MutableTruffleString;
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
        if(isTruffleStringType()) {
            return ((AbstractTruffleString) storage).toJavaStringUncached();
        } else {
            return StandardCharsets.UTF_8.decode(ByteBuffer.wrap((byte[]) storage)).toString();
        }
    }

    @TruffleBoundary
    public String asStringFromWideString() {
        final MutableTruffleString string = getTruffleStringStorage();
        return string.toJavaStringUncached();
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        final ClassObject squeakClass = getSqueakClass();
        final SqueakImageContext image = squeakClass.getImage();
        if (isByteType()) {
            if (image.isByteSymbolClass(squeakClass)) {
                return "#" + asStringUnsafe();
            } else {
                return "byte[" + getByteLength() + "]";
            }
        } else if (isShortType()) {
            return "short[" + getShortLength() + "]";
        } else if (isIntType()) {
            return "int[" + getIntLength() + "]";
        } else if (isLongType()) {
            return "long[" + getLongLength() + "]";
        } else if (isTruffleStringType()) {
            final MutableTruffleString truffleString = getTruffleStringStorage();
            final String javaString  = truffleString.toJavaStringUncached();
            final int length = javaString.length();
            if (length <= 40) {
                return "'" + javaString + "'";
            } else {
                return String.format("'%.30s...' (string length: %s)", javaString, length);
            }
        } else {
            throw SqueakException.create("Unexpected native object type");
        }
    }

    public boolean isDebugErrorSelector(final SqueakImageContext image) {
        return this == image.getDebugErrorSelector();
    }

    public boolean isDebugSyntaxErrorSelector(final SqueakImageContext image) {
        return this == image.getDebugSyntaxErrorSelector();
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
                return IndirectCallNode.getUncached().call(method.getCallTarget(), FrameAccess.newWith(GetOrCreateContextNode.getOrCreateUncached(frame), null, receiver, arguments));
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

    public static boolean needsWideString(final TruffleString s) {
        return needsWideString(s.toJavaStringUncached());
    }

    @Override
    public void write(final SqueakImageWriter writer) {
        if (isByteType()) {
            final int numSlots = getNumSlots();
            final int formatOffset = numSlots * BYTE_TO_WORD - getByteLength();
            assert 0 <= formatOffset && formatOffset <= 7 : "too many odd bits (see instSpec)";
            if (writeHeader(writer, formatOffset)) {
                writer.writeBytes(getByteStorage());
                writePaddingIfAny(writer, getByteLength());
            }
        } else if (isShortType()) {
            final int numSlots = getNumSlots();
            final int formatOffset = numSlots * SHORT_TO_WORD - getShortLength();
            assert 0 <= formatOffset && formatOffset <= 3 : "too many odd bits (see instSpec)";
            if (writeHeader(writer, formatOffset)) {
                for (final short value : getShortStorage()) {
                    writer.writeShort(value);
                }
                writePaddingIfAny(writer, getShortLength() * Short.BYTES);
            }
        } else if (isIntType()) {
            final int numSlots = getNumSlots();
            final int formatOffset = numSlots * INTEGER_TO_WORD - getIntLength();
            assert 0 <= formatOffset && formatOffset <= 1 : "too many odd bits (see instSpec)";
            if (writeHeader(writer, formatOffset)) {
                for (final int value : getIntStorage()) {
                    writer.writeInt(value);
                }
                writePaddingIfAny(writer, getIntLength() * Integer.BYTES);
            }
        } else if (isLongType()) {
            if (!writeHeader(writer)) {
                return;
            }
            for (final long value : getLongStorage()) {
                writer.writeLong(value);
            }
            /* Padding not required. */
        } else if (isTruffleStringType()) {
            final int numSlots = getNumSlots();
            final MutableTruffleString string = getTruffleStringStorage();
            final TruffleString.Encoding encoding = getTruffleStringEncoding();
            final int formatOffset = numSlots * BYTE_TO_WORD - string.byteLength(encoding);
            // TODO unsure if this is correct
            if (writeHeader(writer, formatOffset)) {
                writer.writeBytes(string.copyToByteArrayUncached(encoding));
                writePaddingIfAny(writer, string.byteLength(encoding));
            }
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
