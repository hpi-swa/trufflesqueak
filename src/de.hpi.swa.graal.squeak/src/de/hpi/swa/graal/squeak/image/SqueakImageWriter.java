/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.image;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakAbortException;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.io.DisplayPoint;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObjectWithHash;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BooleanObject;
import de.hpi.swa.graal.squeak.model.CharacterObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.util.MiscUtils;
import de.hpi.swa.graal.squeak.util.ObjectGraphUtils;
import de.hpi.swa.graal.squeak.util.UnsafeUtils;

public final class SqueakImageWriter {
    private final SqueakImageContext image;
    private final NativeObject freeList;
    private final BufferedOutputStream stream;
    private final byte[] byteArrayBuffer = new byte[Long.BYTES];
    private final HashMap<AbstractSqueakObjectWithHash, Long> oopMap = new HashMap<>(ObjectGraphUtils.getLastSeenObjects());
    private final ArrayList<AbstractSqueakObjectWithHash> allTracedObjects = new ArrayList<>(ObjectGraphUtils.getLastSeenObjects());
    private final ArrayDeque<AbstractSqueakObjectWithHash> traceQueue = new ArrayDeque<>();
    private final ArrayList<AbstractSqueakObjectWithHash> additionalBoxedObjects = new ArrayList<>();

    private long position;
    private long nextChunk;
    private long nextChunkAfterTracing;

    private long nilOop;
    private long falseOop;
    private long trueOop;
    private long specialObjectOop;
    private long freeListOop;
    private long hiddenRootsOop;

    private SqueakImageWriter(final SqueakImageContext image) {
        this.image = image;
        final TruffleFile truffleFile = image.env.getPublicTruffleFile(image.getImagePath());
        try {
            stream = new BufferedOutputStream(truffleFile.newOutputStream());
        } catch (final IOException e) {
            e.printStackTrace();
            throw SqueakException.illegalState(e);
        }
        freeList = NativeObject.newNativeLongs(image, image.nilClass /* ignored */, SqueakImageConstants.NUM_FREE_LISTS);
    }

    /*
     * Image writing is a rare operation and should therefore be excluded from Truffle compilation.
     */
    @TruffleBoundary
    public static void write(final SqueakImageContext image, final ContextObject thisContext) {
        new SqueakImageWriter(image).run(thisContext);
    }

    public SqueakImageContext getImage() {
        return image;
    }

    private void run(final ContextObject thisContext) {
        final long start = MiscUtils.currentTimeMillis();
        nextChunk = image.flags.getOldBaseAddress();
        final PointersObject activeProcess = image.getActiveProcess(AbstractPointersObjectReadNode.getUncached());
        try {
            /* Mark thisContext as suspended during tracing and writing. */
            AbstractPointersObjectWriteNode.getUncached().execute(activeProcess, PROCESS.SUSPENDED_CONTEXT, thisContext);
            traceObjects();
            writeImageHeader();
            writeBody();
        } finally {
            /* Unmark thisContext as suspended. */
            AbstractPointersObjectWriteNode.getUncached().executeNil(activeProcess, PROCESS.SUSPENDED_CONTEXT);
            closeStream();
            finalizeImageHeader();
        }
        final double fileSize = Math.ceil((double) position / 1024 / 1024 * 100) / 100;
        image.printToStdOut("Image saved in " + (MiscUtils.currentTimeMillis() - start) + "ms (" + fileSize + "MiB).");
    }

