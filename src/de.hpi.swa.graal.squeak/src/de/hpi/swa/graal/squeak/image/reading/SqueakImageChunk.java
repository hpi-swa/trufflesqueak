/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.image.reading;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CharacterObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.VariablePointersObject;
import de.hpi.swa.graal.squeak.model.WeakVariablePointersObject;
import de.hpi.swa.graal.squeak.util.ArrayConversionUtils;
import de.hpi.swa.graal.squeak.util.UnsafeUtils;

public final class SqueakImageChunk {
    private static final long SMALLFLOAT_MASK = 896L << 52 + 1;

    protected Object object;
    private ClassObject sqClass;
    private Object[] pointers;

    protected final int classIndex;
    protected final int pos;

    public final SqueakImageContext image;
    private final SqueakImageReader reader;
    private final int format;
    private final int hash;
    private final byte[] data;
    private long[] words;

    public SqueakImageChunk(final SqueakImageReader reader,
                    final SqueakImageContext image,
                    final byte[] data,
                    final int format,
                    final int classIndex,
                    final int hash,
                    final int pos) {
        this.reader = reader;
        this.image = image;
        this.format = format;
        this.classIndex = classIndex;
        this.hash = hash;
        this.pos = pos;
        this.data = Arrays.copyOf(data, data.length - getPadding());
    }

    public static SqueakImageChunk createDummyChunk(final SqueakImageContext image, final Object[] pointers) {
        final SqueakImageChunk chunk = new SqueakImageChunk(null, image, new byte[0], 0, 0, 0, 0);
        chunk.pointers = pointers;
        return chunk;
    }

    public ClassObject asClassObject(final ClassObject metaClassObject) {
        if (object == null) {
            assert format == 1;
            object = new ClassObject(image, hash, metaClassObject);
        } else if (object == NilObject.SINGLETON) {
            return null;
        }
        return (ClassObject) object;
    }

    public Object asObject() {
        if (object == null) {
            final ClassObject squeakClass = getSqClass();
            if (format == 0) { // no fields
                object = new EmptyObject(image, hash, squeakClass);
            } else if (format == 1) { // fixed pointers
                // classes should already be instantiated at this point, check a bit
                assert squeakClass != image.metaClass && squeakClass.getSqueakClass() != image.metaClass;
                if (squeakClass.instancesAreClasses()) {
                    object = new ClassObject(image, hash, squeakClass);
                } else {
                    object = new PointersObject(image, hash, squeakClass);
                }
            } else if (format == 2) { // indexable fields
                object = new ArrayObject(image, hash, squeakClass);
            } else if (format == 3) { // fixed and indexable fields
                if (squeakClass == image.methodContextClass) {
                    object = ContextObject.createWithHash(image, hash);
                } else if (squeakClass == image.blockClosureClass) {
                    object = new BlockClosureObject(image, hash);
                } else {
                    object = new VariablePointersObject(image, hash, squeakClass);
                }
            } else if (format == 4) { // indexable weak fields
                object = new WeakVariablePointersObject(image, hash, squeakClass);
            } else if (format == 5) { // fixed weak fields
                throw SqueakException.create("Ephemerons not (yet) supported");
            } else if (format <= 8) {
                assert false : "Should never happen (unused format)";
            } else if (format == 9) { // 64-bit integers
                object = NativeObject.newNativeLongs(this);
            } else if (format <= 11) { // 32-bit integers
                if (squeakClass == image.floatClass) {
                    object = FloatObject.newFromChunkWords(image, getInts());
                } else {
                    object = NativeObject.newNativeInts(this);
                }
            } else if (format <= 15) { // 16-bit integers
                object = NativeObject.newNativeShorts(this);
            } else if (format <= 23) { // bytes
                if (squeakClass == image.largePositiveIntegerClass || squeakClass == image.largeNegativeIntegerClass) {
                    object = new LargeIntegerObject(image, hash, squeakClass, getBytes()).reduceIfPossible();
                } else {
                    object = NativeObject.newNativeBytes(this);
                }
            } else if (format <= 31) { // compiled methods
                object = new CompiledMethodObject(image, hash);
            }
        }
        return object;
    }

    public int getFormat() {
        return format;
    }

    public int getHash() {
        return hash;
    }

    public ClassObject getSqClass() {
        if (sqClass == null) {
            sqClass = getClassChunk().asClassObject(null);
        }
        return sqClass;
    }

