package de.hpi.swa.trufflesqueak.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Vector;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.SqueakObject;

@SuppressWarnings("unused")
public class ImageReader {
    public static final Object NIL_OBJECT_PLACEHOLDER = new Object();
    private static final int SPECIAL_SELECTORS_INDEX = 23;
    private static final int FREE_OBJECT_CLASS_INDEX_PUN = 0;
    private static final long SLOTS_MASK = 0xFF << 56;
    private static final long OVERFLOW_SLOTS = 255;
    private static final int HIDDEN_ROOTS_CHUNK = 4; // nil, false, true, freeList, hiddenRoots
    final BufferedInputStream stream;
    final ByteBuffer shortBuf = ByteBuffer.allocate(2);
    final ByteBuffer intBuf = ByteBuffer.allocate(4);
    final ByteBuffer longBuf = ByteBuffer.allocate(8);
    private int headerSize;
    private int endOfMemory;
    private int oldBaseAddress;
    private int specialObjectsPointer;
    private int lastHash;
    private int lastWindowSize;
    private int wasFullscreen;
    private int extraVMMemory;
    private short numStackPages;
    private short cogCodeSize;
    private int edenBytes;
    private short maxExternalSemaphoreTableSize;
    private int firstSegmentSize;
    private int freeOldSpace;
    private int position;
    private Vector<Chunk> chunklist;
    HashMap<Integer, Chunk> chunktable;
    private PrintWriter output;

    public ImageReader(FileInputStream inputStream, PrintWriter printWriter) throws FileNotFoundException {
        shortBuf.order(ByteOrder.nativeOrder());
        intBuf.order(ByteOrder.nativeOrder());
        longBuf.order(ByteOrder.nativeOrder());
        this.output = printWriter;
        this.stream = new BufferedInputStream(inputStream);
        this.position = 0;
        this.chunklist = new Vector<>();
        this.chunktable = new HashMap<>();
    }

    void nextInto(ByteBuffer buf) throws IOException {
        assert buf.hasArray();
        this.position += buf.capacity();
        buf.rewind();
        stream.read(buf.array());
        buf.rewind();
    }

    short nextShort() throws IOException {
        nextInto(shortBuf);
        return shortBuf.getShort();
    }

    int nextInt() throws IOException {
        nextInto(intBuf);
        return intBuf.getInt();
    }

    long nextLong() throws IOException {
        nextInto(longBuf);
        return longBuf.getLong();
    }

    int readVersion() throws IOException {
        int version = nextInt();
        assert version == 0x00001979;
        return version;
    }

    void readBaseHeader() throws IOException {
        headerSize = nextInt();
        endOfMemory = nextInt();
        oldBaseAddress = nextInt();
        specialObjectsPointer = nextInt();
        lastHash = nextInt();
        lastWindowSize = nextInt();
        wasFullscreen = nextInt();
        extraVMMemory = nextInt();
    }

    void readSpurHeader() throws IOException {
        numStackPages = nextShort();
        cogCodeSize = nextShort();
        edenBytes = nextInt();
        maxExternalSemaphoreTableSize = nextShort();
        nextShort(); // re-align
        firstSegmentSize = nextInt();
        freeOldSpace = nextInt();
    }

    void readHeader() throws IOException {
        readVersion();
        readBaseHeader();
        readSpurHeader();
        skipToBody();
    }

    void skipToBody() throws IOException {
        int skip = headerSize - this.position;
        this.stream.skip(skip);
        this.position += skip;
    }

    void readBody(SqueakImageContext image) throws IOException {
        position = 0;
        int segmentEnd = firstSegmentSize;
        int currentAddressSwizzle = oldBaseAddress;
        while (this.position < segmentEnd) {
            while (this.position < segmentEnd - 16) {
                Chunk chunk = readObject(image);
                if (chunk.classid == FREE_OBJECT_CLASS_INDEX_PUN) {
                    continue;
                }
                chunklist.add(chunk);
                chunktable.put(chunk.pos + currentAddressSwizzle, chunk);
            }
            long bridge = nextLong();
            int bridgeSpan = 0;
            if ((bridge & SLOTS_MASK) != 0) {
                bridgeSpan = (int) (bridge & ~SLOTS_MASK);
            }
            int nextSegmentSize = (int) nextLong();
            assert bridgeSpan >= 0;
            assert nextSegmentSize >= 0;
            assert position == segmentEnd;
            if (nextSegmentSize == 0) {
                break;
            }
            segmentEnd = segmentEnd + nextSegmentSize;
            currentAddressSwizzle += bridgeSpan * 4;
        }
        this.stream.close();
    }

