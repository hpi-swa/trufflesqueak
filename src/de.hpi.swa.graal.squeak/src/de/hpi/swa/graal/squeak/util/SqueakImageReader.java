package de.hpi.swa.graal.squeak.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hpi.swa.graal.squeak.SqueakImageContext;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.BaseSqueakObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.MiscellaneousPrimitives.SimulationPrimitiveNode;

@SuppressWarnings("unused")
public final class SqueakImageReader {
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
    private int extraVMMemory;
    private short numStackPages;
    private short cogCodeSize;
    private int edenBytes;
    private short maxExternalSemaphoreTableSize;
    private int firstSegmentSize;
    private int freeOldSpace;
    private int position = 0;
    private List<AbstractImageChunk> chunklist = new ArrayList<>();
    protected HashMap<Integer, AbstractImageChunk> chunktable = new HashMap<>();
    private PrintWriter output;

    public static void readImage(final SqueakImageContext squeakImageContext, final FileInputStream inputStream) throws IOException {
        final SqueakImageReader instance = new SqueakImageReader(inputStream, squeakImageContext.getOutput());
        instance.readImage(squeakImageContext);
    }

    private SqueakImageReader(final FileInputStream inputStream, final PrintWriter printWriter) throws FileNotFoundException {
        shortBuf.order(ByteOrder.nativeOrder());
        intBuf.order(ByteOrder.nativeOrder());
        longBuf.order(ByteOrder.nativeOrder());
        output = printWriter;
        stream = new BufferedInputStream(inputStream);
    }

    private void readImage(final SqueakImageContext image) throws IOException {
        readHeader(image);
        readBody(image);
        initObjects(image);
    }

    private void nextInto(final ByteBuffer buf) throws IOException {
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
        final int version = nextInt();
        assert version == 0x00001979;
        return version;
    }