    public SqueakImageChunk getClassChunk() {
        final int majorIdx = majorClassIndexOf(classIndex);
        final int minorIdx = minorClassIndexOf(classIndex);
        final SqueakImageChunk classTablePage = reader.getChunk(reader.hiddenRootsChunk.getWords()[majorIdx]);
        final SqueakImageChunk classChunk = reader.getChunk(classTablePage.getWords()[minorIdx]);
        assert classChunk != null : "Unable to find class chunk.";
        return classChunk;
    }

    private static int majorClassIndexOf(final int classid) {
        return classid >> 10;
    }

    private static int minorClassIndexOf(final int classid) {
        return classid & (1 << 10) - 1;
    }

    public void setSqClass(final ClassObject baseSqueakObject) {
        sqClass = baseSqueakObject;
    }

    public Object[] getPointers() {
        if (pointers == null) {
            final long[] theWords = getWords();
            pointers = new Object[theWords.length];
            for (int i = 0; i < theWords.length; i++) {
                pointers[i] = decodePointer(theWords[i]);
            }
        }
        return pointers;
    }

    public Object[] getPointers(final int end) {
        if (pointers == null) {
            final long[] theWords = getWords();
            pointers = new Object[end];
            for (int i = 0; i < end; i++) {
                pointers[i] = decodePointer(theWords[i]);
            }
        }
        return pointers;
    }

    private Object decodePointer(final long ptr) {
        if (reader.is64bit) {
            switch ((int) (ptr & 7)) {
                case 0:
                    final SqueakImageChunk chunk = reader.getChunk(ptr);
                    if (chunk == null) {
                        logBogusPointer(ptr);
                        return ptr >>> 3;
                    } else {
                        return chunk.asObject();
                    }
                case 1: // SmallInteger
                    return ptr >> 3;
                case 2: // Character
                    return CharacterObject.valueOf((int) (ptr >> 3));
                case 4: // SmallFloat (see Spur64BitMemoryManager>>#smallFloatBitsOf:)
                    long valueWithoutTag = ptr >>> 3;
                    if (valueWithoutTag > 1) {
                        valueWithoutTag += SMALLFLOAT_MASK;
                    }
                    return Double.longBitsToDouble(Long.rotateRight(valueWithoutTag, 1));
                default:
                    throw SqueakException.create("Unexpected pointer");
            }
        } else {
            if ((ptr & 3) == 0) {
                final SqueakImageChunk chunk = reader.getChunk(ptr);
                if (chunk == null) {
                    logBogusPointer(ptr);
                    return ptr >> 1;
                } else {
                    return chunk.asObject();
                }
            } else if ((ptr & 1) == 1) {
                return ptr >> 1;
            } else {
                assert (ptr & 3) == 2;
                return CharacterObject.valueOf((int) (ptr >> 2));
            }
        }
    }

    @TruffleBoundary
    private void logBogusPointer(final long ptr) {
        image.getError().println("Bogus pointer: " + ptr + ". Treating as smallint.");
    }

    public byte[] getBytes() {
        return getBytes(0);
    }

    public byte[] getBytes(final int start) {
        return Arrays.copyOfRange(data, start, data.length);
    }

    public short[] getShorts() {
        return ArrayConversionUtils.shortsFromBytes(data);
    }

    public int[] getInts() {
        return ArrayConversionUtils.intsFromBytes(data);
    }

    public long[] getWords() {
        if (words == null) {
            if (reader.is64bit) {
                words = ArrayConversionUtils.longsFromBytes(data);
            } else {
                final int size = data.length / ArrayConversionUtils.INTEGER_BYTE_SIZE;
                words = new long[size];
                for (int i = 0; i < size; i++) {
                    words[i] = UnsafeUtils.getInt(data, i);
                }
            }
        }
        return words;
    }

    public long[] getLongs() {
        return ArrayConversionUtils.longsFromBytes(data);
    }

    public int getPadding() {
        if (image.flags.is64bit()) {
            if (16 <= format && format <= 31) {
                return format & 7;
            } else if (format == 11) {
                // 32-bit words with 1 word padding
                return 4;
            } else if (12 <= format && format <= 15) {
                // 16-bit words with 2, 4, or 6 bytes padding
                return format & 3;
            } else if (10 <= format) {
                return format & 1;
            } else {
                return 0;
            }
        } else {
            if (16 <= format && format <= 31) {
                return format & 3;
            } else if (format == 11) {
                // 32-bit words with 1 word padding
                return 4;
            } else if (12 <= format && format <= 15) {
                // 16-bit words with 2, 4, or 6 bytes padding
                return (format & 3) * 2;
            } else {
                return 0;
            }
        }
    }

    public byte getElementSize() {
        if (16 <= format && format <= 23) {
            return 1;
        } else {
            return 4;
        }
    }
}
