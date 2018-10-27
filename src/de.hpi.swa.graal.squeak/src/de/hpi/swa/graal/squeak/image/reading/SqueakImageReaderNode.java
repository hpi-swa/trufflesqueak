package de.hpi.swa.graal.squeak.image.reading;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.HashMap;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.util.BitSplitter;

public final class SqueakImageReaderNode extends RootNode {
    @CompilationFinal(dimensions = 1) private static final int[] CHUNK_HEADER_BIT_PATTERN = new int[]{22, 2, 5, 3, 22, 2, 8};
    public static final Object NIL_OBJECT_PLACEHOLDER = new Object();
    private static final int IMAGE_32BIT_VERSION = 6521;
    private static final int IMAGE_64BIT_VERSION = 68021;
    private static final int FREE_OBJECT_CLASS_INDEX_PUN = 0;
    private static final long SLOTS_MASK = 0xFF << 56;
    private static final long OVERFLOW_SLOTS = 255;
    private static final int HIDDEN_ROOTS_CHUNK_INDEX = 4;

    @CompilationFinal protected boolean is64bit = false;
    @CompilationFinal protected int wordSize = 4;
    @CompilationFinal protected SqueakImageChunk hiddenRootsChunk;

    private final BufferedInputStream stream;
    private final HashMap<Long, SqueakImageChunk> chunktable = new HashMap<>(750000);
    private final SqueakImageContext image;

    private int chunkCount = 0;
    private long headerSize;
    private long oldBaseAddress;
    private long specialObjectsPointer;
    @SuppressWarnings("unused") private short maxExternalSemaphoreTableSize; // TODO: use value
    private long firstSegmentSize;
    private int position = 0;
    private long segmentEnd;
    private long currentAddressSwizzle;

    @Child private LoopNode readObjectLoopNode;
    @Child private FillInClassAndHashNode fillInClassNode = FillInClassAndHashNode.create();
    @Child private FillInNode fillInNode;

