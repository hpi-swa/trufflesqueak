package de.hpi.swa.graal.squeak.image;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.FillInNode;
import de.hpi.swa.graal.squeak.util.BitSplitter;
import de.hpi.swa.graal.squeak.util.StopWatch;

@SuppressWarnings("unused")
public final class SqueakImageReader extends Node {
    @CompilationFinal(dimensions = 1) private static final int[] CHUNK_HEADER_BIT_PATTERN = new int[]{22, 2, 5, 3, 22, 2, 8};
    @CompilationFinal static final Object NIL_OBJECT_PLACEHOLDER = new Object();
    @CompilationFinal private static final int SPECIAL_SELECTORS_INDEX = 23;
    @CompilationFinal private static final int FREE_OBJECT_CLASS_INDEX_PUN = 0;
    @CompilationFinal private static final long SLOTS_MASK = 0xFF << 56;
    @CompilationFinal private static final long OVERFLOW_SLOTS = 255;
    @CompilationFinal private SqueakImageChunk hiddenRootsChunk;
    @CompilationFinal private static final int HIDDEN_ROOTS_CHUNK_INDEX = 4;
    @CompilationFinal private final BufferedInputStream stream;
    @CompilationFinal private final LinkedHashMap<Integer, SqueakImageChunk> chunktable = new LinkedHashMap<>();
    private int chunkCount = 0;
    private int headerSize;
    private int oldBaseAddress;
    private int specialObjectsPointer;
    private short maxExternalSemaphoreTableSize;
    private int firstSegmentSize;
    private int position = 0;
    private final PrintWriter output;
    @CompilationFinal private final SqueakImageContext image;

    @Child private FillInNode fillInNode;
    @Child private LoopNode repeatingNode;
    private int segmentEnd;
    private int currentAddressSwizzle;

    public SqueakImageReader(final InputStream inputStream, final SqueakImageContext image) {
        stream = new BufferedInputStream(inputStream);
        output = image.getOutput();
        this.image = image;
        fillInNode = FillInNode.create(image);
        repeatingNode = Truffle.getRuntime().createLoopNode(new ReadObjectNode(this));
    }

    public void executeRead(final VirtualFrame frame) throws SqueakException {
        print("Reading image...");
        final StopWatch imageWatch = StopWatch.start("readImage");
        print("Reading header...");
        final StopWatch headerWatch = StopWatch.start("readHeader");
        readHeader();
        headerWatch.stopAndPrint();
        print("Reading body...");
        final StopWatch bodyWatch = StopWatch.start("readBody");
        readBody(frame);
        bodyWatch.stopAndPrint();
        initObjects(frame);
        imageWatch.stopAndPrint();
        if (!image.display.isHeadless() && image.simulatePrimitiveArgs.isNil()) {
            throw new SqueakException("Unable to find BitBlt simulation in image, cannot run with display.");
        }
    }

    @TruffleBoundary
    public void print(final String str) {
        output.println(str);
    }

    @TruffleBoundary
    private void readBytes(final byte[] bytes, final int length) {
        try {
            stream.read(bytes, 0, length);
        } catch (IOException e) {
            throw new SqueakException("Unable to read next bytes");
        }
    }

    private short nextShort() {
        final byte[] bytes = new byte[2];
        readBytes(bytes, 2);
        this.position += 2;
        return (short) (
          (bytes[1] & 0xFF) << 8 |
          (bytes[0] & 0xFF)
        );
    }

    private int nextInt() {
        final byte[] bytes = new byte[4];
        readBytes(bytes, 4);
        this.position += 4;
        return  (bytes[3] & 0xFF) << 24 |
                (bytes[2] & 0xFF) << 16 |
                (bytes[1] & 0xFF) << 8 |
                (bytes[0] & 0xFF);
    }

    private long nextLong() {
        final byte[] bytes = new byte[8];
        readBytes(bytes, 8);
        this.position += 8;
        return  (long) (bytes[7] & 0xFF) << 56 |
                (long) (bytes[6] & 0xFF) << 48 |
                (long) (bytes[5] & 0xFF) << 40 |
                (long) (bytes[4] & 0xFF) << 32 |
                (long) (bytes[3] & 0xFF) << 24 |
                (long) (bytes[2] & 0xFF) << 16 |
                (long) (bytes[1] & 0xFF) << 8 |
                (long) (bytes[0] & 0xFF);
    }

    private int readVersion() {
        final int version = nextInt();
        assert version == 0x00001979;
        return version;
    }

