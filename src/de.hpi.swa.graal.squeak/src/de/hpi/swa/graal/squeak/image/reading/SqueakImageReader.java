/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.image.reading;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakAbortException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObjectWithHash;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BooleanObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SPECIAL_OBJECT_TAG;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.MiscUtils;
import de.hpi.swa.graal.squeak.util.UnsafeUtils;

public final class SqueakImageReader {
    public static final long IMAGE_32BIT_VERSION = 6521;
    public static final long IMAGE_64BIT_VERSION = 68021;
    private static final int FREE_OBJECT_CLASS_INDEX_PUN = 0;
    private static final long SLOTS_MASK = 0xFF << 56;
    private static final long OVERFLOW_SLOTS = 255;
    private static final int HIDDEN_ROOTS_CHUNK_INDEX = 4;

    protected boolean is64bit = false;
    private int wordSize = 4;
    protected SqueakImageChunk hiddenRootsChunk;

    private final BufferedInputStream stream;
    private final HashMap<Long, SqueakImageChunk> chunktable = new HashMap<>(750000);
    private final SqueakImageContext image;

    private int chunkCount = 0;
    private long headerSize;
    private long oldBaseAddress;
    private long specialObjectsPointer;
    private int lastWindowSizeWord;
    private int headerFlags;
    private short maxExternalSemaphoreTableSize;
    private long firstSegmentSize;
    private int position = 0;
    private long segmentEnd;
    private long currentAddressSwizzle;

