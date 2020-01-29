/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.image;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
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
import de.hpi.swa.graal.squeak.util.UnsafeUtils;

public final class SqueakImageChunk {
    private Object object;
    private ClassObject sqClass;
    private Object[] pointers;

    private final int classIndex;
    private final int position;

    private final SqueakImageContext image;
    private final SqueakImageReader reader;
    private final int format;
    private final int hash;
    private final byte[] bytes;

    public SqueakImageChunk(final SqueakImageReader reader,
                    final SqueakImageContext image,
                    final int format,
                    final int classIndex,
                    final int hash,
                    final int position,
                    final byte[] bytes) {
        this.reader = reader;
        this.image = image;
        this.format = format;
        this.classIndex = classIndex;
        this.hash = hash;
        this.position = position;
        this.bytes = bytes;
    }

    public static SqueakImageChunk createDummyChunk(final SqueakImageContext image, final Object[] pointers) {
        final SqueakImageChunk chunk = new SqueakImageChunk(null, image, 0, 0, 0, 0, new byte[0]);
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
            if (bytes == null) {
                assert SqueakImageReader.isHiddenObject(classIndex);
                /* Ignored object (see SqueakImageReader#ignoreObjectData) */
                return NilObject.SINGLETON;
            }
            final ClassObject squeakClass = getSqClass();
            if (format == 0) { // no fields
                object = new EmptyObject(image, hash, squeakClass);
            } else if (format == 1) { // fixed pointers
                // classes should already be instantiated at this point, check a bit
                assert squeakClass != image.metaClass && squeakClass.getSqueakClass() != image.metaClass;
                if (squeakClass.instancesAreClasses()) {
                    /*
                     * In rare cases, there are still some classes that are not in the class table
                     * for some reason (e.g. not completely removed from the system yet).
                     */
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
                    object = BlockClosureObject.createWithHash(image, hash);
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
            } else if (format <= 11) {
                if (squeakClass == image.floatClass) {
                    object = FloatObject.newFrom(this);
                } else { // 32-bit integers
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

    public void setObject(final Object value) {
        assert object == null;
        object = value;
    }

    public boolean isNil() {
        return object == NilObject.SINGLETON;
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
                return CharacterObject.valueOf((int) (ptr >> SqueakImageConstants.NUM_TAG_BITS));
            case SqueakImageConstants.SMALL_FLOAT_TAG:
                /* SmallFloat (see Spur64BitMemoryManager>>#smallFloatBitsOf:). */
                long valueWithoutTag = ptr >>> SqueakImageConstants.NUM_TAG_BITS;
                if (valueWithoutTag > 1) {
                    valueWithoutTag += SqueakImageConstants.SMALL_FLOAT_TAG_BITS_MASK;
                }
                return Double.longBitsToDouble(Long.rotateRight(valueWithoutTag, 1));
            default:
                throw SqueakException.create("Unexpected pointer");
        }
    }

    @TruffleBoundary
    private void logBogusPointer(final long ptr) {
        image.getError().println("Bogus pointer: " + ptr + ". Treating as smallint.");
    }

    public int getClassIndex() {
        return classIndex;
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
