package de.hpi.swa.trufflesqueak.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Vector;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.SqueakObject;

@SuppressWarnings("unused")
public class ImageReader {
    private static final int FREE_OBJECT_CLASS_INDEX_PUN = 0;
    private static final long SLOTS_MASK = 0xFF << 56;
    private static final long OVERFLOW_SLOTS = 255;
    private static final int HIDDEN_ROOTS_CHUNK = 4; // nil, false, true, freeList, hiddenRoots
    final FileInputStream stream;
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

    public ImageReader(FileInputStream inputStream) throws FileNotFoundException {
        shortBuf.order(ByteOrder.LITTLE_ENDIAN);
        intBuf.order(ByteOrder.LITTLE_ENDIAN);
        longBuf.order(ByteOrder.LITTLE_ENDIAN);
        this.stream = inputStream;
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

    void readBody() throws IOException {
        position = 0;
        int segmentEnd = firstSegmentSize;
        int currentAddressSwizzle = oldBaseAddress;
        while (this.position < segmentEnd) {
            while (this.position < segmentEnd - 16) {
                Chunk chunk = readObject();
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

    private Chunk readObject() throws IOException {
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
        Chunk chunk = new Chunk(this, size, format, classid, hash, pos);
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

    long wordsFor(long size) {
        // see Spur32BitMemoryManager>>smallObjectBytesForSlots:
        return size <= 1 ? 2 : size + (size & 1);
    }

    void setPrebuiltObject(int idx, SqueakObject object) {
        Chunk specialObjectsChunk = chunktable.get(specialObjectsPointer);
        chunktable.get(specialObjectsChunk.data().get(idx)).object = object;
    }

    void initPrebuiltConstant(SqueakImageContext image) {
        Chunk specialObjectsChunk = chunktable.get(specialObjectsPointer);
        specialObjectsChunk.object = image.specialObjectsArray;
        setPrebuiltObject(0, image.nil);
        setPrebuiltObject(1, image.sqFalse);
        setPrebuiltObject(2, image.sqTrue);
        setPrebuiltObject(3, image.schedulerAssociation);
        setPrebuiltObject(5, image.smallIntegerClass);
        setPrebuiltObject(8, image.smalltalk);
        setPrebuiltObject(19, image.characterClass);
    }

    void initObjects(SqueakImageContext image) {
        initPrebuiltConstant(image);

        // connect classes
        for (Chunk chunk : chunklist) {
            chunk.setSqClass(classOf(chunk));
        }
        // fillin objects
        for (Chunk chunk : chunklist) {
            chunk.asObject().fillin(chunk, image);
        }

        image.metaclass = (PointersObject) image.characterClass.getSqClass().getSqClass();
    }

    BaseSqueakObject classOf(Chunk chunk) {
        int majorIdx = majorClassIndexOf(chunk.classid);
        int minorIdx = minorClassIndexOf(chunk.classid);
        Chunk hiddenRoots = chunklist.get(HIDDEN_ROOTS_CHUNK);
        Chunk classTablePage = chunktable.get(hiddenRoots.data().get(majorIdx));
        SqueakObject sqClass = chunktable.get(classTablePage.data().get(minorIdx)).asObject();
        assert sqClass instanceof PointersObject;
        return sqClass;
    }

    int majorClassIndexOf(int classid) {
        return classid >> 10;
    }

    int minorClassIndexOf(int classid) {
        return classid & ((1 << 10) - 1);
    }

    public void readImage(SqueakImageContext image) throws IOException {
        readHeader();
        readBody();
        initObjects(image);
    }

    public static void readImage(SqueakImageContext squeakImageContext, FileInputStream inputStream) throws IOException {
        ImageReader instance = new ImageReader(inputStream);
        instance.readImage(squeakImageContext);
    }
}