    private void writeImageHeader() {
        assert position == 0;
        /* Write basic header. */
        writeInt(SqueakImageConstants.IMAGE_FORMAT);
        writeInt(SqueakImageConstants.IMAGE_HEADER_SIZE); // hdr size
        assert position == SqueakImageConstants.IMAGE_HEADER_MEMORY_SIZE_POSITION;
        writeLong(0); // memory size (yet unknown, see finalizeFileHeader)
        writeLong(image.flags.getOldBaseAddress()); // oldBaseAddress
        writeLong(specialObjectOop);
        writeLong(0xffee); // last hash
        final DisplayPoint displaySize = image.getDisplay().getWindowSize();
        writeLong(displaySize.getWidth() << 16 | displaySize.getHeight());
        writeLong(SqueakImageConstants.IMAGE_HEADER_FLAGS);
        writeInt(0); // extra VM memory
        /* Continue with Spur header. */
        writeInt(0); // (num stack pages << 16) | cog code size
        writeInt(0); // eden bytes
        writeInt(0); // max ext semaphore size << 16
        assert position == SqueakImageConstants.IMAGE_HEADER_FIRST_FRAGMENT_SIZE_POSITION;
        writeLong(0); // first segment size (yet unknown, see finalizeFileHeader)
        writeLong(0); // free old space in image
        writePadding((int) (SqueakImageConstants.IMAGE_HEADER_SIZE - position)); /* Skip to body. */
    }

    private void traceObjects() {
        /* The first objects need to in this order: */
        assert nextChunk == image.flags.getOldBaseAddress();
        nilOop = nextChunk;
        falseOop = nextChunk += 2 * SqueakImageConstants.WORD_SIZE;
        trueOop = nextChunk += 2 * SqueakImageConstants.WORD_SIZE;
        nextChunk += 2 * SqueakImageConstants.WORD_SIZE;
        freeListOop = reserve(freeList);
        hiddenRootsOop = reserve(image.getHiddenRoots());

        /*
         * Remove freeList and hiddenRoots from object list because they will later be written
         * individually.
         */
        allTracedObjects.clear();
        specialObjectOop = reserve(image.specialObjectsArray);

        AbstractSqueakObjectWithHash currentObject;
        while ((currentObject = traceQueue.pollFirst()) != null) {
            currentObject.trace(this);
        }
        nextChunkAfterTracing = nextChunk;
    }

    public void traceIfNecessary(final AbstractSqueakObjectWithHash object) {
        if (object != null && !oopMap.containsKey(object)) {
            reserve(object);
        }
    }

    public void traceIfNecessary(final Object object) {
        if (object instanceof AbstractSqueakObjectWithHash && !oopMap.containsKey(object)) {
            reserve((AbstractSqueakObjectWithHash) object);
        }
    }

    private void writeBody() {
        assert position == SqueakImageConstants.IMAGE_HEADER_SIZE;
        NilObject.SINGLETON.write(this);
        assert currentOop() == falseOop;
        BooleanObject.write(this, false);
        assert currentOop() == trueOop;
        BooleanObject.write(this, true);
        assert currentOop() == freeListOop;
        freeList.writeAsFreeList(this);
        /* `- SqueakImageConstants.WORD_SIZE` for overflow header. */
        assert currentOop() == hiddenRootsOop - SqueakImageConstants.WORD_SIZE;
        image.getHiddenRoots().writeAsHiddenRoots(this);
        assert currentOop() == specialObjectOop : "First objects not written correctly";
        AbstractSqueakObjectWithHash previousObject = image.getHiddenRoots();
        for (final AbstractSqueakObjectWithHash currentObject : allTracedObjects) {
            assert correctPosition(currentObject) : "Previous object was not written correctly: " + previousObject;
            currentObject.write(this);
            previousObject = currentObject;
        }
        assert currentOop() == nextChunkAfterTracing;
        /* Write additional large integers and boxed floats. */
        for (final AbstractSqueakObjectWithHash value : additionalBoxedObjects) {
            value.write(this);
        }
        assert currentOop() == nextChunk;

        /* Write last bridge. */
        writePadding(SqueakImageConstants.IMAGE_BRIDGE_SIZE);
    }