    public SqueakImageReaderNode(final SqueakImageContext image) {
        super(image.getLanguage());
        final TruffleFile truffleFile = image.env.getTruffleFile(image.getImagePath());
        BufferedInputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(truffleFile.newInputStream());
        } catch (IOException e) {
            if (!image.isTesting()) {
                throw new SqueakException(e);
            }
        }
        stream = inputStream;
        this.image = image;
        readObjectLoopNode = Truffle.getRuntime().createLoopNode(new ReadObjectLoopNode(this));
        fillInNode = FillInNode.create(image);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        if (stream == null && image.isTesting()) {
            return null;
        }
        final long start = currentTimeMillis();
        readHeader();
        readBody(frame);
        initObjects();
        validateStateOrFail();
        clearChunktable();
        image.printToStdOut("Image loaded in", (currentTimeMillis() - start) + "ms.");
        return null;
    }

    @TruffleBoundary
    private static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @TruffleBoundary
    private void clearChunktable() {
        chunktable.clear();
    }

    private void validateStateOrFail() {
        if (image.hasDisplay() && image.getSimulatePrimitiveArgsSelector() == null) {
            throw new SqueakException("Unable to find BitBlt simulation in image, cannot run with display.");
        }
        if (image.isTesting() && image.getAsSymbolSelector() == null) {
            throw new SqueakException("Unable to find asSymbol selector");
        }
    }

    @TruffleBoundary
    private void readBytes(final byte[] bytes, final int length) {
        try {
            stream.read(bytes, 0, length);
        } catch (IOException e) {
            throw new SqueakException("Unable to read next bytes");
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
        this.position += count;
        return bytes;
    }

    private short nextShort() {
        final byte[] bytes = new byte[2];
        readBytes(bytes, 2);
        this.position += 2;
        return (short) ((bytes[1] & 0xFF) << 8 |
                        (bytes[0] & 0xFF));
    }

    private int nextInt() {
        final byte[] bytes = new byte[4];
        readBytes(bytes, 4);
        this.position += 4;
        return (bytes[3] & 0xFF) << 24 |
                        (bytes[2] & 0xFF) << 16 |
                        (bytes[1] & 0xFF) << 8 |
                        (bytes[0] & 0xFF);
    }

    private long nextLong() {
        final byte[] bytes = new byte[8];
        readBytes(bytes, 8);
        this.position += 8;
        return (long) (bytes[7] & 0xFF) << 56 |
                        (long) (bytes[6] & 0xFF) << 48 |
                        (long) (bytes[5] & 0xFF) << 40 |
                        (long) (bytes[4] & 0xFF) << 32 |
                        (bytes[3] & 0xFF) << 24 |
                        (bytes[2] & 0xFF) << 16 |
                        (bytes[1] & 0xFF) << 8 |
                        (bytes[0] & 0xFF);
    }

    @TruffleBoundary
    private void skipBytes(final long count) {
        try {
            this.position += this.stream.skip(count);
        } catch (IOException e) {
            throw new SqueakException("Unable to skip next bytes");
        }
    }

    private void readVersion() {
        final long version = nextWord();
        CompilerDirectives.transferToInterpreterAndInvalidate();
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
        final int lastWindowSizeWord = (int) nextWord();
        final int headerFlags = (int) nextWord();
        image.flags.initialize(headerFlags, lastWindowSizeWord, is64bit);
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
        skipToBody();
    }

    private void skipToBody() {
        skipBytes(headerSize - position);
    }

    private static final class ReadObjectLoopNode extends Node implements RepeatingNode {
        private final SqueakImageReaderNode reader;

        private ReadObjectLoopNode(final SqueakImageReaderNode reader) {
            this.reader = reader;
        }

        public boolean executeRepeating(final VirtualFrame frame) {
            if (reader.position < reader.segmentEnd - 16) {
                final SqueakImageChunk chunk = reader.readObject();
                if (chunk.classid == FREE_OBJECT_CLASS_INDEX_PUN) {
                    return true;
                } else {
                    reader.putChunk(chunk);
                    return true;
                }
            }
            return false;
        }
    }

    private void readBody(final VirtualFrame frame) {
        position = 0;
        segmentEnd = firstSegmentSize;
        currentAddressSwizzle = oldBaseAddress;
        while (this.position < segmentEnd) {
            readObjectLoopNode.executeLoop(frame);
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
            this.stream.close();
        } catch (IOException e) {
            throw new SqueakException("Unable to close stream");
        }
    }

    @TruffleBoundary
    private void putChunk(final SqueakImageChunk chunk) {
        chunktable.put(chunk.pos + this.currentAddressSwizzle, chunk);
        if (chunkCount++ == HIDDEN_ROOTS_CHUNK_INDEX) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            hiddenRootsChunk = chunk;
        }
    }

    private SqueakImageChunk readObject() {
        int pos = position;
        assert pos % 8 == 0 : "every object must be 64-bit aligned: " + pos % 8;
        long headerWord = nextLong();
        // 22 2 5 3 22 2 8
        // classid _ format _ hash _ size
        int[] splitHeader = BitSplitter.splitter(headerWord, CHUNK_HEADER_BIT_PATTERN);
        int size = splitHeader[6];
        if (size == OVERFLOW_SLOTS) {
            size = (int) (headerWord & ~SLOTS_MASK);
            pos = position;
            headerWord = nextLong();
            splitHeader = BitSplitter.splitter(headerWord, CHUNK_HEADER_BIT_PATTERN);
            assert splitHeader[6] == OVERFLOW_SLOTS : "Objects with long header must have 255 in slot count";
        }
        final int classid = splitHeader[0];
        final int format = splitHeader[2];
        final int hash = splitHeader[4];
        assert size >= 0 : "Negative object size";
        assert 0 <= format && format <= 31 : "Unexpected format";
        final SqueakImageChunk chunk = new SqueakImageChunk(this, image, nextBytes(size * wordSize), format, classid, hash, pos);
        final int wordsFor = wordsFor(size);
        if (wordsFor > size * wordSize) {
            skipBytes(wordsFor - (size * wordSize)); // skip trailing alignment words
        }
        assert format != 0 || classid == 0 || size == 0 : "Empty objects must not have slots";
        assert checkAddressIntegrity(classid, format, chunk);
        return chunk;
    }

    private boolean checkAddressIntegrity(final int classid, final int format, final SqueakImageChunk chunk) {
        if (is64bit) {
            return true; // FIXME: temporarily disabled for 64bit.
        }
        boolean result = true;
        if (format < 10 && classid != FREE_OBJECT_CLASS_INDEX_PUN) {
            for (long slot : chunk.getWords()) {
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
        sqMetaclass.object = image.metaclass;

        // also cache nil, true, and false classes
        specialObjectChunk(SPECIAL_OBJECT_INDEX.NilObject).getClassChunk().object = image.nilClass;
        image.nil.setSqueakClass(image.nilClass);
        specialObjectChunk(SPECIAL_OBJECT_INDEX.FalseObject).getClassChunk().object = image.falseClass;
        specialObjectChunk(SPECIAL_OBJECT_INDEX.TrueObject).getClassChunk().object = image.trueClass;

        setPrebuiltObject(SPECIAL_OBJECT_INDEX.NilObject, NIL_OBJECT_PLACEHOLDER);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.FalseObject, image.sqFalse);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.TrueObject, image.sqTrue);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.SchedulerAssociation, image.schedulerAssociation);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.ClassBitmap, image.bitmapClass);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.ClassSmallInteger, image.smallIntegerClass);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.ClassString, image.stringClass);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.ClassArray, image.arrayClass);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.SmalltalkDictionary, image.smalltalk);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.ClassFloat, image.floatClass);
        if (specialObjectChunk(SPECIAL_OBJECT_INDEX.ClassTruffleObject).object != NIL_OBJECT_PLACEHOLDER) {
            setPrebuiltObject(SPECIAL_OBJECT_INDEX.ClassTruffleObject, image.initializeTruffleObject());
        }
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.ClassMethodContext, image.methodContextClass);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.ClassPoint, image.pointClass);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.ClassLargePositiveInteger, image.largePositiveIntegerClass);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.ClassMessage, image.messageClass);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.ClassCompiledMethod, image.compiledMethodClass);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.ClassSemaphore, image.semaphoreClass);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.ClassCharacter, image.characterClass);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.SelectorDoesNotUnderstand, image.doesNotUnderstand);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.SelectorMustBeBoolean, image.mustBeBooleanSelector);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.ClassByteArray, image.byteArrayClass);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.ClassProcess, image.processClass);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.ClassBlockClosure, image.blockClosureClass);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.ExternalObjectsArray, image.externalObjectsArray);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.ClassLargeNegativeInteger, image.largeNegativeIntegerClass);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.SelectorAboutToReturn, image.aboutToReturnSelector);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.SelectorRunWithIn, image.runWithInSelector);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.PrimErrTableIndex, image.primitiveErrorTable);
        setPrebuiltObject(SPECIAL_OBJECT_INDEX.SpecialSelectors, image.specialSelectors);
    }

    @ExplodeLoop
    private void initPrebuiltSelectors() {
        final SqueakImageChunk specialObjectsChunk = getChunk(specialObjectsPointer);
        final SqueakImageChunk specialSelectorChunk = getChunk(specialObjectsChunk.getWords()[SPECIAL_OBJECT_INDEX.SpecialSelectors]);

        final NativeObject[] specialSelectors = image.specialSelectorsArray;
        for (int i = 0; i < specialSelectors.length; i++) {
            getChunk(specialSelectorChunk.getWords()[i * 2]).object = specialSelectors[i];
        }
    }

    private void initObjects() {
        initPrebuiltConstant();
        initPrebuiltSelectors();
        // connect all instances to their classes
        image.printToStdOut("Instantiating classes...");
        instantiateClasses();
        image.printToStdOut("Filling in objects...");
        /*
         * TODO: use LoopNode for filling in objects. The following is another candidate for an
         * OSR-able loop. The first attempt resulted in a memory leak though.
         */
        for (final SqueakImageChunk chunk : chunktable.values()) {
            final Object chunkObject = chunk.asObject();
            fillInClassNode.execute(chunkObject, chunk);
            fillInNode.execute(chunkObject, chunk);
        }
    }

    private void instantiateClasses() {
        // find all metaclasses and instantiate their singleton instances as class objects
        for (long classtablePtr : hiddenRootsChunk.getWords()) {
            if (getChunk(classtablePtr) != null) {
                for (long potentialClassPtr : getChunk(classtablePtr).getWords()) {
                    if (potentialClassPtr == 0) {
                        continue;
                    }
                    final SqueakImageChunk metaClass = getChunk(potentialClassPtr);
                    if (metaClass != null && metaClass.getSqClass() == image.metaclass) {
                        final long[] data = metaClass.getWords();
                        final SqueakImageChunk classInstance = getChunk(data[data.length - 1]);
                        assert data.length == 6;
                        metaClass.asClassObject();
                        classInstance.asClassObject();
                    }
                }
            }
        }
    }

    @TruffleBoundary
    public SqueakImageChunk getChunk(final long ptr) {
        return chunktable.get(ptr);
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}
