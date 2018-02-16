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
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;

@SuppressWarnings("unused")
public class SqueakImageReader {
    public static final Object NIL_OBJECT_PLACEHOLDER = new Object();
    private static final int SPECIAL_SELECTORS_INDEX = 23;
    private static final int FREE_OBJECT_CLASS_INDEX_PUN = 0;
    private static final long SLOTS_MASK = 0xFF << 56;
    private static final long OVERFLOW_SLOTS = 255;
    private static final int HIDDEN_ROOTS_CHUNK = 4; // nil, false, true, freeList, hiddenRoots
    private final BufferedInputStream stream;
    private final ByteBuffer shortBuf = ByteBuffer.allocate(2);
    private final ByteBuffer intBuf = ByteBuffer.allocate(4);
    private final ByteBuffer longBuf = ByteBuffer.allocate(8);
    private int headerSize;
    private int endOfMemory;
    private int oldBaseAddress;
    private int specialObjectsPointer;
    private int lastHash;
    private int lastWindowSize;
    private int headerFlags;
    private int extraVMMemory;
    private short numStackPages;
    private short cogCodeSize;
    private int edenBytes;
    private short maxExternalSemaphoreTableSize;
    private int firstSegmentSize;
    private int freeOldSpace;
    private int position = 0;
    private Vector<SqueakImageChunk> chunklist = new Vector<>();
    HashMap<Integer, SqueakImageChunk> chunktable = new HashMap<>();
    private PrintWriter output;

    public static void readImage(SqueakImageContext squeakImageContext, FileInputStream inputStream) throws IOException {
        SqueakImageReader instance = new SqueakImageReader(inputStream, squeakImageContext.getOutput());
        instance.readImage(squeakImageContext);
    }

    private SqueakImageReader(FileInputStream inputStream, PrintWriter printWriter) throws FileNotFoundException {
        shortBuf.order(ByteOrder.nativeOrder());
        intBuf.order(ByteOrder.nativeOrder());
        longBuf.order(ByteOrder.nativeOrder());
        output = printWriter;
        stream = new BufferedInputStream(inputStream);
    }

    private void readImage(SqueakImageContext image) throws IOException {
        readHeader(image);
        readBody(image);
        initObjects(image);
    }

    private void nextInto(ByteBuffer buf) throws IOException {
        assert buf.hasArray();
        this.position += buf.capacity();
        buf.rewind();
        stream.read(buf.array());
        buf.rewind();
    }

    private short nextShort() throws IOException {
        nextInto(shortBuf);
        return shortBuf.getShort();
    }

    private int nextInt() throws IOException {
        nextInto(intBuf);
        return intBuf.getInt();
    }

    private long nextLong() throws IOException {
        nextInto(longBuf);
        return longBuf.getLong();
    }

    private int readVersion() throws IOException {
        int version = nextInt();
        assert version == 0x00001979;
        return version;
    }

    private void readBaseHeader(SqueakImageContext image) throws IOException {
        headerSize = nextInt();
        endOfMemory = nextInt();
        oldBaseAddress = nextInt();
        specialObjectsPointer = nextInt();
        lastHash = nextInt();
        lastWindowSize = nextInt();
        headerFlags = nextInt();
        image.flags.initialize(headerFlags);
        extraVMMemory = nextInt();
    }

    private void readSpurHeader() throws IOException {
        numStackPages = nextShort();
        cogCodeSize = nextShort();
        edenBytes = nextInt();
        maxExternalSemaphoreTableSize = nextShort();
        nextShort(); // re-align
        firstSegmentSize = nextInt();
        freeOldSpace = nextInt();
    }

    private void readHeader(SqueakImageContext image) throws IOException {
        readVersion();
        readBaseHeader(image);
        readSpurHeader();
        skipToBody();
    }

    private void skipToBody() throws IOException {
        int skip = headerSize - this.position;
        this.position += this.stream.skip(skip);
    }