    private Chunk readObject(SqueakImageContext image) throws IOException {
        log("o");
        int pos = position;
        assert pos % 8 == 0;
        long headerWord = nextLong();
        // 22 2 5 3 22 2 8
        // classid _ format _ hash _ size
        int[] splitHeader = BitSplitter.splitter(headerWord, new int[]{22, 2, 5, 3, 22, 2, 8});
        int size = splitHeader[6];
        if (size == OVERFLOW_SLOTS) {
            size = (int) (headerWord & ~SLOTS_MASK);
            pos = position;
            headerWord = nextLong();
            splitHeader = BitSplitter.splitter(headerWord, new int[]{22, 2, 5, 3, 22, 2, 8});
            int overflowSize = splitHeader[6];
            assert overflowSize == OVERFLOW_SLOTS;
        }
        int classid = splitHeader[0];
        int format = splitHeader[2];
        int hash = splitHeader[4];
        assert size >= 0;
        assert 0 <= format && format <= 31;
        Chunk chunk = new Chunk(this, image, size, format, classid, hash, pos);
        for (long i = 0; i < wordsFor(size); i++) {
            if (chunk.size() < size) {
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

    private void log(String string) {
        if (output != null) {
            // output.write(string);
        }
    }

    long wordsFor(long size) {
        // see Spur32BitMemoryManager>>smallObjectBytesForSlots:
        return size <= 1 ? 2 : size + (size & 1);
    }

    Chunk specialObjectChunk(int idx) {
        Chunk specialObjectsChunk = chunktable.get(specialObjectsPointer);
        return chunktable.get(specialObjectsChunk.data().get(idx));
    }

    void setPrebuiltObject(int idx, Object object) {
        specialObjectChunk(idx).object = object;
    }

    void initPrebuiltConstant(SqueakImageContext image) {
        Chunk specialObjectsChunk = chunktable.get(specialObjectsPointer);
        specialObjectsChunk.object = image.specialObjectsArray;

        // first we find the Metaclass, we need it to correctly instantiate
        // those classes that do not have any instances. Metaclass always
        // has instances, and all instances of Metaclass have their singleton
        // Behavior instance, so these are all correctly initialized already
        Chunk Array = classChunkOf(specialObjectsChunk, image);
        Chunk Array_class = classChunkOf(Array, image);
        Chunk Metaclass = classChunkOf(Array_class, image);
        Metaclass.object = image.metaclass;

        // also cache nil, true, and false classes
        classChunkOf(specialObjectChunk(0), image).object = image.nilClass;
        classChunkOf(specialObjectChunk(1), image).object = image.falseClass;
        classChunkOf(specialObjectChunk(2), image).object = image.trueClass;

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

    void initPrebuiltSelectors(SqueakImageContext image) {
        NativeObject[] specialSelectors = new NativeObject[]{
                        image.plus, image.minus, image.lt, image.gt, image.le, image.ge,
                        image.eq, image.ne, image.times, image.div, image.modulo, image.pointAt,
                        image.bitShift, image.divide, image.bitAnd, image.bitOr, image.at,
                        image.atput, image.size_, image.next, image.nextPut, image.atEnd,
                        image.equivalent, image.klass, image.blockCopy, image.value,
                        image.valueWithArg, image.do_, image.new_, image.newWithArg,
                        image.x, image.y
        };

        Chunk specialObjectsChunk = chunktable.get(specialObjectsPointer);
        Chunk specialSelectorChunk = chunktable.get(specialObjectsChunk.data().get(SPECIAL_SELECTORS_INDEX));

        for (int i = 0; i < specialSelectors.length; i++) {
            chunktable.get(specialSelectorChunk.data().get(i * 2)).object = specialSelectors[i];
        }
    }

    void initObjects(SqueakImageContext image) {
        initPrebuiltConstant(image);
        initPrebuiltSelectors(image);

        // connect all instances to their classes
        output.println("Connect classes");
        for (Chunk chunk : chunklist) {
            chunk.setSqClass(classOf(chunk, image));
        }

        output.println("Instantiate classes");
        // find all metaclasses and instantiate their singleton instances as class objects
        for (int classtablePtr : chunklist.get(HIDDEN_ROOTS_CHUNK).data()) {
            if (chunktable.get(classtablePtr) != null) {
                for (int potentialClassPtr : chunktable.get(classtablePtr).data()) {
                    Chunk metaClass = chunktable.get(potentialClassPtr);
                    if (metaClass != null) {
                        if (metaClass.getSqClass() == image.metaclass) {
                            Chunk classInstance = chunktable.get(metaClass.data().lastElement());
                            assert metaClass.data().size() == 6;
                            metaClass.asClassObject();
                            classInstance.asClassObject();
                        }
                    }
                }
            }
        }

        // fillin objects
        output.println("Fillin Objects");
        for (Chunk chunk : chunklist) {
            Object chunkObject = chunk.asObject();
            if (chunkObject instanceof SqueakObject) {
                ((SqueakObject) chunkObject).fillin(chunk);
            }
        }

        output.println();
    }

    Chunk classChunkOf(Chunk chunk, SqueakImageContext image) {
        int majorIdx = majorClassIndexOf(chunk.classid);
        int minorIdx = minorClassIndexOf(chunk.classid);
        Chunk hiddenRoots = chunklist.get(HIDDEN_ROOTS_CHUNK);
        Chunk classTablePage = chunktable.get(hiddenRoots.data().get(majorIdx));
        return chunktable.get(classTablePage.data().get(minorIdx));
    }

    ClassObject classOf(Chunk chunk, SqueakImageContext image) {
        return (ClassObject) classChunkOf(chunk, image).asClassObject();
    }

    int majorClassIndexOf(int classid) {
        return classid >> 10;
    }

    int minorClassIndexOf(int classid) {
        return classid & ((1 << 10) - 1);
    }

    public void readImage(SqueakImageContext image) throws IOException {
        readHeader();
        readBody(image);
        initObjects(image);
    }

    public static void readImage(SqueakImageContext squeakImageContext, FileInputStream inputStream) throws IOException {
        ImageReader instance = new ImageReader(inputStream, squeakImageContext.getOutput());
        instance.readImage(squeakImageContext);
    }
}
