/*
 * Copyright (c) 2017-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.image;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

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
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT_TAG;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.VarHandleUtils;

public final class SqueakImageReader {
    private static final byte[] EMPTY_BYTES = new byte[0];

    protected SqueakImageChunk hiddenRootsChunk;

    private final HashMap<Long, SqueakImageChunk> chunktable = new HashMap<>(750000);
    protected final SqueakImageContext image;
    private final byte[] byteArrayBuffer = new byte[Long.BYTES];

    private long oldBaseAddress;
    private long specialObjectsPointer;
    private long firstSegmentSize;
    private int position;
    private long currentAddressSwizzle;
    private long dataSize;

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

    private Object run() {
        SqueakImageContext.initializeBeforeLoadingImage();
        final long start = MiscUtils.currentTimeMillis();
        final TruffleFile truffleFile = image.env.getPublicTruffleFile(image.getImagePath());
        if (!truffleFile.isRegularFile()) {
            throw SqueakException.create(MiscUtils.format("Image at '%s' does not exist.", image.getImagePath()));
        }
        try (BufferedInputStream inputStream = new BufferedInputStream(truffleFile.newInputStream())) {
            readHeader(inputStream);
            readBody(inputStream);
        } catch (final IOException e) {
            throw SqueakException.create("Failed to read Smalltalk image:", e.getMessage());
        }
        initObjects();
        image.printToStdOut("Image loaded in", MiscUtils.currentTimeMillis() - start + "ms.");
        image.setHiddenRoots((ArrayObject) hiddenRootsChunk.asObject());
        return image.getSqueakImage();
    }

    private static void readBytes(final BufferedInputStream stream, final byte[] bytes, final int length) throws IOException {
        final int readBytes = stream.read(bytes, 0, length);
        assert readBytes == length : "Failed to read bytes";
    }

    private long nextWord(final BufferedInputStream stream) throws IOException {
        return nextLong(stream);
    }

    private short nextShort(final BufferedInputStream stream) throws IOException {
        readBytes(stream, byteArrayBuffer, Short.BYTES);
        position += Short.BYTES;
        return VarHandleUtils.getShort(byteArrayBuffer, 0);
    }

    private int nextInt(final BufferedInputStream stream) throws IOException {
        readBytes(stream, byteArrayBuffer, Integer.BYTES);
        position += Integer.BYTES;
        return VarHandleUtils.getInt(byteArrayBuffer, 0);
    }

    private long nextLong(final BufferedInputStream stream) throws IOException {
        readBytes(stream, byteArrayBuffer, Long.BYTES);
        position += Long.BYTES;
        return VarHandleUtils.getLong(byteArrayBuffer, 0);
    }

    private byte[] nextObjectData(final BufferedInputStream stream, final int size, final int format) throws IOException {
        if (size == 0) {
            skipBytes(stream, SqueakImageConstants.WORD_SIZE); // skip trailing alignment word
            return EMPTY_BYTES;
        }
        final int paddedObjectSize = size * SqueakImageConstants.WORD_SIZE;
        final int padding = calculateObjectPadding(format);
        final int objectDataSize = paddedObjectSize - padding;
        final byte[] bytes = new byte[objectDataSize];
        readBytes(stream, bytes, objectDataSize);
        final long skipped = stream.skip(padding);
        assert skipped == padding : "Failed to skip padding bytes";
        position += paddedObjectSize;
        return bytes;
    }

    private void skipBytes(final BufferedInputStream stream, final int count) throws IOException {
        long pending = count;
        while (pending > 0) {
            final long skipped = stream.skip(pending);
            assert skipped > 0 : "Nothing skipped, reached EOF?";
            pending -= skipped;
        }
        position += count;
    }

    private void readHeader(final BufferedInputStream stream) throws IOException {
        image.imageFormat = nextInt(stream);
        if (!ArrayUtils.contains(SqueakImageConstants.SUPPORTED_IMAGE_FORMATS, image.imageFormat)) {
            throw SqueakException.create(MiscUtils.format("Image format %s not supported. Please supply a compatible 64bit Spur image (%s).", image.imageFormat,
                            Arrays.toString(SqueakImageConstants.SUPPORTED_IMAGE_FORMATS)));
        }

        // Base header start
        final int headerSize = nextInt(stream);
        dataSize = nextWord(stream);
        oldBaseAddress = nextWord(stream);
        specialObjectsPointer = nextWord(stream);
        nextWord(stream); // 1 word last used hash
        final long snapshotScreenSize = nextWord(stream);
        final long headerFlags = nextWord(stream);
        nextInt(stream); // extraVMMemory

        // Spur header start
        nextShort(stream); // numStackPages
        nextShort(stream); // cogCodeSize
        assert position == 64 : "Wrong position";
        nextInt(stream); // edenBytes
        final short maxExternalSemaphoreTableSize = nextShort(stream);
        nextShort(stream); // unused, realign to word boundary
        assert position == 72 : "Wrong position";
        firstSegmentSize = nextWord(stream);
        nextWord(stream); // freeOldSpace

        image.flags.initialize(oldBaseAddress, headerFlags, snapshotScreenSize, maxExternalSemaphoreTableSize);

        skipBytes(stream, headerSize - position); // skip to body
    }

    private void readBody(final BufferedInputStream stream) throws IOException {
        position = 0;
        long segmentEnd = firstSegmentSize;
        currentAddressSwizzle = oldBaseAddress;
        while (position < segmentEnd) {
            while (position < segmentEnd - SqueakImageConstants.IMAGE_BRIDGE_SIZE) {
                final SqueakImageChunk chunk = readObject(stream);
                putChunk(chunk);
            }
            assert hiddenRootsChunk != null : "hiddenRootsChunk must be known from now on.";
            final long bridge = nextLong(stream);
            long bridgeSpan = 0;
            if ((bridge & SqueakImageConstants.SLOTS_MASK) != 0) {
                bridgeSpan = bridge & ~SqueakImageConstants.SLOTS_MASK;
            }
            final long nextSegmentSize = nextLong(stream);
            assert bridgeSpan >= 0 && nextSegmentSize >= 0 && position == segmentEnd;
            if (nextSegmentSize == 0) {
                break;
            }
            segmentEnd += nextSegmentSize;
            currentAddressSwizzle += bridgeSpan * SqueakImageConstants.WORD_SIZE;
        }
        assert dataSize == position;
    }

    private void putChunk(final SqueakImageChunk chunk) {
        chunktable.put(chunk.getPosition() + currentAddressSwizzle, chunk);
    }

    private SqueakImageChunk readObject(final BufferedInputStream stream) throws IOException {
        int pos = position;
        assert pos % SqueakImageConstants.WORD_SIZE == 0 : "every object must be 64-bit aligned: " + pos % SqueakImageConstants.WORD_SIZE;
        long headerWord = nextLong(stream);
        int numSlots = SqueakImageConstants.ObjectHeader.getNumSlots(headerWord);
        if (numSlots == SqueakImageConstants.OVERFLOW_SLOTS) {
            numSlots = (int) (headerWord & ~SqueakImageConstants.SLOTS_MASK);
            assert numSlots >= SqueakImageConstants.OVERFLOW_SLOTS;
            pos = position;
            headerWord = nextLong(stream);
            assert SqueakImageConstants.ObjectHeader.getNumSlots(headerWord) == SqueakImageConstants.OVERFLOW_SLOTS : "Objects with long header must have 255 in slot count";
        }
        final int size = numSlots;
        assert size >= 0 : "Negative object size";
        final int classIndex = SqueakImageConstants.ObjectHeader.getClassIndex(headerWord);
        final byte[] objectData;
        if (ignoreObjectData(headerWord, classIndex, size)) {
            /* Skip some hidden objects for performance reasons. */
            objectData = null;
            skipBytes(stream, size * SqueakImageConstants.WORD_SIZE);
        } else {
            final int format = SqueakImageConstants.ObjectHeader.getFormat(headerWord);
            objectData = nextObjectData(stream, size, format);
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

    protected static boolean ignoreObjectData(final long headerWord, final int classIndex, final int size) {
        return isFreeObject(classIndex) || isObjectStack(classIndex, size) || isHiddenObject(classIndex) && SqueakImageConstants.ObjectHeader.isPinned(headerWord);
    }

    protected static boolean isHiddenObject(final int classIndex) {
        return classIndex <= SqueakImageConstants.LAST_CLASS_INDEX_PUN;
    }

    protected static boolean isFreeObject(final int classIndex) {
        return classIndex == SqueakImageConstants.FREE_OBJECT_CLASS_INDEX_PUN;
    }

    protected static boolean isObjectStack(final int classIndex, final int size) {
        return classIndex == SqueakImageConstants.WORD_SIZE_CLASS_INDEX_PUN && size == SqueakImageConstants.OBJ_STACK_PAGE_SLOTS;
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
        if (!specialObjectChunk(specialChunk, SPECIAL_OBJECT.CLASS_FULL_BLOCK_CLOSURE).isNil()) {
            image.fullBlockClosureClass = new ClassObject(image);
            setPrebuiltObject(specialChunk, SPECIAL_OBJECT.CLASS_FULL_BLOCK_CLOSURE, image.fullBlockClosureClass);
        }
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
        fillInClassesFromCompactClassList();
    }

    /**
     * Fill in classes and ensure instances of Behavior and its subclasses use {@link ClassObject}.
     */
    private void fillInClassObjects() {
        /** Find all metaclasses and instantiate their singleton instances as class objects. */
        int highestKnownClassIndex = -1;
        for (int p = 0; p < SqueakImageConstants.CLASS_TABLE_ROOT_SLOTS; p++) {
            final SqueakImageChunk classTablePage = getChunk(hiddenRootsChunk.getWord(p));
            if (classTablePage.isNil()) {
                break; /* End of classTable reached (pages are consecutive). */
            }
            for (int i = 0; i < SqueakImageConstants.CLASS_TABLE_PAGE_SIZE; i++) {
                final long potentialClassPtr = classTablePage.getWord(i);
                assert potentialClassPtr != 0;
                final SqueakImageChunk classChunk = getChunk(potentialClassPtr);
                if (classChunk.getSqueakClass() == image.metaClass) {
                    /* Derive classIndex from current position in class table. */
                    highestKnownClassIndex = p << SqueakImageConstants.CLASS_TABLE_MAJOR_INDEX_SHIFT | i;
                    assert classChunk.getWordSize() == METACLASS.INST_SIZE;
                    final SqueakImageChunk classInstance = getChunk(classChunk.getWord(METACLASS.THIS_CLASS));
                    final ClassObject metaClassObject = classChunk.asClassObject(image.metaClass);
                    metaClassObject.setInstancesAreClasses();
                    classInstance.asClassObject(metaClassObject);
                }
            }
        }
        assert highestKnownClassIndex > 0 : "Failed to find highestKnownClassIndex";
        image.classTableIndex = highestKnownClassIndex;

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

        for (int p = 0; p < SqueakImageConstants.CLASS_TABLE_ROOT_SLOTS; p++) {
            final SqueakImageChunk classTablePage = getChunk(hiddenRootsChunk.getWord(p));
            if (classTablePage.isNil()) {
                break; /* End of classTable reached (pages are consecutive). */
            }
            for (int i = 0; i < SqueakImageConstants.CLASS_TABLE_PAGE_SIZE; i++) {
                final long potentialClassPtr = classTablePage.getWord(i);
                assert potentialClassPtr != 0;
                final SqueakImageChunk classChunk = getChunk(potentialClassPtr);
                if (classChunk.getSqueakClass() == image.metaClass) {
                    assert classChunk.getWordSize() == METACLASS.INST_SIZE;
                    final SqueakImageChunk classInstance = getChunk(classChunk.getWord(METACLASS.THIS_CLASS));
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

        /** Finally, ensure instances of Behavior are {@link ClassObject}s. */
        final ClassObject behaviorClass = classDescriptionClass.getSuperclassOrNull();
        behaviorClass.setInstancesAreClasses();
    }

    private void fillInObjects() {
        for (final SqueakImageChunk chunk : chunktable.values()) {
            final Object chunkObject = chunk.asObject();
            if (chunkObject instanceof final AbstractSqueakObjectWithClassAndHash obj) {
                // FIXME:
                if (obj.needsSqueakClass()) {
                    obj.setSqueakClass(chunk.getSqueakClass());
                }
                if (obj.getSqueakHash() != chunk.getHash()) {
                    obj.setSqueakHash(chunk.getHash());
                }
                obj.fillin(chunk);
            }
        }
    }

    private void fillInContextObjects() {
        for (final SqueakImageChunk chunk : chunktable.values()) {
            final Object chunkObject = chunk.asObject();
            if (chunkObject instanceof final ContextObject contextObject) {
                assert !contextObject.hasTruffleFrame();
                contextObject.fillinContext(chunk);
            }
        }
    }

    private void fillInClassesFromCompactClassList() {
        image.smallFloatClass = lookupClassInCompactClassList(SPECIAL_OBJECT_TAG.SMALL_FLOAT);
        if (image.fullBlockClosureClass == null) {
            image.fullBlockClosureClass = lookupClassInCompactClassList(SqueakImageConstants.CLASS_FULL_BLOCK_CLOSURE_COMPACT_INDEX);
        }
    }

    private ClassObject lookupClassInCompactClassList(final int compactIndex) {
        final int majorIndex = SqueakImageConstants.majorClassIndexOf(compactIndex);
        final int minorIndex = SqueakImageConstants.minorClassIndexOf(compactIndex);
        final ArrayObject classTablePage = (ArrayObject) getChunk(hiddenRootsChunk.getWord(majorIndex)).asObject();
        final Object result = ArrayObjectReadNode.executeUncached(classTablePage, minorIndex);
        return result instanceof final ClassObject c ? c : null;
    }

    protected SqueakImageChunk getChunk(final long ptr) {
        return chunktable.get(ptr);
    }

    /* Calculate odd bits (see Behavior>>instSpec). */
    public static int calculateObjectPadding(final int format) {
        if (16 <= format && format <= 31) {
            return format & 7; /* 8-bit indexable and compiled methods: three odd bits */
        } else if (format == 11) {
            return 4; /* 32-bit words with 1 word padding. */
        } else if (12 <= format && format <= 15) {
            // 16-bit words with 2, 4, or 6 bytes padding
            return format & 3; /* 16-bit indexable: two odd bits */
        } else if (10 <= format) {
            return format & 1; /* 1 word padding */
        } else {
            return 0;
        }
    }
}