    private boolean correctPosition(final AbstractSqueakObjectWithHash currentObject) {
        final int offset = currentObject.getNumSlots() < SqueakImageConstants.OVERFLOW_SLOTS ? 0 : SqueakImageConstants.WORD_SIZE;
        return currentOop() + offset == oopMap.get(currentObject);
    }

    /*
     * Memory size and first fragment size (the same value in GraalSqueak's case) are unknown when
     * the image header is written. This updates both values in the header accordingly.
     */
    private void finalizeImageHeader() {
        final TruffleFile truffleFile = image.env.getPublicTruffleFile(image.getImagePath());
        assert truffleFile.exists();
        try {
            final EnumSet<StandardOpenOption> options = EnumSet.<StandardOpenOption> of(StandardOpenOption.WRITE, StandardOpenOption.READ);
            final SeekableByteChannel channel = truffleFile.newByteChannel(options);
            try {
                UnsafeUtils.putLong(byteArrayBuffer, 0, position - SqueakImageConstants.IMAGE_HEADER_SIZE);
                channel.position(SqueakImageConstants.IMAGE_HEADER_MEMORY_SIZE_POSITION);
                channel.write(ByteBuffer.wrap(byteArrayBuffer));
                channel.position(SqueakImageConstants.IMAGE_HEADER_FIRST_FRAGMENT_SIZE_POSITION);
                channel.write(ByteBuffer.wrap(byteArrayBuffer));
            } finally {
                channel.close();
            }
        } catch (final IOException e) {
            e.printStackTrace();
            throw SqueakException.illegalState(e);
        }
    }

    private long currentOop() {
        return image.flags.getOldBaseAddress() + position - SqueakImageConstants.IMAGE_HEADER_SIZE;
    }

    public long toWord(final Object object) {
        assert object != null;
        if (object == NilObject.SINGLETON) {
            return nilOop;
        } else if (object instanceof Boolean) {
            return (boolean) object ? trueOop : falseOop;
        } else if (object instanceof Character) {
            return toTaggedCharacter((char) object);
        } else if (object instanceof CharacterObject) {
            return toTaggedCharacter(((CharacterObject) object).getValue());
        } else if (object instanceof Long) {
            return toTaggedSmallInteger((long) object);
        } else if (object instanceof Double) {
            return toTaggedSmallFloat((double) object);
        } else if (object instanceof AbstractSqueakObject) {
            final Long oop = oopMap.get(object);
            if (oop != null) {
                return oop;
            } else {
                image.printToStdErr("Unreserved object detected: " + object + ". Replacing with nil.");
                return nilOop;
            }
        } else {
            /* Nil out any foreign objects. */
            return nilOop;
        }
    }

    private void insertIntoClassTable(final ClassObject classObject) {
        final int classHash = (int) classObject.getSqueakHash();
        final long majorIndex = SqueakImageConstants.majorClassIndexOf(classHash);
        final long minorIndex = SqueakImageConstants.minorClassIndexOf(classHash);
        if (image.getHiddenRoots().getObject(majorIndex) == NilObject.SINGLETON) {
            ensureConsecutiveClassPagesUpTo(majorIndex);
        }
        final ArrayObject classTablePage = (ArrayObject) image.getHiddenRoots().getObject(majorIndex);
        final Object pageEntry = classTablePage.getObject(minorIndex);
        if (pageEntry == classObject) {
            return; /* Found myself in page (possible because we are re-using hiddenRoots). */
        } else if (pageEntry == NilObject.SINGLETON) {
            /* Free slot found in classTable. */
            classTablePage.setObject(minorIndex, classObject);
        } else {
            /* classIndex clashed, re-hash class until there's no longer a clash. */
            long newMajorIndex = majorIndex;
            long newMinorIndex = minorIndex;
            while (image.getHiddenRoots().getObject(newMajorIndex) != NilObject.SINGLETON || classTablePage.getObject(newMinorIndex) != NilObject.SINGLETON) {
                final int newHash = (int) classObject.rehashForClassTable();
                newMajorIndex = SqueakImageConstants.majorClassIndexOf(newHash);
                newMinorIndex = SqueakImageConstants.minorClassIndexOf(newHash);
            }
            insertIntoClassTable(classObject);
        }
    }