    private void readBody(SqueakImageContext image) throws IOException {
        position = 0;
        int segmentEnd = firstSegmentSize;
        int currentAddressSwizzle = oldBaseAddress;
        while (this.position < segmentEnd) {
            while (this.position < segmentEnd - 16) {
                SqueakImageChunk chunk = readObject(image);
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

    private SqueakImageChunk readObject(SqueakImageContext image) throws IOException {
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
        SqueakImageChunk chunk = new SqueakImageChunk(this, image, size, format, classid, hash, pos);
        for (long i = 0; i < wordsFor(size); i++) {
            if (chunk.size() < size) {
                chunk.append(nextInt());
            } else {
                nextInt(); // don't add trailing alignment words
            }
        }
        if (format < 10 && classid != FREE_OBJECT_CLASS_INDEX_PUN) {
            for (long slot : chunk.data()) {
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

    private static long wordsFor(long size) {
        // see Spur32BitMemoryManager>>smallObjectBytesForSlots:
        return size <= 1 ? 2 : size + (size & 1);
    }

    private SqueakImageChunk specialObjectChunk(int idx) {
        SqueakImageChunk specialObjectsChunk = chunktable.get(specialObjectsPointer);
        return chunktable.get(specialObjectsChunk.data().get(idx));
    }

    private void setPrebuiltObject(int idx, Object object) {
        specialObjectChunk(idx).object = object;
    }

    private void initPrebuiltConstant(SqueakImageContext image) {
        SqueakImageChunk specialObjectsChunk = chunktable.get(specialObjectsPointer);
        specialObjectsChunk.object = image.specialObjectsArray;

        // first we find the Metaclass, we need it to correctly instantiate
        // those classes that do not have any instances. Metaclass always
        // has instances, and all instances of Metaclass have their singleton
        // Behavior instance, so these are all correctly initialized already
        SqueakImageChunk Array = classChunkOf(specialObjectsChunk, image);
        SqueakImageChunk Array_class = classChunkOf(Array, image);
        SqueakImageChunk Metaclass = classChunkOf(Array_class, image);
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

    private void initPrebuiltSelectors(SqueakImageContext image) {
        SqueakImageChunk specialObjectsChunk = chunktable.get(specialObjectsPointer);
        SqueakImageChunk specialSelectorChunk = chunktable.get(specialObjectsChunk.data().get(SPECIAL_SELECTORS_INDEX));

        NativeObject[] specialSelectors = image.specialSelectorsArray;
        for (int i = 0; i < specialSelectors.length; i++) {
            chunktable.get(specialSelectorChunk.data().get(i * 2)).object = specialSelectors[i];
        }
    }

    private void initObjects(SqueakImageContext image) {
        initPrebuiltConstant(image);
        initPrebuiltSelectors(image);

        // connect all instances to their classes
        output.println("Connecting classes...");
        for (SqueakImageChunk chunk : chunklist) {
            chunk.setSqClass(classOf(chunk, image));
        }

        // find all metaclasses and instantiate their singleton instances as class objects
        output.println("Instantiating classes...");
        for (int classtablePtr : chunklist.get(HIDDEN_ROOTS_CHUNK).data()) {
            if (chunktable.get(classtablePtr) != null) {
                for (int potentialClassPtr : chunktable.get(classtablePtr).data()) {
                    SqueakImageChunk metaClass = chunktable.get(potentialClassPtr);
                    if (metaClass != null) {
                        if (metaClass.getSqClass() == image.metaclass) {
                            SqueakImageChunk classInstance = chunktable.get(metaClass.data().lastElement());
                            assert metaClass.data().size() == 6;
                            metaClass.asClassObject();
                            classInstance.asClassObject();
                        }
                    }
                }
            }
        }

        output.println("Filling in objects...");
        for (SqueakImageChunk chunk : chunklist) {
            Object chunkObject = chunk.asObject();
            if (chunkObject instanceof BaseSqueakObject) {
                ((BaseSqueakObject) chunkObject).fillin(chunk);
            }
            if (chunkObject instanceof NativeObject) {
                if (((NativeObject) chunkObject).getSqClass() == image.doesNotUnderstand.getSqClass() && chunkObject.toString().equals("asSymbol")) {
                    image.asSymbol = (NativeObject) chunkObject;
                } else if (((NativeObject) chunkObject).getSqClass() == image.doesNotUnderstand.getSqClass() && chunkObject.toString().equals("simulatePrimitive:args:")) {
                    image.simulatePrimitiveArgs = (NativeObject) chunkObject;
                }
            }
        }
        if (image.asSymbol == image.nil) {
            throw new SqueakException("Unable to find asSymbol selector");
        } else if (image.simulatePrimitiveArgs == image.nil) {
            image.getError().println("Unable to find BitBlt simulation in image, can only run in headless mode...");
        }
    }

    private SqueakImageChunk classChunkOf(SqueakImageChunk chunk, SqueakImageContext image) {
        int majorIdx = majorClassIndexOf(chunk.classid);
        int minorIdx = minorClassIndexOf(chunk.classid);
        SqueakImageChunk hiddenRoots = chunklist.get(HIDDEN_ROOTS_CHUNK);
        SqueakImageChunk classTablePage = chunktable.get(hiddenRoots.data().get(majorIdx));
        return chunktable.get(classTablePage.data().get(minorIdx));
    }

    private ClassObject classOf(SqueakImageChunk chunk, SqueakImageContext image) {
        return (ClassObject) classChunkOf(chunk, image).asClassObject();
    }

    private static int majorClassIndexOf(int classid) {
        return classid >> 10;
    }

    private static int minorClassIndexOf(int classid) {
        return classid & ((1 << 10) - 1);
    }
}