    private void readBaseHeader() {
        headerSize = nextInt();
        nextInt(); // endOfMemory
        oldBaseAddress = nextInt();
        specialObjectsPointer = nextInt();
        nextInt(); // 1 word last used hash
        final int lastWindowSize = nextInt();
        image.display.resizeTo((lastWindowSize >> 16) & 0xffff, lastWindowSize & 0xffff);
        final int headerFlags = nextInt();
        image.flags.initialize(headerFlags);
        nextInt(); // extraVMMemory
    }

    private void readSpurHeader() {
        nextShort(); // numStackPages
        nextShort(); // cogCodeSize
        nextInt(); // edenBytes
        maxExternalSemaphoreTableSize = nextShort();
        nextShort(); // re-align
        firstSegmentSize = nextInt();
        nextInt(); // freeOldSpace
    }

    private void readHeader() {
        readVersion();
        readBaseHeader();
        readSpurHeader();
        skipToBody();
    }

    private void skipToBody() {
        final int skip = headerSize - this.position;
        try {
            this.position += this.stream.skip(skip);
        } catch (IOException e) {
            throw new SqueakException("Unable to skip next bytes");
        }
    }

    static class ReadObjectNode extends Node implements RepeatingNode {
        private final SqueakImageReader reader;

        ReadObjectNode(final SqueakImageReader reader) {
            this.reader = reader;
        }