    private ArrayObject newClassPage() {
        final Object[] values = new Object[SqueakImageConstants.CLASS_TABLE_PAGE_SIZE];
        Arrays.fill(values, NilObject.SINGLETON);
        final ArrayObject newClassPage = image.asArrayOfObjects(values);
        reserve(newClassPage);
        return newClassPage;
    }

    /* Are all entries up to numClassTablePages must not be nil (see validClassTableRootPages). */
    private void ensureConsecutiveClassPagesUpTo(final long majorIndex) {
        for (int i = 0; i < majorIndex; i++) {
            if (image.getHiddenRoots().getObject(majorIndex) == NilObject.SINGLETON) {
                image.getHiddenRoots().setObject(majorIndex, newClassPage());
            }
        }
    }

    private long reserve(final AbstractSqueakObjectWithHash object) {
        final int numSlots = object.getNumSlots();
        final int padding = SqueakImageReader.calculateObjectPadding(object.getSqueakClass().getInstanceSpecification());

        final int headerSlots = numSlots < SqueakImageConstants.OVERFLOW_SLOTS ? 1 : 2;
        final int offset = (headerSlots - 1) * SqueakImageConstants.WORD_SIZE;
        final long oop = nextChunk + offset;
        nextChunk += (headerSlots + Math.max(numSlots, 1 /* at least an alignment word */)) * SqueakImageConstants.WORD_SIZE + padding;

        assert !oopMap.containsKey(object);
        oopMap.put(object, oop);
        allTracedObjects.add(object);
        traceQueue.addLast(object);

        if (object instanceof ClassObject) {
            insertIntoClassTable((ClassObject) object);
        }
        return oop;
    }

    private long reserveLargeInteger(final long value) {
        final LargeIntegerObject largeIntegerObject = new LargeIntegerObject(image, BigInteger.valueOf(value));
        final long oop = nextChunk;
        final int numSlots = largeIntegerObject.getNumSlots();
        final int headerSlots = numSlots < 255 ? 1 : 2;
        nextChunk += (headerSlots + numSlots) * SqueakImageConstants.WORD_SIZE /* No padding */;

        additionalBoxedObjects.add(largeIntegerObject);
        return oop;
    }

    private long reserveBoxedFloat(final double value) {
        final FloatObject floatObject = new FloatObject(image, value);
        final long oop = nextChunk;
        final int numSlots = floatObject.getNumSlots();
        final int headerSlots = numSlots < 255 ? 1 : 2;
        nextChunk += (headerSlots + numSlots) * SqueakImageConstants.WORD_SIZE /* No padding */;

        additionalBoxedObjects.add(floatObject);
        return oop;
    }

    public void writeBytes(final byte[] bytes) {
        try {
            stream.write(bytes);
        } catch (final IOException e) {
            throw SqueakAbortException.create("Failed to write bytes:", e.getMessage());
        }
        position += bytes.length;
    }

    public void writeShort(final short value) {
        UnsafeUtils.putShort(byteArrayBuffer, 0, value);
        position += writeBytesFromBuffer(Short.BYTES);
    }

    public void writeInt(final int value) {
        UnsafeUtils.putInt(byteArrayBuffer, 0, value);
        position += writeBytesFromBuffer(Integer.BYTES);
    }

    public void writeLong(final long value) {
        UnsafeUtils.putLong(byteArrayBuffer, 0, value);
        position += writeBytesFromBuffer(Long.BYTES);
    }

    private int writeBytesFromBuffer(final int numberOfBytes) {
        try {
            stream.write(byteArrayBuffer, 0, numberOfBytes);
        } catch (final IOException e) {
            throw SqueakAbortException.create("Failed to write bytes:", e.getMessage());
        }
        return numberOfBytes;
    }

