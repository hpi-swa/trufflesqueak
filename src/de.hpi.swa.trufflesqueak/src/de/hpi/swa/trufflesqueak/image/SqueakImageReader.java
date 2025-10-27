/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.image;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CLASS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.METACLASS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class SqueakImageReader {
    public SqueakImageChunk hiddenRootsChunk;

    public final AddressToChunkMap chunkMap = new AddressToChunkMap();
    public final SqueakImageContext image;

    private int headerSize;
    private long specialObjectsPointer;

    private SqueakImageChunk freePageList;

    public SqueakImageReader(final SqueakImageContext image) {
        this.image = image;
    }

    /*
     * Image reading happens only once per TruffleSqueak instance and should therefore be excluded
     * from Truffle compilation.
     */
    @TruffleBoundary
    public static void load(final SqueakImageContext image) {
        new SqueakImageReader(image).run();
        System.gc(); // Clean up after image loading
    }

    private void run() {
        SqueakImageContext.initializeBeforeLoadingImage();
        final long start = MiscUtils.currentTimeMillis();
        final TruffleFile truffleFile = image.env.getPublicTruffleFile(image.getImagePath());
        if (!truffleFile.isRegularFile()) {
            throw SqueakException.create(MiscUtils.format("Image at '%s' does not exist.", image.getImagePath()));
        }
        try (var channel = FileChannel.open(Path.of(image.getImagePath()), StandardOpenOption.READ)) {
            final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            readImage(buffer);
            UnsafeUtils.invokeCleaner(buffer);
        } catch (final IOException e) {
            throw SqueakException.create("Failed to read Smalltalk image:", e.getMessage());
        }
        initObjects();
        LogUtils.IMAGE.fine(() -> "Image loaded in " + (MiscUtils.currentTimeMillis() - start) + "ms.");
        image.setHiddenRoots((ArrayObject) hiddenRootsChunk.asObject());
        image.getSqueakImage();
    }

    private static void skip(final MappedByteBuffer buffer, final int numBytes) {
        buffer.position(buffer.position() + numBytes);
    }

    private void readImage(final MappedByteBuffer buffer) {
        // Read header
        image.imageFormat = buffer.getInt();
        if (!ArrayUtils.contains(SqueakImageConstants.SUPPORTED_IMAGE_FORMATS, image.imageFormat)) {
            throw SqueakException.create(MiscUtils.format("Image format %s not supported. Please supply a compatible 64bit Spur image (%s).", image.imageFormat,
                            Arrays.toString(SqueakImageConstants.SUPPORTED_IMAGE_FORMATS)));
        }

        // Base header start
        headerSize = buffer.getInt();
        final long dataSize = buffer.getLong();
        final long oldBaseAddress = buffer.getLong();
        specialObjectsPointer = buffer.getLong();
        image.setLastHash((int) buffer.getLong());
        final long snapshotScreenSize = buffer.getLong();
        final long headerFlags = buffer.getLong();
        // extraVMMemory
        buffer.getInt();

        // Spur header start
        // numStackPages
        buffer.getShort();
        // cogCodeSize
        buffer.getShort();
        assert buffer.position() == 64 : "Wrong position";
        // edenBytes
        buffer.getInt();
        final short maxExternalSemaphoreTableSize = buffer.getShort();
        // unused, realign to word boundary
        buffer.getShort();
        assert buffer.position() == 72 : "Wrong position";
        final long firstSegmentSize = buffer.getLong();
        // freeOldSpace
        buffer.getLong();
        image.flags.initialize(oldBaseAddress, headerFlags, snapshotScreenSize, maxExternalSemaphoreTableSize);

        skip(buffer, headerSize - buffer.position());
        assert buffer.position() == headerSize;

        // Read body
        long segmentEnd = headerSize + firstSegmentSize;
        long currentAddressSwizzle = oldBaseAddress;
        while (buffer.position() < segmentEnd) {
            while (buffer.position() < segmentEnd - SqueakImageConstants.IMAGE_BRIDGE_SIZE) {
                final SqueakImageChunk chunk = readObject(buffer);
                chunkMap.put(chunk.getPosition() + currentAddressSwizzle, chunk);
            }
            assert hiddenRootsChunk != null : "hiddenRootsChunk must be known from now on.";
            final long bridge = buffer.getLong();
            long bridgeSpan = 0;
            if ((bridge & SqueakImageConstants.SLOTS_MASK) != 0) {
                bridgeSpan = bridge & ~SqueakImageConstants.SLOTS_MASK;
            }
            final long nextSegmentSize = buffer.getLong();
            assert bridgeSpan >= 0 && nextSegmentSize >= 0 && buffer.position() == segmentEnd;
            if (nextSegmentSize == 0) {
                break;
            }
            segmentEnd += nextSegmentSize;
            currentAddressSwizzle += bridgeSpan * SqueakImageConstants.WORD_SIZE;
        }
        assert buffer.position() == headerSize + dataSize;
    }

    private SqueakImageChunk readObject(final MappedByteBuffer buffer) {
        int pos = buffer.position() - headerSize;
        assert pos % SqueakImageConstants.WORD_SIZE == 0 : "every object must be 64-bit aligned: " + pos % SqueakImageConstants.WORD_SIZE;
        long headerWord = buffer.getLong();
        int numSlots = SqueakImageConstants.ObjectHeader.getNumSlots(headerWord);
        if (numSlots == SqueakImageConstants.OVERFLOW_SLOTS) {
            numSlots = (int) (headerWord & ~SqueakImageConstants.SLOTS_MASK);
            assert numSlots >= SqueakImageConstants.OVERFLOW_SLOTS;
            pos = buffer.position() - headerSize;
            headerWord = buffer.getLong();
            assert SqueakImageConstants.ObjectHeader.getNumSlots(headerWord) == SqueakImageConstants.OVERFLOW_SLOTS : "Objects with long header must have 255 in slot count";
        }
        final int size = numSlots;
        assert size >= 0 : "Negative object size";
        final int classIndex = SqueakImageConstants.ObjectHeader.getClassIndex(headerWord);
        final byte[] objectData;
        if (ignoreObjectData(headerWord, classIndex, size)) {
            /* Skip some hidden objects for performance reasons. */
            objectData = null;
            skip(buffer, size * SqueakImageConstants.WORD_SIZE);
        } else {
            final int format = SqueakImageConstants.ObjectHeader.getFormat(headerWord);
            objectData = nextObjectData(buffer, size, format);
        }
        final SqueakImageChunk chunk = new SqueakImageChunk(this, headerWord, pos, objectData);
        if (hiddenRootsChunk == null && isHiddenObject(classIndex)) {
            if (freePageList == null) {
                assert classIndex == SqueakImageConstants.WORD_SIZE_CLASS_INDEX_PUN && size == SqueakImageConstants.NUM_FREE_LISTS;
                freePageList = chunk; /* First hidden object. */
            } else {
                assert classIndex == SqueakImageConstants.ARRAY_CLASS_INDEX_PUN &&
                                size == SqueakImageConstants.CLASS_TABLE_ROOT_SLOTS + SqueakImageConstants.HIDDEN_ROOT_SLOTS : "hiddenRootsObj has unexpected size";
                hiddenRootsChunk = chunk; /* Second hidden object. */
            }
        }
        return chunk;
    }

    private static byte[] nextObjectData(final MappedByteBuffer buffer, final int size, final int format) {
        if (size == 0) {
            skip(buffer, SqueakImageConstants.WORD_SIZE); // skip trailing alignment word
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        final int paddedObjectSize = size * SqueakImageConstants.WORD_SIZE;
        final int padding = calculateObjectPadding(format);
        final int objectDataSize = paddedObjectSize - padding;
        final byte[] bytes = new byte[objectDataSize];
        buffer.get(bytes);
        skip(buffer, padding);
        return bytes;
    }

    private static boolean ignoreObjectData(final long headerWord, final int classIndex, final int size) {
        return isFreeObject(classIndex) || isObjectStack(classIndex, size) || isHiddenObject(classIndex) && SqueakImageConstants.ObjectHeader.isPinned(headerWord);
    }

    protected static boolean isHiddenObject(final int classIndex) {
        return classIndex <= SqueakImageConstants.LAST_CLASS_INDEX_PUN;
    }

    private static boolean isFreeObject(final int classIndex) {
        return classIndex == SqueakImageConstants.FREE_OBJECT_CLASS_INDEX_PUN;
    }

    private static boolean isObjectStack(final int classIndex, final int size) {
        return classIndex == SqueakImageConstants.WORD_SIZE_CLASS_INDEX_PUN && size == SqueakImageConstants.OBJ_STACK_PAGE_SLOTS;
    }

    private SqueakImageChunk specialObjectChunk(final SqueakImageChunk specialObjectsChunk, final int idx) {
        return chunkMap.get(specialObjectsChunk.getWord(idx));
    }

    private void setPrebuiltObject(final SqueakImageChunk specialObjectsChunk, final int idx, final Object object) {
        specialObjectChunk(specialObjectsChunk, idx).setObject(object);
    }

    private void initPrebuiltConstant() {
        final SqueakImageChunk specialChunk = chunkMap.get(specialObjectsPointer);
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
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_SMALL_INTEGER, image.smallIntegerClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_STRING, image.byteStringClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_ARRAY, image.arrayClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.SMALLTALK_DICTIONARY, image.smalltalk);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_FLOAT, image.floatClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_METHOD_CONTEXT, image.methodContextClass);
        if (!specialObjectChunk(specialChunk, SPECIAL_OBJECT.CLASS_WIDE_STRING).isNil()) {
            setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_WIDE_STRING, image.initializeWideStringClass());
        }
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_POINT, image.pointClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_LARGE_POSITIVE_INTEGER, image.largePositiveIntegerClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_MESSAGE, image.messageClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_COMPILED_METHOD, image.compiledMethodClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_SEMAPHORE, image.semaphoreClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_CHARACTER, image.characterClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.SELECTOR_DOES_NOT_UNDERSTAND, image.doesNotUnderstand);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.SELECTOR_CANNOT_RETURN, image.cannotReturn);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.SPECIAL_SELECTORS, image.specialSelectors);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.SELECTOR_MUST_BE_BOOLEAN, image.mustBeBooleanSelector);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_BYTE_ARRAY, image.byteArrayClass);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_PROCESS, image.processClass);
        if (!specialObjectChunk(specialChunk, SPECIAL_OBJECT.CLASS_DOUBLE_BYTE_ARRAY).isNil()) {
            setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_DOUBLE_BYTE_ARRAY, image.initializeDoubleByteArrayClass());
        }
        if (!specialObjectChunk(specialChunk, SPECIAL_OBJECT.CLASS_WORD_ARRAY).isNil()) {
            setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_WORD_ARRAY, image.initializeWordArrayClass());
        }
        if (!specialObjectChunk(specialChunk, SPECIAL_OBJECT.CLASS_DOUBLE_WORD_ARRAY).isNil()) {
            setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_DOUBLE_WORD_ARRAY, image.initializeDoubleWordArrayClass());
        }
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.SELECTOR_CANNOT_INTERPRET, image.cannotInterpretSelector);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_BLOCK_CLOSURE, image.blockClosureClass);
        if (!specialObjectChunk(specialChunk, SPECIAL_OBJECT.CLASS_FULL_BLOCK_CLOSURE).isNil()) {
            setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_FULL_BLOCK_CLOSURE, image.initializeFullBlockClosureClass());
        }
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_LARGE_NEGATIVE_INTEGER, image.largeNegativeIntegerClass);
        if (!specialObjectChunk(specialChunk, SPECIAL_OBJECT.CLASS_EXTERNAL_ADDRESS).isNil()) {
            setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_EXTERNAL_ADDRESS, image.initializeExternalAddressClass());
        }
        if (!specialObjectChunk(specialChunk, SPECIAL_OBJECT.CLASS_EXTERNAL_STRUCTURE).isNil()) {
            setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_EXTERNAL_STRUCTURE, image.initializeExternalStructureClass());
        }
        if (!specialObjectChunk(specialChunk, SPECIAL_OBJECT.CLASS_EXTERNAL_DATA).isNil()) {
            setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_EXTERNAL_DATA, image.initializeExternalDataClass());
        }
        if (!specialObjectChunk(specialChunk, SPECIAL_OBJECT.CLASS_EXTERNAL_FUNCTION).isNil()) {
            setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_EXTERNAL_FUNCTION, image.initializeExternalFunctionClass());
        }
        if (!specialObjectChunk(specialChunk, SPECIAL_OBJECT.CLASS_EXTERNAL_LIBRARY).isNil()) {
            setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_EXTERNAL_LIBRARY, image.initializeExternalLibraryClass());
        }
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.SELECTOR_ABOUT_TO_RETURN, image.aboutToReturnSelector);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.SELECTOR_RUN_WITHIN, image.runWithInSelector);
        setPrebuiltObject(specialChunk, SPECIAL_OBJECT.PRIM_ERR_TABLE_INDEX, image.primitiveErrorTable);
        if (!specialObjectChunk(specialChunk, SPECIAL_OBJECT.CLASS_ALIEN).isNil()) {
            setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_ALIEN, image.initializeAlienClass());
        }
        if (!specialObjectChunk(specialChunk, SPECIAL_OBJECT.CLASS_UNSAFE_ALIEN).isNil()) {
            setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_UNSAFE_ALIEN, image.initializeUnsafeAlienClass());
        }
    }

    private void initObjects() {
        initPrebuiltConstant();
        fillInClassObjects();
        fillInObjects();
        fillInContextObjects();
        fillInClassesFromCompactClassList();
    }

    /**
     * Fill in classes and ensure instances of Behavior and its subclasses use {@link ClassObject}.
     */
    private void fillInClassObjects() {
        /* Find all metaclasses and instantiate their singleton instances as class objects. */
        int highestKnownClassIndex = -1;
        for (int p = 0; p < SqueakImageConstants.CLASS_TABLE_ROOT_SLOTS; p++) {
            final SqueakImageChunk classTablePage = chunkMap.get(hiddenRootsChunk.getWord(p));
            if (classTablePage.isNil()) {
                break; /* End of classTable reached (pages are consecutive). */
            }
            for (int i = 0; i < SqueakImageConstants.CLASS_TABLE_PAGE_SIZE; i++) {
                final long potentialClassPtr = classTablePage.getWord(i);
                assert potentialClassPtr != 0;
                final SqueakImageChunk classChunk = chunkMap.get(potentialClassPtr);
                if (classChunk.getSqueakClass() == image.metaClass) {
                    /* Derive classIndex from current position in class table. */
                    highestKnownClassIndex = p << SqueakImageConstants.CLASS_TABLE_MAJOR_INDEX_SHIFT | i;
                    assert classChunk.getWordSize() == METACLASS.INST_SIZE;
                    final SqueakImageChunk classInstance = chunkMap.get(classChunk.getWord(METACLASS.THIS_CLASS));
                    final ClassObject metaClassObject = classChunk.asClassObject(image.metaClass);
                    metaClassObject.setInstancesAreClasses();
                    classInstance.asClassObject(metaClassObject);
                }
            }
        }
        assert highestKnownClassIndex > 0 : "Failed to find highestKnownClassIndex";
        image.classTableIndex = highestKnownClassIndex;

        /* Fill in metaClass. */
        final SqueakImageChunk specialObjectsChunk = chunkMap.get(specialObjectsPointer);
        final SqueakImageChunk sqArray = specialObjectsChunk.getClassChunk();
        final SqueakImageChunk sqArrayClass = sqArray.getClassChunk();
        final SqueakImageChunk sqMetaclass = sqArrayClass.getClassChunk();
        image.metaClass.fillin(sqMetaclass);

        /*
         * Walk over all classes again and ensure instances of all subclasses of ClassDescriptions
         * are {@link ClassObject}s.
         */
        final HashSet<ClassObject> inst = new HashSet<>();
        final ClassObject classDescriptionClass = image.metaClass.getSuperclassOrNull();
        classDescriptionClass.setInstancesAreClasses();
        inst.add(classDescriptionClass);

        for (int p = 0; p < SqueakImageConstants.CLASS_TABLE_ROOT_SLOTS; p++) {
            final SqueakImageChunk classTablePage = chunkMap.get(hiddenRootsChunk.getWord(p));
            if (classTablePage.isNil()) {
                break; /* End of classTable reached (pages are consecutive). */
            }
            for (int i = 0; i < SqueakImageConstants.CLASS_TABLE_PAGE_SIZE; i++) {
                final long potentialClassPtr = classTablePage.getWord(i);
                assert potentialClassPtr != 0;
                final SqueakImageChunk classChunk = chunkMap.get(potentialClassPtr);
                if (classChunk.getSqueakClass() == image.metaClass) {
                    assert classChunk.getWordSize() == METACLASS.INST_SIZE;
                    final SqueakImageChunk classInstance = chunkMap.get(classChunk.getWord(METACLASS.THIS_CLASS));
                    final ClassObject metaClassObject = classChunk.asClassObject(image.metaClass);
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

        /* Finally, ensure instances of Behavior are {@link ClassObject}s. */
        final ClassObject behaviorClass = classDescriptionClass.getSuperclassOrNull();
        behaviorClass.setInstancesAreClasses();
    }

    private void fillInObjects() {
        for (final SqueakImageChunk chunk : chunkMap.getChunks()) {
            if (chunk == null) {
                continue;
            }
            final Object chunkObject = chunk.asObject();
            if (chunkObject instanceof final AbstractSqueakObjectWithClassAndHash obj) {
                // FIXME:
                if (obj.needsSqueakClass()) {
                    obj.setSqueakClass(chunk.getSqueakClass());
                }
                if (obj.getSqueakHashInt() != chunk.getHash()) {
                    obj.setSqueakHash(chunk.getHash());
                }
                obj.fillin(chunk);
            }
        }
    }

    public void fillInContextObjects() {
        for (final SqueakImageChunk chunk : chunkMap.getChunks()) {
            if (chunk == null) {
                continue;
            }
            final Object chunkObject = chunk.asObject();
            if (chunkObject instanceof final ContextObject contextObject) {
                contextObject.fillInContext(chunk);
            }
        }
    }

    private void fillInClassesFromCompactClassList() {
        image.smallFloatClass = lookupClassInCompactClassList(SqueakImageConstants.SMALL_FLOAT_TAG);
    }

    private ClassObject lookupClassInCompactClassList(final int compactIndex) {
        final int majorIndex = SqueakImageConstants.majorClassIndexOf(compactIndex);
        final int minorIndex = SqueakImageConstants.minorClassIndexOf(compactIndex);
        final ArrayObject classTablePage = (ArrayObject) chunkMap.get(hiddenRootsChunk.getWord(majorIndex)).asObject();
        final Object result = ArrayObjectReadNode.executeUncached(classTablePage, minorIndex);
        return result instanceof final ClassObject c ? c : null;
    }

    /* Calculate odd bits (see Behavior>>instSpec). */
    public static int calculateObjectPadding(final int format) {
        if (16 <= format && format <= 31) {
            return format & 7; /* 8-bit indexable and compiled methods: three odd bits */
        } else if (format == 11) {
            return Integer.BYTES; /* 32-bit words with 1 word padding. */
        } else if (12 <= format && format <= 15) {
            // 16-bit words with 2, 4, or 6 bytes padding
            return (format & 3) * Short.BYTES; /* 16-bit indexable: two odd bits */
        } else if (10 <= format) {
            return format & 1; /* 1 word padding */
        } else {
            return 0;
        }
    }

    public static class AddressToChunkMap {
        private static final int INITIAL_CAPACITY = 1_000_000;
        private static final float THRESHOLD_PERCENTAGE = 0.75f;
        private static final float RESIZE_FACTOR = 1.5f;
        private static final int COLLISION_OFFSET = 31;

        private int capacity = INITIAL_CAPACITY;
        private int threshold = (int) (capacity * THRESHOLD_PERCENTAGE);
        private long[] addresses = new long[capacity];
        private SqueakImageChunk[] chunks = new SqueakImageChunk[capacity];
        private int size;

        public void put(final long address, final SqueakImageChunk chunk) {
            if (size > threshold) {
                resize();
            }
            int slot = (int) (address % capacity);
            while (true) {
                if (chunks[slot] == null) {
                    addresses[slot] = address;
                    chunks[slot] = chunk;
                    size++;
                    return;
                }
                slot = (slot + COLLISION_OFFSET) % capacity;
            }
        }

        public SqueakImageChunk get(final long address) {
            int slot = (int) (address % capacity);
            while (true) {
                if (addresses[slot] == address) {
                    return chunks[slot];
                }
                slot = (slot + COLLISION_OFFSET) % capacity;
            }
        }

        private SqueakImageChunk[] getChunks() {
            return chunks;
        }

        private void resize() {
            capacity = (int) (capacity * RESIZE_FACTOR);
            threshold = (int) (capacity * THRESHOLD_PERCENTAGE);
            LogUtils.READER.log(Level.FINE, "Resizing chunk map to {0}", capacity);
            final long[] oldAddresses = addresses;
            final SqueakImageChunk[] oldChunks = chunks;
            addresses = new long[capacity];
            chunks = new SqueakImageChunk[capacity];
            size = 0;
            for (int i = 0; i < oldChunks.length; i++) {
                final SqueakImageChunk chunk = oldChunks[i];
                if (chunk != null) {
                    put(oldAddresses[i], chunk);
                }
            }
        }
    }
}