        public boolean executeRepeating(final VirtualFrame frame) {
            if (reader.position < reader.segmentEnd - 16) {
                final SqueakImageChunk chunk;
                chunk = reader.readObject();
                if (chunk.classid == FREE_OBJECT_CLASS_INDEX_PUN) {
                    return true;
                } else {
                    reader.extracted(chunk);
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
            repeatingNode.executeLoop(frame);
            final long bridge = nextLong();
            int bridgeSpan = 0;
            if ((bridge & SLOTS_MASK) != 0) {
                bridgeSpan = (int) (bridge & ~SLOTS_MASK);
            }
            final int nextSegmentSize = (int) nextLong();
            assert bridgeSpan >= 0;
            assert nextSegmentSize >= 0;
            assert position == segmentEnd;
            if (nextSegmentSize == 0) {
                break;
            }
            segmentEnd += nextSegmentSize;
            currentAddressSwizzle += bridgeSpan * 4;
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
    private void extracted(final SqueakImageChunk chunk) {
        chunktable.put(chunk.pos + this.currentAddressSwizzle, chunk);
        if (chunkCount++ == HIDDEN_ROOTS_CHUNK_INDEX) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            hiddenRootsChunk = chunk;
        }
    }

    private SqueakImageChunk readObject() {
        int pos = position;
        assert pos % 8 == 0;
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
            final int overflowSize = splitHeader[6];
            assert overflowSize == OVERFLOW_SLOTS;
        }
        final int classid = splitHeader[0];
        final int format = splitHeader[2];
        final int hash = splitHeader[4];
        assert size >= 0;
        assert 0 <= format && format <= 31;
        final SqueakImageChunk chunk = new SqueakImageChunk(this, image, size, format, classid, hash, pos);
        for (int i = 0; i < wordsFor(size); i++) {
            if (!chunk.isFull()) {
                chunk.append(nextInt());
            } else {
                nextInt(); // don't add trailing alignment words
            }
        }
        if (format < 10 && classid != FREE_OBJECT_CLASS_INDEX_PUN) {
            for (int slot : chunk.data()) {
                assert slot % 16 != 0 || slot >= oldBaseAddress;
            }
        }
        return chunk;
    }

    private static int wordsFor(final int size) {
        // see Spur32BitMemoryManager>>smallObjectBytesForSlots:
        return size <= 1 ? 2 : size + (size & 1);
    }

    @TruffleBoundary
    private SqueakImageChunk specialObjectChunk(final int idx) {
        final SqueakImageChunk specialObjectsChunk = getChunk(specialObjectsPointer);
        return getChunk(specialObjectsChunk.data()[idx]);
    }

    private void setPrebuiltObject(final int idx, final Object object) {
        specialObjectChunk(idx).object = object;
    }

    @TruffleBoundary
    private void initPrebuiltConstant() {
        final SqueakImageChunk specialObjectsChunk = getChunk(specialObjectsPointer);
        specialObjectsChunk.object = image.specialObjectsArray;

        // first we find the Metaclass, we need it to correctly instantiate
        // those classes that do not have any instances. Metaclass always
        // has instances, and all instances of Metaclass have their singleton
        // Behavior instance, so these are all correctly initialized already
        final SqueakImageChunk sqArray = classChunkOf(specialObjectsChunk);
        final SqueakImageChunk sqArrayClass = classChunkOf(sqArray);
        final SqueakImageChunk sqMetaclass = classChunkOf(sqArrayClass);
        sqMetaclass.object = image.metaclass;

        // also cache nil, true, and false classes
        classChunkOf(specialObjectChunk(0)).object = image.nilClass;
        classChunkOf(specialObjectChunk(1)).object = image.falseClass;
        classChunkOf(specialObjectChunk(2)).object = image.trueClass;

        setPrebuiltObject(0, NIL_OBJECT_PLACEHOLDER);
        setPrebuiltObject(1, image.sqFalse);
        setPrebuiltObject(2, image.sqTrue);
        setPrebuiltObject(3, image.schedulerAssociation);
        setPrebuiltObject(5, image.smallIntegerClass);
        setPrebuiltObject(6, image.stringClass);
        setPrebuiltObject(7, image.arrayClass);
        setPrebuiltObject(8, image.smalltalk);
        setPrebuiltObject(9, image.floatClass);
        setPrebuiltObject(10, image.methodContextClass);
        setPrebuiltObject(13, image.largePositiveIntegerClass);
        setPrebuiltObject(16, image.compiledMethodClass);
        setPrebuiltObject(19, image.characterClass);
        setPrebuiltObject(20, image.doesNotUnderstand);
        setPrebuiltObject(25, image.mustBeBoolean);
        setPrebuiltObject(36, image.blockClosureClass);
        setPrebuiltObject(42, image.largeNegativeIntegerClass);
        setPrebuiltObject(SPECIAL_SELECTORS_INDEX, image.specialSelectors);
    }

    @TruffleBoundary
    private void initPrebuiltSelectors() {
        final SqueakImageChunk specialObjectsChunk = getChunk(specialObjectsPointer);
        final SqueakImageChunk specialSelectorChunk = getChunk(specialObjectsChunk.data()[SPECIAL_SELECTORS_INDEX]);

        final NativeObject[] specialSelectors = image.specialSelectorsArray;
        for (int i = 0; i < specialSelectors.length; i++) {
            getChunk(specialSelectorChunk.data()[i * 2]).object = specialSelectors[i];
        }
    }

    private void initObjects(final VirtualFrame frame) {
        initPrebuiltConstant();
        initPrebuiltSelectors();
        // connect all instances to their classes
        print("Connecting classes...");
        final StopWatch setClassesWatch = StopWatch.start("setClasses");
        for (SqueakImageChunk chunk : chunktable.values()) {
            chunk.setSqClass(classChunkOf(chunk).asClassObject());
        }
        setClassesWatch.stopAndPrint();
        final StopWatch instantiateWatch = StopWatch.start("instClasses");
        instantiateClasses();
        instantiateWatch.stopAndPrint();
        final StopWatch fillInWatch = StopWatch.start("fillInObjects");
        fillInObjects(frame);
        fillInWatch.stopAndPrint();
    }

    @TruffleBoundary
    private void instantiateClasses() {
        // find all metaclasses and instantiate their singleton instances as class objects
        print("Instantiating classes...");
        for (int classtablePtr : hiddenRootsChunk.data()) {
            if (getChunk(classtablePtr) != null) {
                for (int potentialClassPtr : getChunk(classtablePtr).data()) {
                    final SqueakImageChunk metaClass = getChunk(potentialClassPtr);
                    if (metaClass != null && metaClass.getSqClass() == image.metaclass) {
                        final int[] data = metaClass.data();
                        final SqueakImageChunk classInstance = getChunk(data[data.length - 1]);
                        assert data.length == 6;
                        metaClass.asClassObject();
                        classInstance.asClassObject();
                    }
                }
            }
        }
    }

    private void fillInObjects(final VirtualFrame frame) {
        print("Filling in objects...");
        for (SqueakImageChunk chunk : chunktable.values()) {
            final Object chunkObject = chunk.asObject();
            fillInNode.execute(frame, chunkObject, chunk);
        }

        if (image.asSymbol.isNil()) {
            throw new SqueakException("Unable to find asSymbol selector");
        }
    }

    @TruffleBoundary
    private SqueakImageChunk classChunkOf(final SqueakImageChunk chunk) {
        final int majorIdx = majorClassIndexOf(chunk.classid);
        final int minorIdx = minorClassIndexOf(chunk.classid);
        final SqueakImageChunk classTablePage = getChunk(hiddenRootsChunk.data()[majorIdx]);
        return getChunk(classTablePage.data()[minorIdx]);
    }

    private static int majorClassIndexOf(final int classid) {
        return classid >> 10;
    }

    private static int minorClassIndexOf(final int classid) {
        return classid & ((1 << 10) - 1);
    }

    @TruffleBoundary
    public SqueakImageChunk getChunk(final int ptr) {
        return chunktable.get(ptr);
    }
}
