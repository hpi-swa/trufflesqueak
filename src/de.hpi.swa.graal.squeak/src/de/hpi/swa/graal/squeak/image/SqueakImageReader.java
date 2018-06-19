package de.hpi.swa.graal.squeak.image;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.Node.Child;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.FillInNode;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.MiscellaneousPrimitives.SimulationPrimitiveNode;
import de.hpi.swa.graal.squeak.util.BitSplitter;
import de.hpi.swa.graal.squeak.util.StopWatch;

@SuppressWarnings("unused")
public final class SqueakImageReader extends Node {
    @CompilationFinal static final Object NIL_OBJECT_PLACEHOLDER = new Object();
    @CompilationFinal private static final int SPECIAL_SELECTORS_INDEX = 23;
    @CompilationFinal private static final int FREE_OBJECT_CLASS_INDEX_PUN = 0;
    @CompilationFinal private static final long SLOTS_MASK = 0xFF << 56;
    @CompilationFinal private static final long OVERFLOW_SLOTS = 255;
    @CompilationFinal private static final int HIDDEN_ROOTS_CHUNK = 4;
    @CompilationFinal private final BufferedInputStream stream;
    @CompilationFinal private final List<AbstractImageChunk> chunklist = new ArrayList<>();
    @CompilationFinal private final HashMap<Integer, AbstractImageChunk> chunktable = new HashMap<>();
    private int headerSize;
    private int oldBaseAddress;
    private int specialObjectsPointer;
    private short maxExternalSemaphoreTableSize;
    private int firstSegmentSize;
    private int position = 0;
    private final PrintWriter output;
    @CompilationFinal private final SqueakImageContext image;

    @Child private FillInNode fillInNode;

    public SqueakImageReader(final InputStream inputStream, final SqueakImageContext image) {
        stream = new BufferedInputStream(inputStream);
        output = image.getOutput();
        this.image = image;
        fillInNode = FillInNode.create(image);
    }

    public void executeRead(final VirtualFrame frame) throws IOException, SqueakException {
        output.println("Reading image...");
        final StopWatch imageWatch = StopWatch.start("readImage");
        output.println("Reading header...");
        final StopWatch headerWatch = StopWatch.start("readHeader");
        readHeader();
        headerWatch.stopAndPrint();
        output.println("Reading body...");
        final StopWatch bodyWatch = StopWatch.start("readBody");
        readBody();
        bodyWatch.stopAndPrint();
        initObjects(frame);
        imageWatch.stopAndPrint();
        if (!image.display.isHeadless() && image.simulatePrimitiveArgs.isNil()) {
            throw new SqueakException("Unable to find BitBlt simulation in image, cannot run with display.");
        }
    }

    private short nextShort() throws IOException {
        final byte[] bytes = new byte[2];
        stream.read(bytes, 0, 2);
        this.position += 2;
        short value = 0;
        value += (bytes[1] & 0x000000FF) << 8;
        value += (bytes[0] & 0x000000FF);
        return value;
    }

    private int nextInt() throws IOException {
        final byte[] bytes = new byte[4];
        stream.read(bytes, 0, 4);
        this.position += 4;
        int value = 0;
        value += (bytes[3] & 0x000000FF) << 24;
        value += (bytes[2] & 0x000000FF) << 16;
        value += (bytes[1] & 0x000000FF) << 8;
        value += (bytes[0] & 0x000000FF);
        return value;
    }

    private long nextLong() throws IOException {
        final byte[] bytes = new byte[8];
        stream.read(bytes, 0, 8);
        this.position += 8;
        long value = 0;
        value += (long) (bytes[7] & 0x000000FF) << 56;
        value += (long) (bytes[6] & 0x000000FF) << 48;
        value += (long) (bytes[5] & 0x000000FF) << 40;
        value += (long) (bytes[4] & 0x000000FF) << 32;
        value += (bytes[3] & 0x000000FF) << 24;
        value += (bytes[2] & 0x000000FF) << 16;
        value += (bytes[1] & 0x000000FF) << 8;
        value += (bytes[0] & 0x000000FF);
        return value;
    }

    private int readVersion() throws IOException {
        final int version = nextInt();
        assert version == 0x00001979;
        return version;
    }

