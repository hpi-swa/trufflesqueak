/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.image;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageConstants.ObjectHeader;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.model.WeakVariablePointersObject;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class SqueakImageChunk {
    private Object object;
    private ClassObject sqClass;
    private Object[] pointers;

    private final int position;

    private final SqueakImageContext image;
    private final SqueakImageReader reader;
    private final long objectHeader;
    private final byte[] bytes;

    public SqueakImageChunk(final SqueakImageReader reader,
                    final SqueakImageContext image,
                    final long objectHeader,
                    final int position,
                    final byte[] bytes) {
        this.reader = reader;
        this.image = image;
        this.objectHeader = objectHeader;
        this.position = position;
        this.bytes = bytes;
        if (bytes == null) { // ignored object
            object = NilObject.SINGLETON;
        }
    }

    public static SqueakImageChunk createDummyChunk(final SqueakImageContext image, final Object[] pointers) {
        final SqueakImageChunk chunk = new SqueakImageChunk(null, image, 0, 0, new byte[0]);
        chunk.pointers = pointers;
        return chunk;
    }

    public ClassObject asClassObject() {
        if (object == null) {
            assert getFormat() == 1;
            final int classIndex = getClassIndex();
            if (classIndex == SqueakImageConstants.FREE_OBJECT_CLASS_INDEX_PUN) {
                object = NilObject.SINGLETON;
            } else {
                object = new ClassObject(image, objectHeader);
            }
        } else if (object == NilObject.SINGLETON) {
            return null;
        }
        return (ClassObject) object;
    }

    public Object asObject() {
        if (object == null) {
            final int classIndex = getClassIndex();
            if (classIndex == SqueakImageConstants.FREE_OBJECT_CLASS_INDEX_PUN) {
                return object = NilObject.SINGLETON;
            }
            if (bytes == null) {
                assert SqueakImageReader.isHiddenObject(classIndex);
                /* Ignored object (see SqueakImageReader#ignoreObjectData) */
                return object = NilObject.SINGLETON;
            }
            final int format = getFormat();
            if (format == 0) { // no fields
                object = new EmptyObject(image, objectHeader);
            } else if (format == 1) { // fixed pointers
                final ClassObject squeakClass = getSqueakClass();
                if (squeakClass.instancesAreClasses()) {
                    /*
                     * In rare cases, there are still some classes that are not in the class table
                     * for some reason (e.g. not completely removed from the system yet).
                     */
                    object = new ClassObject(image, objectHeader);
                } else {
                    // classes should already be instantiated at this point, check a bit
// assert squeakClass != image.metaClass && squeakClass.getSqueakClass() != image.metaClass;
                    object = new PointersObject(image, objectHeader);
                }
            } else if (format == 2) { // indexable fields
                object = new ArrayObject(image, objectHeader);
            } else if (format == 3) { // fixed and indexable fields
                if (classIndex == image.methodContextClassIndex) {
                    object = ContextObject.createWithHeader(image, objectHeader);
                } else if (classIndex == reader.blockClosureClassIndex || classIndex == reader.fullBlockClosureClassIndex) {
                    object = BlockClosureObject.createWithHeader(image, objectHeader);
                } else {
                    object = new VariablePointersObject(image, objectHeader);
                }
            } else if (format == 4) { // indexable weak fields
                object = new WeakVariablePointersObject(image, objectHeader);
            } else if (format == 5) { // fixed weak fields
                throw SqueakException.create("Ephemerons not (yet) supported");
            } else if (format <= 8) {
                assert false : "Should never happen (unused format)";
            } else if (format == 9) { // 64-bit integers
                object = NativeObject.newNativeLongs(this);
            } else if (format <= 11) { // 32-bit integers
                if (classIndex == image.floatClassIndex) {
                    object = FloatObject.newFrom(this);
                } else {
                    object = NativeObject.newNativeInts(this);
                }
            } else if (format <= 15) { // 16-bit integers
                object = NativeObject.newNativeShorts(this);
            } else if (format <= 23) { // bytes
                if (classIndex == reader.largePositiveIntegerClassIndex || classIndex == reader.largeNegativeIntegerClassIndex) {
                    object = new LargeIntegerObject(image, objectHeader, getBytes()).reduceIfPossible();
                } else {
                    object = NativeObject.newNativeBytes(this);
                }
            } else if (format <= 31) { // compiled methods
                object = new CompiledCodeObject(image, objectHeader);
            }
        }
        return object;
    }

    public void setObject(final Object value) {
        assert object == null;
        object = value;
    }

    public boolean isNil() {
// assert object != null;
        return object == NilObject.SINGLETON;
    }

    public long getObjectHeader() {
        return objectHeader;
    }

    private int getFormat() {
        final int format = ObjectHeader.getFormat(objectHeader);
        assert 0 <= format && format != 6 && format != 8 && format <= 31 : "Unexpected format";
        return format;
    }

    public int getHash() {
        return ObjectHeader.getHash(objectHeader);
    }

    public ClassObject getSqueakClass() {
        if (sqClass == null) {
            sqClass = getClassChunk().asClassObject();
        }
        return sqClass;
    }

    public SqueakImageContext getImage() {
        return image;
    }

    public SqueakImageReader getReader() {
        return reader;
    }

    public int getPosition() {
        return position;
    }

    public SqueakImageChunk getClassChunk() {
        final int classIndex = getClassIndex();
        final int majorIdx = SqueakImageConstants.majorClassIndexOf(classIndex);
        final int minorIdx = SqueakImageConstants.minorClassIndexOf(classIndex);
        final SqueakImageChunk classTablePage = reader.getChunk(reader.hiddenRootsChunk.getWord(majorIdx));
        final SqueakImageChunk classChunk = reader.getChunk(classTablePage.getWord(minorIdx));
        assert classChunk != null : "Unable to find class chunk.";
        return classChunk;
    }

    public void setSqClass(final ClassObject baseSqueakObject) {
        sqClass = baseSqueakObject;
    }

    public Object[] getPointers() {
        if (pointers == null) {
            final int length = getWordSize();
            pointers = new Object[length];
            for (int i = 0; i < length; i++) {
                pointers[i] = decodePointer(getWord(i));
            }
        }
        return pointers;
    }

    public Object[] getPointers(final int end) {
        if (pointers == null) {
            pointers = new Object[end];
            for (int i = 0; i < end; i++) {
                pointers[i] = decodePointer(getWord(i));
            }
        }
        return pointers;
    }

    private Object decodePointer(final long ptr) {
        switch ((int) (ptr & 7)) {
            case SqueakImageConstants.OBJECT_TAG:
                final SqueakImageChunk chunk = reader.getChunk(ptr);
                if (chunk == null) {
                    logBogusPointer(ptr);
                    return ptr >>> SqueakImageConstants.NUM_TAG_BITS;
                } else {
                    assert bytes != null : "Must not be an ignored object";
                    return chunk.asObject();
                }
            case SqueakImageConstants.SMALL_INTEGER_TAG: // SmallInteger
                return ptr >> SqueakImageConstants.NUM_TAG_BITS;
            case SqueakImageConstants.CHARACTER_TAG: // Character
                return CharacterObject.valueOf(ptr >> SqueakImageConstants.NUM_TAG_BITS);
            case SqueakImageConstants.SMALL_FLOAT_TAG:
                /* SmallFloat (see Spur64BitMemoryManager>>#smallFloatBitsOf:). */
                long valueWithoutTag = ptr >>> SqueakImageConstants.NUM_TAG_BITS;
                if (valueWithoutTag > 1) {
                    valueWithoutTag += SqueakImageConstants.SMALL_FLOAT_TAG_BITS_MASK;
                }
                final double value = Double.longBitsToDouble(Long.rotateRight(valueWithoutTag, 1));
                assert Double.isFinite(value) : "SmallFloats must be finite";
                return value;
            default:
                throw SqueakException.create("Unexpected pointer");
        }
    }

    @TruffleBoundary
    private void logBogusPointer(final long ptr) {
        image.getError().println("Bogus pointer: " + ptr + ". Treating as smallint.");
    }

    public int getClassIndex() {
        return ObjectHeader.getClassIndex(objectHeader);
    }

    public byte[] getBytes() {
        return bytes;
    }

    public long getWord(final int index) {
        return UnsafeUtils.getLong(bytes, index);
    }

    public int getWordSize() {
        return bytes.length / SqueakImageConstants.WORD_SIZE;
    }
}
