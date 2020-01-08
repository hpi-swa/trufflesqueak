/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.image.reading;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLogger;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakAbortException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.SqueakImageFlags;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObjectWithHash;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BooleanObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.CLASS;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.METACLASS;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SPECIAL_OBJECT_TAG;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageOptions;
import de.hpi.swa.graal.squeak.util.MiscUtils;
import de.hpi.swa.graal.squeak.util.UnsafeUtils;

public final class SqueakImageReader {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, SqueakImageReader.class);

    private static final int CLASS_INDEX_FIELD_WIDTH = 22; /* 22-bit class mask => ~ 4M classes */
    private static final int CLASS_TABLE_MAJOR_INDEX_SHIFT = 10;
    private static final int CLASS_TABLE_PAGE_SIZE = 1 << CLASS_TABLE_MAJOR_INDEX_SHIFT;
    /* Answer the number of slots for class table pages in the hidden root object. */
    private static final int CLASS_TABLE_ROOT_SLOTS = 1 << CLASS_INDEX_FIELD_WIDTH - CLASS_TABLE_MAJOR_INDEX_SHIFT;
    /* Answer the number of extra root slots in the root of the hidden root object. */
    private static final int HIDDEN_ROOT_SLOTS = 8;
    private static final int NUM_FREE_LISTS = 64;
    private static final int OBJ_STACK_PAGE_SLOTS = 4092;
    private static final long OVERFLOW_SLOTS = 255;
    private static final long SLOTS_MASK = 0xFF << 56;

    // CLASS INDEX PUNS
    private static final int ARRAY_CLASS_INDEX_PUN = 16;
    private static final int FREE_OBJECT_CLASS_INDEX_PUN = 0;
    private static final int LAST_CLASS_INDEX_PUN = 31;
    private static final int WORD_SIZE_CLASS_INDEX_PUN = 19;

    protected SqueakImageChunk hiddenRootsChunk;

    private final BufferedInputStream stream;
    private final HashMap<Long, SqueakImageChunk> chunktable = new HashMap<>(750000);
    private final SqueakImageContext image;
    private final byte[] byteArrayBuffer = new byte[8];
    private final Map<PointersObject, ContextObject> suspendedContexts = new HashMap<>();

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

    private SqueakImageChunk freePageList = null;

    private SqueakImageReader(final SqueakImageContext image) {
        final TruffleFile truffleFile = image.env.getPublicTruffleFile(image.getImagePath());
        if (!truffleFile.isRegularFile()) {
            if (image.getImagePath().isEmpty()) {
                throw SqueakAbortException.create(MiscUtils.format("An image must be provided via `%s.%s`.", SqueakLanguageConfig.ID, SqueakLanguageOptions.IMAGE_PATH));
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

    @TruffleBoundary
    public static void load(final SqueakImageContext image) {
        new SqueakImageReader(image).run();
    }

    private Object run() {
        if (stream == null && image.isTesting()) {
            return null;
        }
        SqueakImageContext.initializeBeforeLoadingImage();
        final long start = System.currentTimeMillis();
        readHeader();
        try {
            readBody();
        } finally {
            closeStream();
        }
        initObjects();
        image.printToStdOut("Image loaded in", System.currentTimeMillis() - start + "ms.");
        initializeSuspendedContexts();
        image.initializeAfterLoadingImage();
        return image.getSqueakImage();
    }

    public Map<PointersObject, ContextObject> getSuspendedContexts() {
        return suspendedContexts;
    }

    private void readBytes(final byte[] bytes, final int length) {
        try {
            final int readBytes = stream.read(bytes, 0, length);
            assert readBytes == length : "Failed to read bytes";
        } catch (final IOException e) {
            throw SqueakAbortException.create("Unable to read next bytes:", e.getMessage());
        }
    }

    private long nextWord() {
        return nextLong();
    }

    private short nextShort() {
        readBytes(byteArrayBuffer, 2);
        position += 2;
        return UnsafeUtils.getShort(byteArrayBuffer, 0);
    }

    private int nextInt() {
        readBytes(byteArrayBuffer, 4);
        position += 4;
        return UnsafeUtils.getInt(byteArrayBuffer, 0);
    }

    private long nextLong() {
        readBytes(byteArrayBuffer, 8);
        position += 8;
        return UnsafeUtils.getLong(byteArrayBuffer, 0);
    }

    private byte[] nextObjectData(final int size, final int format) {
        final int paddedObjectSize = size * SqueakImageFlags.WORD_SIZE;
        final int padding = calculateObjectPadding(format);
        final int dataSize = paddedObjectSize - padding;
        final byte[] bytes = new byte[dataSize];
        if (size == 0) {
            skipBytes(SqueakImageFlags.WORD_SIZE); // skip trailing alignment word
            return bytes;
        }
        readBytes(bytes, dataSize);
        try {
            final long skipped = stream.skip(padding);
            assert skipped == padding : "Failed to skip padding bytes";
        } catch (final IOException e) {
            throw SqueakAbortException.create("Unable to skip next bytes:", e);
        }
        position += paddedObjectSize;
        return bytes;
    }

    private void skipBytes(final long count) {
        long pending = count;
        try {
            while (pending > 0) {
                final long skipped = stream.skip(pending);
                assert skipped > 0 : "Nothing skipped, reached EOF?";
                pending -= skipped;
            }
        } catch (final IOException e) {
            throw SqueakAbortException.create("Unable to skip next bytes:", e);
        }
        position += count;
    }

    private void readVersion() {
        final long version = nextInt();
        if (version != SqueakImageFlags.IMAGE_FORMAT) {
            throw SqueakAbortException.create(MiscUtils.format("Image format %s not supported. Please supply a 64bit Spur image (format %s).", version, SqueakImageFlags.IMAGE_FORMAT));
        }
        // nextWord(); // magic2
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
        assert position == 64 : "Wrong position";
        nextInt(); // edenBytes
        maxExternalSemaphoreTableSize = nextShort();
        nextShort(); // unused, realign to word boundary
        assert position == 72 : "Wrong position";
        firstSegmentSize = nextWord();
        nextWord(); // freeOldSpace
    }

    private void readHeader() {
        readVersion();
        readBaseHeader();
        readSpurHeader();
        image.flags.initialize(headerFlags, lastWindowSizeWord, maxExternalSemaphoreTableSize);
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
                if (chunk != null) {
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
            currentAddressSwizzle += bridgeSpan * SqueakImageFlags.WORD_SIZE;
        }
    }

    private void closeStream() {
        try {
            stream.close();
        } catch (final IOException e) {
            throw SqueakAbortException.create("Unable to close stream:", e);
        }
    }

    private void putChunk(final SqueakImageChunk chunk) {
        chunktable.put(chunk.getPosition() + currentAddressSwizzle, chunk);
    }

    private SqueakImageChunk readObject() {
        int pos = position;
        assert pos % 8 == 0 : "every object must be 64-bit aligned: " + pos % 8;
        long headerWord = nextLong();
        int numSlots = ObjectHeaderDecoder.getNumSlots(headerWord);
        if (numSlots == OVERFLOW_SLOTS) {
            numSlots = (int) (headerWord & ~SLOTS_MASK);
            pos = position;
            headerWord = nextLong();
            assert ObjectHeaderDecoder.getNumSlots(headerWord) == OVERFLOW_SLOTS : "Objects with long header must have 255 in slot count";
        }
        final int size = numSlots;
        assert size >= 0 : "Negative object size";
        final int classIndex = ObjectHeaderDecoder.getClassIndex(headerWord);
        final int format = ObjectHeaderDecoder.getFormat(headerWord);
        assert 0 <= format && format <= 31 : "Unexpected format";
        assert format != 0 || classIndex == 0 || size == 0 : "Empty objects must not have slots";
        final int hash = ObjectHeaderDecoder.getHash(headerWord);
        final byte[] objectData;
        if (ignoreObjectData(headerWord, classIndex, size)) {
            /* Skip some hidden objects for performance reasons. */
            objectData = null;
            LOG.log(Level.FINE, () -> "classIdx: " + classIndex + ", size: " + size + ", format: " + format + ", hash: " + hash);
            skipBytes(size * SqueakImageFlags.WORD_SIZE);
        } else {
            objectData = nextObjectData(size, format);
        }
        final SqueakImageChunk chunk = new SqueakImageChunk(this, image, format, classIndex, hash, pos, objectData);
        if (isHiddenObject(classIndex)) {
            if (freePageList == null) {
                assert classIndex == WORD_SIZE_CLASS_INDEX_PUN && size == NUM_FREE_LISTS;
                freePageList = chunk; /* First hidden object. */
            } else if (hiddenRootsChunk == null) {
                assert classIndex == ARRAY_CLASS_INDEX_PUN && size == CLASS_TABLE_ROOT_SLOTS + HIDDEN_ROOT_SLOTS : "hiddenRootsObj has unexpected size";
                hiddenRootsChunk = chunk; /* Seconds hidden object. */
            }
        }
        assert checkAddressIntegrity(classIndex, format, chunk);
        return chunk;
    }

    protected static boolean ignoreObjectData(final long headerWord, final int classIndex, final int size) {
        return isFreeObject(classIndex) || isObjectStack(classIndex, size) || isHiddenObject(classIndex) && ObjectHeaderDecoder.isPinned(headerWord);
    }

    protected static boolean isHiddenObject(final int classIndex) {
        return classIndex <= LAST_CLASS_INDEX_PUN;
    }

    protected static boolean isFreeObject(final int classIndex) {
        return classIndex == FREE_OBJECT_CLASS_INDEX_PUN;
    }

    protected static boolean isObjectStack(final int classIndex, final int size) {
        return classIndex == WORD_SIZE_CLASS_INDEX_PUN && size == OBJ_STACK_PAGE_SLOTS;
    }

    @SuppressWarnings("unused")
    private static boolean checkAddressIntegrity(final int classIndex, final int format, final SqueakImageChunk chunk) {
        // boolean result = true;
        // if (format < 10 && classIndex != FREE_OBJECT_CLASS_INDEX_PUN) {
        // for (final long slot : chunk.getWords()) {
        // result &= slot % 16 != 0 || slot >= oldBaseAddress;
        // }
        // }
        return true; // FIXME: temporarily disabled (used to work for 32bit).
    }

    private SqueakImageChunk specialObjectChunk(final SqueakImageChunk specialObjectsChunk, final int idx) {
        return getChunk(specialObjectsChunk.getWord(idx));
    }

    private void setPrebuiltObject(final SqueakImageChunk specialObjectsChunk, final int idx, final Object object) {
        specialObjectChunk(specialObjectsChunk, idx).setObject(object);
    }

    private void initPrebuiltConstant() {
        final SqueakImageChunk specialChunk = getChunk(specialObjectsPointer);
        specialChunk.setObject(image.specialObjectsArray);

        // first we find the Metaclass, we need it to correctly instantiate
        // those classes that do not have any instances. Metaclass always
        // has instances, and all instances of Metaclass have their singleton
        // Behavior instance, so these are all correctly initialized already
        final SqueakImageChunk sqArray = specialChunk.getClassChunk();
        final SqueakImageChunk sqArrayClass = sqArray.getClassChunk();
        final SqueakImageChunk sqMetaclass = sqArrayClass.getClassChunk();
        sqMetaclass.setObject(image.metaClass);

        // also cache nil, true, and false classes
        specialObjectChunk(specialChunk, SPECIAL_OBJECT.NIL_OBJECT).getClassChunk().setObject(image.nilClass);
        specialObjectChunk(specialChunk, SPECIAL_OBJECT.FALSE_OBJECT).getClassChunk().setObject(image.falseClass);
        specialObjectChunk(specialChunk, SPECIAL_OBJECT.TRUE_OBJECT).getClassChunk().setObject(image.trueClass);

        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.NIL_OBJECT, NilObject.SINGLETON);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.FALSE_OBJECT, BooleanObject.FALSE);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.TRUE_OBJECT, BooleanObject.TRUE);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.SCHEDULER_ASSOCIATION, image.schedulerAssociation);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_BITMAP, image.bitmapClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_SMALLINTEGER, image.smallIntegerClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_STRING, image.byteStringClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_ARRAY, image.arrayClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.SMALLTALK_DICTIONARY, image.smalltalk);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_FLOAT, image.floatClass);
        if (!specialObjectChunk(specialChunk, SPECIAL_OBJECT.CLASS_FOREIGN_OBJECT).isNil()) {
            setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_FOREIGN_OBJECT, image.initializeForeignObject());
        }
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_METHOD_CONTEXT, image.methodContextClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_POINT, image.pointClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_LARGE_POSITIVE_INTEGER, image.largePositiveIntegerClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_MESSAGE, image.messageClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_COMPILED_METHOD, image.compiledMethodClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_SEMAPHORE, image.semaphoreClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_CHARACTER, image.characterClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.SELECTOR_DOES_NOT_UNDERSTAND, image.doesNotUnderstand);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.SELECTOR_CANNOT_RETURN, image.cannotReturn);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.SELECTOR_MUST_BE_BOOLEAN, image.mustBeBooleanSelector);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_BYTE_ARRAY, image.byteArrayClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_PROCESS, image.processClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_BLOCK_CLOSURE, image.blockClosureClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_LARGE_NEGATIVE_INTEGER, image.largeNegativeIntegerClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.SELECTOR_ABOUT_TO_RETURN, image.aboutToReturnSelector);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.SELECTOR_RUN_WITHIN, image.runWithInSelector);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.PRIM_ERR_TABLE_INDEX, image.primitiveErrorTable);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.SPECIAL_SELECTORS, image.specialSelectors);
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
        for (int p = 0; p < CLASS_TABLE_ROOT_SLOTS; p++) {
            final SqueakImageChunk classTablePage = getChunk(hiddenRootsChunk.getWord(p));
            if (classTablePage.getWordSize() != CLASS_TABLE_PAGE_SIZE) {
                break;
            }
            for (int i = 0; i < CLASS_TABLE_PAGE_SIZE; i++) {
                final long potentialClassPtr = classTablePage.getWord(i);
                assert potentialClassPtr != 0;
                final SqueakImageChunk metaClass = getChunk(potentialClassPtr);
                if (metaClass != null && metaClass.getSqClass() == image.metaClass) {
                    assert metaClass.getWordSize() == METACLASS.INST_SIZE;
                    final SqueakImageChunk classInstance = getChunk(metaClass.getWord(METACLASS.THIS_CLASS));
                    final ClassObject metaClassObject = metaClass.asClassObject(image.metaClass);
                    metaClassObject.setInstancesAreClasses();
                    classInstance.asClassObject(metaClassObject);
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

        for (int p = 0; p < CLASS_TABLE_ROOT_SLOTS; p++) {
            final SqueakImageChunk classTablePage = getChunk(hiddenRootsChunk.getWord(p));
            if (classTablePage.getWordSize() != CLASS_TABLE_PAGE_SIZE) {
                break;
            }
            for (int i = 0; i < CLASS_TABLE_PAGE_SIZE; i++) {
                final long potentialClassPtr = classTablePage.getWord(i);
                assert potentialClassPtr != 0;
                final SqueakImageChunk metaClass = getChunk(potentialClassPtr);
                if (metaClass != null && metaClass.getSqClass() == image.metaClass) {
                    assert metaClass.getWordSize() == METACLASS.INST_SIZE;
                    final SqueakImageChunk classInstance = getChunk(metaClass.getWord(METACLASS.THIS_CLASS));
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
        assert image.metaClass.instancesAreClasses();
        image.setByteSymbolClass(((NativeObject) image.metaClass.getOtherPointers()[CLASS.NAME]).getSqueakClass());

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
        final ArrayObject classTableFirstPage = (ArrayObject) getChunk(hiddenRootsChunk.getWord(0)).asObject();
        final ArrayObjectReadNode arrayReadNode = ArrayObjectReadNode.getUncached();
        assert arrayReadNode.execute(classTableFirstPage, SPECIAL_OBJECT_TAG.SMALL_INTEGER) == image.smallIntegerClass;
        assert arrayReadNode.execute(classTableFirstPage, SPECIAL_OBJECT_TAG.CHARACTER) == image.characterClass;
        final Object smallFloatClassOrNil = arrayReadNode.execute(classTableFirstPage, SPECIAL_OBJECT_TAG.SMALL_FLOAT);
        image.setSmallFloatClass((ClassObject) smallFloatClassOrNil);
    }

    protected SqueakImageChunk getChunk(final long ptr) {
        return chunktable.get(ptr);
    }

    public static int calculateObjectPadding(final int format) {
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
    }

    /* Set process in all ContextObjects. */
    private void initializeSuspendedContexts() {
        for (final PointersObject process : suspendedContexts.keySet()) {
            AbstractSqueakObject currentContext = suspendedContexts.get(process);
            while (currentContext != NilObject.SINGLETON) {
                final ContextObject context = (ContextObject) currentContext;
                context.setProcess(process);
                currentContext = context.getSender();
            }
        }
        suspendedContexts.clear();
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

        private static boolean isPinned(final long headerWord) {
            return (headerWord >> AbstractSqueakObjectWithHash.PINNED_BIT_SHIFT & 1) == 1;
        }
    }
}