    private void readBaseHeader() throws IOException {
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

    private void readSpurHeader() throws IOException {
        nextShort(); // numStackPages
        nextShort(); // cogCodeSize
        nextInt(); // edenBytes
        maxExternalSemaphoreTableSize = nextShort();
        nextShort(); // re-align
        firstSegmentSize = nextInt();
        nextInt(); // freeOldSpace
    }

    private void readHeader() throws IOException {
        readVersion();
        readBaseHeader();
        readSpurHeader();
        skipToBody();
    }

    private void skipToBody() throws IOException {
        final int skip = headerSize - this.position;
        this.position += this.stream.skip(skip);
    }

    private void readBody() throws IOException {
        position = 0;
        int segmentEnd = firstSegmentSize;
        int currentAddressSwizzle = oldBaseAddress;
        while (this.position < segmentEnd) {
            while (this.position < segmentEnd - 16) {
                final AbstractImageChunk chunk = readObject();
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

    private AbstractImageChunk readObject() throws IOException {
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
        for (int i = 0; i < wordsFor(size); i++) {
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

    private static int wordsFor(final int size) {
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

    private void initPrebuiltConstant() {
        final AbstractImageChunk specialObjectsChunk = chunktable.get(specialObjectsPointer);
        specialObjectsChunk.object = image.specialObjectsArray;

        // first we find the Metaclass, we need it to correctly instantiate
        // those classes that do not have any instances. Metaclass always
        // has instances, and all instances of Metaclass have their singleton
        // Behavior instance, so these are all correctly initialized already
        final AbstractImageChunk sqArray = classChunkOf(specialObjectsChunk);
        final AbstractImageChunk sqArrayClass = classChunkOf(sqArray);
        final AbstractImageChunk sqMetaclass = classChunkOf(sqArrayClass);
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

    private void initPrebuiltSelectors() {
        final AbstractImageChunk specialObjectsChunk = chunktable.get(specialObjectsPointer);
        final AbstractImageChunk specialSelectorChunk = chunktable.get(specialObjectsChunk.data().get(SPECIAL_SELECTORS_INDEX));

        final NativeObject[] specialSelectors = image.specialSelectorsArray;
        for (int i = 0; i < specialSelectors.length; i++) {
            chunktable.get(specialSelectorChunk.data().get(i * 2)).object = specialSelectors[i];
        }
    }

    private void initObjects(final VirtualFrame frame) {
        initPrebuiltConstant();
        initPrebuiltSelectors();
        // connect all instances to their classes
        output.println("Connecting classes...");
        final StopWatch setClassesWatch = StopWatch.start("setClasses");
        for (AbstractImageChunk chunk : chunklist) {
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

    private void instantiateClasses() {
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
    }

    private void fillInObjects(final VirtualFrame frame) {
        output.println("Filling in objects...");
        // final StopWatch watch = new StopWatch("fillInWatch");
        // long maxDuration = 0;
        for (AbstractImageChunk chunk : chunklist) {
            // watch.start();
            final Object chunkObject = chunk.asObject();
            fillInNode.execute(frame, chunkObject, chunk);
            // final long duration = watch.stop();
            // if (duration >= maxDuration) {
            // maxDuration = duration;
            // }
        }
        // final double deltaf = (maxDuration / 1000_000);
        // output.println("maxFillInDuration" + ":\t" + deltaf + "ms");
        if (image.asSymbol.isNil()) {
            throw new SqueakException("Unable to find asSymbol selector");
        }
    }

    private AbstractImageChunk classChunkOf(final AbstractImageChunk chunk) {
        final int majorIdx = majorClassIndexOf(chunk.classid);
        final int minorIdx = minorClassIndexOf(chunk.classid);
        final AbstractImageChunk hiddenRoots = chunklist.get(HIDDEN_ROOTS_CHUNK);
        final AbstractImageChunk classTablePage = chunktable.get(hiddenRoots.data().get(majorIdx));
        return chunktable.get(classTablePage.data().get(minorIdx));
    }

    private static int majorClassIndexOf(final int classid) {
        return classid >> 10;
    }

    private static int minorClassIndexOf(final int classid) {
        return classid & ((1 << 10) - 1);
    }

    public AbstractImageChunk getChunk(final int ptr) {
        return chunktable.get(ptr);
    }
}