    private SqueakImageReader(final SqueakImageContext image) {
        final TruffleFile truffleFile = image.env.getTruffleFile(image.getImagePath());
        if (!truffleFile.isRegularFile()) {
            if (image.getImagePath().isEmpty()) {
                throw SqueakAbortException.create(MiscUtils.format("An image must be provided via `%s.ImagePath`.", SqueakLanguageConfig.ID));
            } else {
                throw SqueakAbortException.create(MiscUtils.format("Image at '%s' does not exist.", image.getImagePath()));
            }
        }
        BufferedInputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(truffleFile.newInputStream());
        } catch (final IOException e) {
            if (!image.isTesting()) {
                throw SqueakAbortException.create(e);
            }
        }
        stream = inputStream;
        this.image = image;
    }

    public static void load(final SqueakImageContext image) {
        new SqueakImageReader(image).run();
    }

    private Object run() {
        if (stream == null && image.isTesting()) {
            return null;
        }
        final long start = currentTimeMillis();
        readHeader();
        readBody();
        initObjects();
        clearChunktable();
        image.initializePrimitives();
        image.printToStdOut("Image loaded in", currentTimeMillis() - start + "ms.");
        return image.getSqueakImage();
    }

    private static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    private void clearChunktable() {
        chunktable.clear();
    }

    @TruffleBoundary
    private void readBytes(final byte[] bytes, final int length) {
        try {
            stream.read(bytes, 0, length);
        } catch (final IOException e) {
            throw SqueakAbortException.create("Unable to read next bytes:", e.getMessage());
        }
    }

    private long nextWord() {
        if (is64bit) {
            return nextLong();
        } else {
            return nextInt();
        }
    }

    private byte[] nextBytes(final int count) {
        final byte[] bytes = new byte[count];
        readBytes(bytes, count);
        position += count;
        return bytes;
    }

    private short nextShort() {
        final byte[] bytes = new byte[2];
        readBytes(bytes, 2);
        position += 2;
        return UnsafeUtils.getShort(bytes, 0);
    }

    private int nextInt() {
        final byte[] bytes = new byte[4];
        readBytes(bytes, 4);
        position += 4;
        return UnsafeUtils.getInt(bytes, 0);
    }

    private long nextLong() {
        final byte[] bytes = new byte[8];
        readBytes(bytes, 8);
        position += 8;
        return UnsafeUtils.getLong(bytes, 0);
    }

    @TruffleBoundary
    private void skipBytes(final long count) {
        try {
            position += stream.skip(count);
        } catch (final IOException e) {
            throw SqueakAbortException.create("Unable to skip next bytes:", e);
        }
    }

    private void readVersion() {
        final long version = nextWord();
        assert version == IMAGE_32BIT_VERSION || version == IMAGE_64BIT_VERSION : "Image not supported: " + version;
        if (version == IMAGE_64BIT_VERSION) {
            // nextWord(); // magic2
            wordSize = 8;
            is64bit = true;
        } else {
            wordSize = 4;
        }
    }

    private void readBaseHeader() {
        headerSize = nextInt();
        nextWord(); // "length of heap in file"
        oldBaseAddress = nextWord();
        specialObjectsPointer = nextWord();
        nextWord(); // 1 word last used hash
        lastWindowSizeWord = (int) nextWord();
        headerFlags = (int) nextWord();
        nextInt(); // extraVMMemory
    }

    private void readSpurHeader() {
        nextShort(); // numStackPages
        nextShort(); // cogCodeSize
        assert position == (is64bit ? 64 : 40) : "Wrong position";
        nextInt(); // edenBytes
        maxExternalSemaphoreTableSize = nextShort();
        nextShort(); // unused, realign to word boundary
        assert position == (is64bit ? 72 : 48) : "Wrong position";
        firstSegmentSize = nextWord();
        nextWord(); // freeOldSpace
    }

    private void readHeader() {
        readVersion();
        readBaseHeader();
        readSpurHeader();
        image.flags.initialize(is64bit, headerFlags, lastWindowSizeWord, maxExternalSemaphoreTableSize);
        skipToBody();
    }

    private void skipToBody() {
        skipBytes(headerSize - position);
    }

    private void readBody() {
        position = 0;
        segmentEnd = firstSegmentSize;
        currentAddressSwizzle = oldBaseAddress;
        while (position < segmentEnd) {
            while (position < segmentEnd - 16) {
                final SqueakImageChunk chunk = readObject();
                if (chunk.classIndex != FREE_OBJECT_CLASS_INDEX_PUN) {
                    putChunk(chunk);
                }
            }
            final long bridge = nextLong();
            long bridgeSpan = 0;
            if ((bridge & SLOTS_MASK) != 0) {
                bridgeSpan = bridge & ~SLOTS_MASK;
            }
            final long nextSegmentSize = nextLong();
            assert bridgeSpan >= 0;
            assert nextSegmentSize >= 0;
            assert position == segmentEnd;
            if (nextSegmentSize == 0) {
                break;
            }
            segmentEnd += nextSegmentSize;
            currentAddressSwizzle += bridgeSpan * wordSize;
        }
        closeStream();
    }

    @TruffleBoundary
    private void closeStream() {
        try {
            stream.close();
        } catch (final IOException e) {
            throw SqueakAbortException.create("Unable to close stream:", e);
        }
    }

    private void putChunk(final SqueakImageChunk chunk) {
        chunktable.put(chunk.pos + currentAddressSwizzle, chunk);
        if (chunkCount++ == HIDDEN_ROOTS_CHUNK_INDEX) {
            hiddenRootsChunk = chunk;
        }
    }

    private SqueakImageChunk readObject() {
        int pos = position;
        assert pos % 8 == 0 : "every object must be 64-bit aligned: " + pos % 8;
        long headerWord = nextLong();
        int size = ObjectHeaderDecoder.getNumSlots(headerWord);
        if (size == OVERFLOW_SLOTS) {
            size = (int) (headerWord & ~SLOTS_MASK);
            pos = position;
            headerWord = nextLong();
            assert ObjectHeaderDecoder.getNumSlots(headerWord) == OVERFLOW_SLOTS : "Objects with long header must have 255 in slot count";
        }
        final int classIndex = ObjectHeaderDecoder.getClassIndex(headerWord);
        final int format = ObjectHeaderDecoder.getFormat(headerWord);
        final int hash = ObjectHeaderDecoder.getHash(headerWord);
        assert size >= 0 : "Negative object size";
        assert 0 <= format && format <= 31 : "Unexpected format";
        final SqueakImageChunk chunk = new SqueakImageChunk(this, image, nextBytes(size * wordSize), format, classIndex, hash, pos);
        final int wordsFor = wordsFor(size);
        if (wordsFor > size * wordSize) {
            skipBytes(wordsFor - size * wordSize); // skip trailing alignment words
        }
        assert format != 0 || classIndex == 0 || size == 0 : "Empty objects must not have slots";
        assert checkAddressIntegrity(classIndex, format, chunk);
        return chunk;
    }

    private boolean checkAddressIntegrity(final int classIndex, final int format, final SqueakImageChunk chunk) {
        if (is64bit) {
            return true; // FIXME: temporarily disabled for 64bit.
        }
        boolean result = true;
        if (format < 10 && classIndex != FREE_OBJECT_CLASS_INDEX_PUN) {
            for (final long slot : chunk.getWords()) {
                result &= slot % 16 != 0 || slot >= oldBaseAddress;
            }
        }
        return result;
    }

    private int wordsFor(final int size) {
        if (is64bit) {
            // see Spur64BitMemoryManager>>smallObjectBytesForSlots:
            return size < 1 ? 8 : size * wordSize;
        } else {
            // see Spur32BitMemoryManager>>smallObjectBytesForSlots:
            return size <= 1 ? 8 : (size + (size & 1)) * wordSize;
        }
    }

    private SqueakImageChunk specialObjectChunk(final int idx) {
        final SqueakImageChunk specialObjectsChunk = getChunk(specialObjectsPointer);
        return getChunk(specialObjectsChunk.getWords()[idx]);
    }

    private void setPrebuiltObject(final int idx, final Object object) {
        specialObjectChunk(idx).object = object;
    }

    private void initPrebuiltConstant() {
        final SqueakImageChunk specialObjectsChunk = getChunk(specialObjectsPointer);
        specialObjectsChunk.object = image.specialObjectsArray;

        // first we find the Metaclass, we need it to correctly instantiate
        // those classes that do not have any instances. Metaclass always
        // has instances, and all instances of Metaclass have their singleton
        // Behavior instance, so these are all correctly initialized already
        final SqueakImageChunk sqArray = specialObjectsChunk.getClassChunk();
        final SqueakImageChunk sqArrayClass = sqArray.getClassChunk();
        final SqueakImageChunk sqMetaclass = sqArrayClass.getClassChunk();
        sqMetaclass.object = image.metaClass;

        // also cache nil, true, and false classes
        specialObjectChunk(SPECIAL_OBJECT.NIL_OBJECT).getClassChunk().object = image.nilClass;
        specialObjectChunk(SPECIAL_OBJECT.FALSE_OBJECT).getClassChunk().object = image.falseClass;
        specialObjectChunk(SPECIAL_OBJECT.TRUE_OBJECT).getClassChunk().object = image.trueClass;

        setPrebuiltObject(SPECIAL_OBJECT.NIL_OBJECT, NilObject.SINGLETON);
        setPrebuiltObject(SPECIAL_OBJECT.FALSE_OBJECT, BooleanObject.FALSE);
        setPrebuiltObject(SPECIAL_OBJECT.TRUE_OBJECT, BooleanObject.TRUE);
        setPrebuiltObject(SPECIAL_OBJECT.SCHEDULER_ASSOCIATION, image.schedulerAssociation);
        setPrebuiltObject(SPECIAL_OBJECT.CLASS_BITMAP, image.bitmapClass);
        setPrebuiltObject(SPECIAL_OBJECT.CLASS_SMALLINTEGER, image.smallIntegerClass);
        setPrebuiltObject(SPECIAL_OBJECT.CLASS_STRING, image.byteStringClass);
        setPrebuiltObject(SPECIAL_OBJECT.CLASS_ARRAY, image.arrayClass);
        setPrebuiltObject(SPECIAL_OBJECT.SMALLTALK_DICTIONARY, image.smalltalk);
        setPrebuiltObject(SPECIAL_OBJECT.CLASS_FLOAT, image.floatClass);
        if (specialObjectChunk(SPECIAL_OBJECT.CLASS_TRUFFLE_OBJECT).object != NilObject.SINGLETON) {
            setPrebuiltObject(SPECIAL_OBJECT.CLASS_TRUFFLE_OBJECT, image.initializeTruffleObject());
        }
        setPrebuiltObject(SPECIAL_OBJECT.CLASS_METHOD_CONTEXT, image.methodContextClass);
        setPrebuiltObject(SPECIAL_OBJECT.CLASS_POINT, image.pointClass);
        setPrebuiltObject(SPECIAL_OBJECT.CLASS_LARGE_POSITIVE_INTEGER, image.largePositiveIntegerClass);
        setPrebuiltObject(SPECIAL_OBJECT.CLASS_MESSAGE, image.messageClass);
        setPrebuiltObject(SPECIAL_OBJECT.CLASS_COMPILED_METHOD, image.compiledMethodClass);
        setPrebuiltObject(SPECIAL_OBJECT.CLASS_SEMAPHORE, image.semaphoreClass);
        setPrebuiltObject(SPECIAL_OBJECT.CLASS_CHARACTER, image.characterClass);
        setPrebuiltObject(SPECIAL_OBJECT.SELECTOR_DOES_NOT_UNDERSTAND, image.doesNotUnderstand);
        setPrebuiltObject(SPECIAL_OBJECT.SELECTOR_MUST_BE_BOOLEAN, image.mustBeBooleanSelector);
        setPrebuiltObject(SPECIAL_OBJECT.CLASS_BYTE_ARRAY, image.byteArrayClass);
        setPrebuiltObject(SPECIAL_OBJECT.CLASS_PROCESS, image.processClass);
        setPrebuiltObject(SPECIAL_OBJECT.CLASS_BLOCK_CLOSURE, image.blockClosureClass);
        setPrebuiltObject(SPECIAL_OBJECT.EXTERNAL_OBJECTS_ARRAY, image.externalObjectsArray);
        setPrebuiltObject(SPECIAL_OBJECT.CLASS_LARGE_NEGATIVE_INTEGER, image.largeNegativeIntegerClass);
        setPrebuiltObject(SPECIAL_OBJECT.SELECTOR_ABOUT_TO_RETURN, image.aboutToReturnSelector);
        setPrebuiltObject(SPECIAL_OBJECT.SELECTOR_RUN_WITHIN, image.runWithInSelector);
        setPrebuiltObject(SPECIAL_OBJECT.PRIM_ERR_TABLE_INDEX, image.primitiveErrorTable);
        setPrebuiltObject(SPECIAL_OBJECT.SPECIAL_SELECTORS, image.specialSelectors);
    }

    private void initObjects() {
        initPrebuiltConstant();
        fillInClassObjects();
        fillInObjects();
        fillInContextObjects();
        fillInSmallFloatClass();
    }

    /**
     * Fill in classes and ensure instances of Behavior and its subclasses use {@link ClassObject}.
     */
    private void fillInClassObjects() {
        /** Find all metaclasses and instantiate their singleton instances as class objects. */
        for (final long classtablePtr : hiddenRootsChunk.getWords()) {
            if (getChunk(classtablePtr) != null) {
                for (final long potentialClassPtr : getChunk(classtablePtr).getWords()) {
                    if (potentialClassPtr == 0) {
                        continue;
                    }
                    final SqueakImageChunk metaClass = getChunk(potentialClassPtr);
                    if (metaClass != null && metaClass.getSqClass() == image.metaClass) {
                        final long[] data = metaClass.getWords();
                        final SqueakImageChunk classInstance = getChunk(data[data.length - 1]);
                        assert data.length == 6;
                        final ClassObject metaClassObject = metaClass.asClassObject(image.metaClass);
                        metaClassObject.setInstancesAreClasses();
                        classInstance.asClassObject(metaClassObject);
                    }
                }
            }
        }

        /** Fill in metaClass. */
        final SqueakImageChunk specialObjectsChunk = getChunk(specialObjectsPointer);
        final SqueakImageChunk sqArray = specialObjectsChunk.getClassChunk();
        final SqueakImageChunk sqArrayClass = sqArray.getClassChunk();
        final SqueakImageChunk sqMetaclass = sqArrayClass.getClassChunk();
        image.metaClass.fillin(sqMetaclass);

        /**
         * Walk over all classes again and ensure instances of all subclasses of ClassDescriptions
         * are {@link ClassObject}s.
         */
        final HashSet<ClassObject> inst = new HashSet<>();
        final ClassObject classDescriptionClass = image.metaClass.getSuperclassOrNull();
        classDescriptionClass.setInstancesAreClasses();
        inst.add(classDescriptionClass);
        for (final long classtablePtr : hiddenRootsChunk.getWords()) {
            if (getChunk(classtablePtr) != null) {
                for (final long potentialClassPtr : getChunk(classtablePtr).getWords()) {
                    if (potentialClassPtr == 0) {
                        continue;
                    }
                    final SqueakImageChunk metaClass = getChunk(potentialClassPtr);
                    if (metaClass != null && metaClass.getSqClass() == image.metaClass) {
                        final long[] data = metaClass.getWords();
                        final SqueakImageChunk classInstance = getChunk(data[data.length - 1]);
                        assert data.length == 6;
                        final ClassObject metaClassObject = metaClass.asClassObject(image.metaClass);
                        final ClassObject classObject = classInstance.asClassObject(metaClassObject);
                        classObject.fillin(classInstance);
                        if (inst.contains(classObject.getSuperclassOrNull())) {
                            inst.add(classObject);
                            classObject.setInstancesAreClasses();
                        }
                    }
                }
            }
        }
        assert image.metaClass.instancesAreClasses();

        /** Finally, ensure instances of Behavior are {@link ClassObject}s. */
        final ClassObject behaviorClass = classDescriptionClass.getSuperclassOrNull();
        behaviorClass.setInstancesAreClasses();
    }

    private void fillInObjects() {
        for (final SqueakImageChunk chunk : chunktable.values()) {
            final Object chunkObject = chunk.asObject();
            if (chunkObject instanceof AbstractSqueakObjectWithHash) {
                final AbstractSqueakObjectWithHash obj = (AbstractSqueakObjectWithHash) chunkObject;
                if (obj.needsSqueakClass()) {
                    obj.setSqueakClass(chunk.getSqClass());
                }
                if (obj.needsSqueakHash()) {
                    obj.setSqueakHash(chunk.getHash());
                }
                obj.fillin(chunk);
            }
        }
    }

    private void fillInContextObjects() {
        for (final SqueakImageChunk chunk : chunktable.values()) {
            final Object chunkObject = chunk.asObject();
            if (chunkObject instanceof ContextObject) {
                final ContextObject contextObject = (ContextObject) chunkObject;
                assert !contextObject.hasTruffleFrame();
                contextObject.fillinContext(chunk);
            }
        }
    }

    private void fillInSmallFloatClass() {
        final ArrayObject classTableFirstPage = (ArrayObject) getChunk(hiddenRootsChunk.getWords()[0]).asObject();
        final ArrayObjectReadNode arrayReadNode = ArrayObjectReadNode.getUncached();
        assert arrayReadNode.execute(classTableFirstPage, SPECIAL_OBJECT_TAG.SMALL_INTEGER) == image.smallIntegerClass;
        assert arrayReadNode.execute(classTableFirstPage, SPECIAL_OBJECT_TAG.CHARACTER) == image.characterClass;
        if (image.flags.is64bit()) {
            final Object smallFloatClassOrNil = arrayReadNode.execute(classTableFirstPage, SPECIAL_OBJECT_TAG.SMALL_FLOAT);
            image.setSmallFloat((ClassObject) smallFloatClassOrNil);
        } else {
            assert image.smallFloatClass != null : "smallFloatClass was not found when filling in objects of a 32bit image.";
        }
    }

    public SqueakImageChunk getChunk(final long ptr) {
        return chunktable.get(ptr);
    }

    /**
     * Object Header Specification (see SpurMemoryManager).
     *
     * <pre>
     *  MSB:  | 8: numSlots       | (on a byte boundary)
     *        | 2 bits            |   (msb,lsb = {isMarked,?})
     *        | 22: identityHash  | (on a word boundary)
     *        | 3 bits            |   (msb <-> lsb = {isGrey,isPinned,isRemembered}
     *        | 5: format         | (on a byte boundary)
     *        | 2 bits            |   (msb,lsb = {isImmutable,?})
     *        | 22: classIndex    | (on a word boundary) : LSB
     * </pre>
     */
    private static final class ObjectHeaderDecoder {
        private static int getClassIndex(final long headerWord) {
            return MiscUtils.bitSplit(headerWord, 0, 22);
        }

        private static int getFormat(final long headerWord) {
            return MiscUtils.bitSplit(headerWord, 24, 5);
        }

        private static int getHash(final long headerWord) {
            return MiscUtils.bitSplit(headerWord, 32, 22);
        }

        private static int getNumSlots(final long headerWord) {
            return MiscUtils.bitSplit(headerWord, 56, 8);
        }
    }
}