    private void readBaseHeader(final SqueakImageContext image) throws IOException {
        headerSize = nextInt();
        endOfMemory = nextInt();
        oldBaseAddress = nextInt();
        specialObjectsPointer = nextInt();
        lastHash = nextInt();
        final int lastWindowSize = nextInt();
        image.display.resizeTo((lastWindowSize >> 16) & 0xffff, lastWindowSize & 0xffff);
        final int headerFlags = nextInt();
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

    private void readHeader(final SqueakImageContext image) throws IOException {
        readVersion();
        readBaseHeader(image);
        readSpurHeader();
        skipToBody();
    }

    private void skipToBody() throws IOException {
        final int skip = headerSize - this.position;
        this.position += this.stream.skip(skip);
    }

    private void readBody(final SqueakImageContext image) throws IOException {
        position = 0;
        int segmentEnd = firstSegmentSize;
        int currentAddressSwizzle = oldBaseAddress;
        while (this.position < segmentEnd) {
            while (this.position < segmentEnd - 16) {
                final AbstractImageChunk chunk = readObject(image);
                if (chunk.classid == FREE_OBJECT_CLASS_INDEX_PUN) {
                    continue;
                }
                chunklist.add(chunk);
                chunktable.put(chunk.pos + currentAddressSwizzle, chunk);
            }
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
            segmentEnd = segmentEnd + nextSegmentSize;
            currentAddressSwizzle += bridgeSpan * 4;
        }
        this.stream.close();
    }

    private AbstractImageChunk readObject(final SqueakImageContext image) throws IOException {
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
            final int overflowSize = splitHeader[6];
            assert overflowSize == OVERFLOW_SLOTS;
        }
        final int classid = splitHeader[0];
        final int format = splitHeader[2];
        final int hash = splitHeader[4];
        assert size >= 0;
        assert 0 <= format && format <= 31;
        final AbstractImageChunk chunk = new SqueakImageChunk(this, image, size, format, classid, hash, pos);
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

    private void log(final String string) {
        if (output != null) {
            // output.write(string);
        }
    }

    private static long wordsFor(final long size) {
        // see Spur32BitMemoryManager>>smallObjectBytesForSlots:
        return size <= 1 ? 2 : size + (size & 1);
    }

    private AbstractImageChunk specialObjectChunk(final int idx) {
        final AbstractImageChunk specialObjectsChunk = chunktable.get(specialObjectsPointer);
        return chunktable.get(specialObjectsChunk.data().get(idx));
    }

    private void setPrebuiltObject(final int idx, final Object object) {
        specialObjectChunk(idx).object = object;
    }

    private void initPrebuiltConstant(final SqueakImageContext image) {
        final AbstractImageChunk specialObjectsChunk = chunktable.get(specialObjectsPointer);
        specialObjectsChunk.object = image.specialObjectsArray;

        // first we find the Metaclass, we need it to correctly instantiate
        // those classes that do not have any instances. Metaclass always
        // has instances, and all instances of Metaclass have their singleton
        // Behavior instance, so these are all correctly initialized already
        final AbstractImageChunk sqArray = classChunkOf(specialObjectsChunk, image);
        final AbstractImageChunk sqArrayClass = classChunkOf(sqArray, image);
        final AbstractImageChunk sqMetaclass = classChunkOf(sqArrayClass, image);
        sqMetaclass.object = image.metaclass;

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

    private void initPrebuiltSelectors(final SqueakImageContext image) {
        final AbstractImageChunk specialObjectsChunk = chunktable.get(specialObjectsPointer);
        final AbstractImageChunk specialSelectorChunk = chunktable.get(specialObjectsChunk.data().get(SPECIAL_SELECTORS_INDEX));

        final NativeObject[] specialSelectors = image.specialSelectorsArray;
        for (int i = 0; i < specialSelectors.length; i++) {
            chunktable.get(specialSelectorChunk.data().get(i * 2)).object = specialSelectors[i];
        }
    }

    private void initObjects(final SqueakImageContext image) {
        initPrebuiltConstant(image);
        initPrebuiltSelectors(image);

        // connect all instances to their classes
        output.println("Connecting classes...");
        for (AbstractImageChunk chunk : chunklist) {
            chunk.setSqClass(classOf(chunk, image));
        }

        // find all metaclasses and instantiate their singleton instances as class objects
        output.println("Instantiating classes...");
        for (int classtablePtr : chunklist.get(HIDDEN_ROOTS_CHUNK).data()) {
            if (chunktable.get(classtablePtr) != null) {
                for (int potentialClassPtr : chunktable.get(classtablePtr).data()) {
                    final AbstractImageChunk metaClass = chunktable.get(potentialClassPtr);
                    if (metaClass != null && metaClass.getSqClass() == image.metaclass) {
                        final List<Integer> data = metaClass.data();
                        final AbstractImageChunk classInstance = chunktable.get(data.get(data.size() - 1));
                        assert data.size() == 6;
                        metaClass.asClassObject();
                        classInstance.asClassObject();
                    }
                }
            }
        }

        output.println("Filling in objects...");
        for (AbstractImageChunk chunk : chunklist) {
            final Object chunkObject = chunk.asObject();
            if (chunkObject instanceof BaseSqueakObject) {
                ((BaseSqueakObject) chunkObject).fillin(chunk);
            }
            if (chunkObject instanceof NativeObject && ((NativeObject) chunkObject).getSqClass() == image.doesNotUnderstand.getSqClass()) { // check
                                                                                                                                            // ByteSymbols
                if (chunkObject.toString().equals("asSymbol")) {
                    image.asSymbol = (NativeObject) chunkObject;
                } else if (chunkObject.toString().equals(SimulationPrimitiveNode.SIMULATE_PRIMITIVE_SELECTOR)) {
                    image.simulatePrimitiveArgs = (NativeObject) chunkObject;
                }
            }
        }
        if (image.asSymbol.isNil()) {
            throw new SqueakException("Unable to find asSymbol selector");
        }
    }

    private AbstractImageChunk classChunkOf(final AbstractImageChunk chunk, final SqueakImageContext image) {
        final int majorIdx = majorClassIndexOf(chunk.classid);
        final int minorIdx = minorClassIndexOf(chunk.classid);
        final AbstractImageChunk hiddenRoots = chunklist.get(HIDDEN_ROOTS_CHUNK);
        final AbstractImageChunk classTablePage = chunktable.get(hiddenRoots.data().get(majorIdx));
        return chunktable.get(classTablePage.data().get(minorIdx));
    }

    private ClassObject classOf(final AbstractImageChunk chunk, final SqueakImageContext image) {
        return (ClassObject) classChunkOf(chunk, image).asClassObject();
    }

    private static int majorClassIndexOf(final int classid) {
        return classid >> 10;
    }

    private static int minorClassIndexOf(final int classid) {
        return classid & ((1 << 10) - 1);
    }
}
