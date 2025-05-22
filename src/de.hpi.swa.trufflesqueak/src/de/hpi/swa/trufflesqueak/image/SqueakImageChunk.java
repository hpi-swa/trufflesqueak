/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
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
import de.hpi.swa.trufflesqueak.model.EphemeronObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.model.WeakVariablePointersObject;
import de.hpi.swa.trufflesqueak.util.VarHandleUtils;

public final class SqueakImageChunk {
    private final SqueakImageReader reader;
    private final long header;
    private final int position;
    private final byte[] bytes;

    private Object object;
    private ClassObject squeakClass;

    public SqueakImageChunk(final SqueakImageReader reader, final long header, final int position, final byte[] bytes) {
        this.reader = reader;
        this.header = header;
        this.position = position;
        this.bytes = bytes;
        if (bytes == null) { /* Ignored object (see SqueakImageReader#ignoreObjectData) */
            assert SqueakImageReader.isHiddenObject(getClassIndex());
            object = NilObject.SINGLETON;
        }
    }

    public ClassObject asClassObject(final ClassObject metaClassObject) {
        if (object == null) {
            assert getFormat() == 1;
            object = new ClassObject(getImage(), header, metaClassObject);
        } else if (object == NilObject.SINGLETON) {
            return null;
        }
        return (ClassObject) object;
    }

    public Object asObject() {
        if (object == null) {
            final int format = getFormat();
            final ClassObject classObject = getSqueakClass();
            if (format == 0) { // no fields
                object = new EmptyObject(header, classObject);
            } else if (format == 1) { // fixed pointers
                if (classObject.instancesAreClasses()) {
                    /*
                     * In rare cases, there are still some classes that are not in the class table
                     * for some reason (e.g. not completely removed from the system yet).
                     */
                    object = new ClassObject(getImage(), header, classObject);
                } else {
                    // classes should already be instantiated at this point, check a bit
                    assert classObject != getImage().metaClass && classObject.getSqueakClass() != getImage().metaClass;
                    object = new PointersObject(header, classObject);
                }
            } else if (format == 2) { // indexable fields
                object = new ArrayObject(header, classObject);
            } else if (format == 3) { // fixed and indexable fields
                if (classObject == getImage().methodContextClass) {
                    object = ContextObject.createWithHeader(getImage(), header);
                } else if (getImage().isBlockClosureClass(classObject) || getImage().isFullBlockClosureClass(classObject)) {
                    object = BlockClosureObject.createWithHeaderAndClass(header, classObject);
                } else {
                    object = new VariablePointersObject(header, classObject);
                }
            } else if (format == 4) { // indexable weak fields
                object = new WeakVariablePointersObject(getImage(), header, classObject);
            } else if (format == 5) { // fixed fields, special notification
                object = new EphemeronObject(getImage(), header, classObject);
            } else if (format <= 8) {
                assert false : "Should never happen (unused format)";
            } else if (format == 9) { // 64-bit integers
                object = NativeObject.newNativeLongs(this);
            } else if (format <= 11) { // 32-bit integers
                if (classObject == getImage().floatClass) {
                    object = FloatObject.newFrom(this);
                } else {
                    object = NativeObject.newNativeInts(this);
                }
            } else if (format <= 15) { // 16-bit integers
                object = NativeObject.newNativeShorts(this);
            } else if (format <= 23) { // bytes
                if (classObject == getImage().largePositiveIntegerClass || classObject == getImage().largeNegativeIntegerClass) {
                    object = new LargeIntegerObject(getImage(), header, classObject, getBytes()).reduceIfPossible();
                } else if (classObject == getImage().getWideStringClass() || classObject == getImage().byteStringClass) {
                    object = NativeObject.newNativeString(this);
                } else {
                    object = NativeObject.newNativeBytes(this);
                }
            } else if (format <= 31) { // compiled methods
                object = new CompiledCodeObject(header, classObject);
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

    private int getFormat() {
        final int format = ObjectHeader.getFormat(header);
        assert 0 <= format && format != 6 && format != 8 && format <= 31 : "Unexpected format";
        return format;
    }

    public int getHash() {
        return ObjectHeader.getHash(header);
    }

    public long getHeader() {
        return header;
    }

    public ClassObject getSqueakClass() {
        if (squeakClass == null) {
            squeakClass = getClassChunk().asClassObject(null);
        }
        return squeakClass;
    }

    public SqueakImageContext getImage() {
        return reader.image;
    }

    public int getPosition() {
        return position;
    }

    public SqueakImageChunk getClassChunk() {
        final int classIndex = getClassIndex();
        final int majorIndex = SqueakImageConstants.majorClassIndexOf(classIndex);
        final int minorIndex = SqueakImageConstants.minorClassIndexOf(classIndex);
        final SqueakImageChunk classTablePage = reader.chunkMap.get(reader.hiddenRootsChunk.getWord(majorIndex));
        assert !classTablePage.isNil() : "Class page does not exist";
        final SqueakImageChunk classChunk = reader.chunkMap.get(classTablePage.getWord(minorIndex));
        assert classChunk != null : "Unable to find class chunk.";
        return classChunk;
    }

    public void setSqueakClass(final ClassObject baseSqueakObject) {
        squeakClass = baseSqueakObject;
    }

    public Object getPointer(final int index) {
        return decodePointer(getWord(index));
    }

    public Object[] getPointers(final int start) {
        return getPointers(start, getWordSize());
    }

    public Object[] getPointers(final int start, final int end) {
        final int numObjects = end - start;
        final Object[] result = new Object[numObjects];
        for (int i = 0; i < numObjects; i++) {
            result[i] = getPointer(start + i);
        }
        return result;
    }

    private Object decodePointer(final long ptr) {
        switch ((int) (ptr & 7)) {
            case SqueakImageConstants.OBJECT_TAG:
                final SqueakImageChunk chunk = reader.chunkMap.get(ptr);
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
        getImage().getError().println("Bogus pointer: " + ptr + ". Treating as smallint.");
    }

    public int getClassIndex() {
        return ObjectHeader.getClassIndex(header);
    }

    public byte[] getBytes() {
        return bytes;
    }

    public long getWord(final int index) {
        return VarHandleUtils.getLong(bytes, index);
    }

    public int getWordSize() {
        return bytes.length / SqueakImageConstants.WORD_SIZE;
    }
}