    public void writePadding(final int byteLength) {
        try {
            for (int i = 0; i < byteLength; i++) {
                stream.write(0);
            }
        } catch (final IOException e) {
            throw SqueakAbortException.create("Failed to write padding bytes:", e);
        }
        position += byteLength;
    }

    public void writeObjectHeader(final long numSlots, final long identityHash, final ClassObject classObject, final long formatOffset) {
        writeObjectHeader(numSlots, identityHash, classObject.getInstanceSpecification(), formatOffset, classObject.getSqueakHash());
    }

    public void writeObjectHeader(final long numSlots, final long identityHash, final long format, final long formatOffset, final long classIndex) {
        assert (format & formatOffset) == 0 : "Invalid formatOffset";
        // TODO: turn into one operation?
        writeLong(SqueakImageConstants.ObjectHeader.getHeader(numSlots, identityHash, format | formatOffset, classIndex));
    }

    public void writeNil() {
        writeLong(nilOop);
    }

    public void writeFalse() {
        writeLong(falseOop);
    }

    public void writeTrue() {
        writeLong(trueOop);
    }

    public void writeChar(final char value) {
        writeLong(toTaggedCharacter(value));
    }

    public void writeSmallInteger(final long value) {
        writeLong(toTaggedSmallInteger(value));
    }

    public void writeSmallFloat(final double value) {
        writeLong(toTaggedSmallFloat(value));
    }

    public void writeObject(final Object object) {
        writeLong(toWord(object));
    }

    public void writeObjectIfTracedElseNil(final Object object) {
        writeLong(toWord(oopMap.containsKey(object) ? object : NilObject.SINGLETON));
    }

    private static long toTaggedCharacter(final long value) {
        return value << SqueakImageConstants.NUM_TAG_BITS | SqueakImageConstants.CHARACTER_TAG;
    }

    private long toTaggedSmallInteger(final long value) {
        if (SqueakImageConstants.SMALL_INTEGER_MIN_VAL <= value && value <= SqueakImageConstants.SMALL_INTEGER_MAX_VAL) {
            if (value < 0) {
                return 0x8000000000000000L + value << SqueakImageConstants.NUM_TAG_BITS | SqueakImageConstants.SMALL_INTEGER_TAG;
            } else {
                return value << SqueakImageConstants.NUM_TAG_BITS | SqueakImageConstants.SMALL_INTEGER_TAG;
            }
        } else {
            /* value is too big for SmallInteger, so allocate a LargeIntegerObject for it. */
            return reserveLargeInteger(value);
        }
    }

    private long toTaggedSmallFloat(final double value) {
        assert Double.isFinite(value) : "Unboxed values must be finite";
        if (Math.getExponent(value) >> SqueakImageConstants.SMALL_FLOAT_TAGGED_EXPONENT_SIZE == 0) {
            /* See Spur64BitMemoryManager>>smallFloatObjectOf:. */
            final long bits = Double.doubleToRawLongBits(value);
            long valueWithoutTag = Long.rotateLeft(bits, 1);
            if (value != 0) {
                valueWithoutTag -= SqueakImageConstants.SMALL_FLOAT_TAG_BITS_MASK;
            }
            final long valueWithTag = valueWithoutTag << SqueakImageConstants.NUM_TAG_BITS | SqueakImageConstants.SMALL_FLOAT_TAG;
            if (valueWithTag >>> SqueakImageConstants.NUM_TAG_BITS != valueWithoutTag) {
                return reserveBoxedFloat(value); /* Overflow, fall back to boxed float. */
            } else {
                return valueWithTag;
            }
        } else {
            return reserveBoxedFloat(value);
        }
    }

    private void closeStream() {
        try {
            stream.close();
        } catch (final IOException e) {
            throw SqueakAbortException.create("Failed to close file:", e.getMessage());
        }
    }
}
