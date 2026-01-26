/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.image;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithHash;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.LargeIntegers;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils;
import de.hpi.swa.trufflesqueak.util.TimeUtils;
import de.hpi.swa.trufflesqueak.util.VarHandleUtils;

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

    private SqueakImageWriter(final SqueakImageContext image) throws IOException {
        this.image = image;
        final TruffleFile truffleFile = image.env.getPublicTruffleFile(image.getImagePath());
        stream = new BufferedOutputStream(truffleFile.newOutputStream());
        freeList = NativeObject.newNativeLongs(image.nilClass /* ignored */, SqueakImageConstants.NUM_FREE_LISTS);
    }

    /*
     * Image writing is a rare operation and should therefore be excluded from Truffle compilation.
     */
    @TruffleBoundary
    public static void write(final SqueakImageContext image, final ContextObject thisContext) {
        try {
            new SqueakImageWriter(image).run(thisContext);
        } catch (final IOException e) {
            LogUtils.IMAGE.log(Level.WARNING, "Failed to write image", e);
        }
    }

    public SqueakImageContext getImage() {
        return image;
    }

    private void run(final ContextObject thisContext) throws IOException {
        final long start = TimeUtils.currentTimeMillis();
        nextChunk = image.flags.getOldBaseAddress();
        final PointersObject activeProcess = image.getActiveProcessSlow();
        try {
            /* Mark thisContext as suspended during tracing and writing. */
            AbstractPointersObjectWriteNode.executeUncached(activeProcess, PROCESS.SUSPENDED_CONTEXT, thisContext);
            traceObjects();
            writeImageHeader();
            writeBody();
        } finally {
            /* Unmark thisContext as suspended. */
            AbstractPointersObjectWriteNode.executeUncached(activeProcess, PROCESS.SUSPENDED_CONTEXT, NilObject.SINGLETON);
            closeStream();
            finalizeImageHeader();
        }
        final double fileSize = Math.ceil((double) position / 1024 / 1024 * 100) / 100;
        LogUtils.IMAGE.fine(() -> "Image saved in " + (TimeUtils.currentTimeMillis() - start) + "ms (" + fileSize + "MiB).");
    }

    private void writeImageHeader() {
        assert position == 0 && image.imageFormat != 0;
        /* Write basic header. */
        writeInt(image.imageFormat);
        writeInt(SqueakImageConstants.IMAGE_HEADER_SIZE); // hdr size
        assert position == SqueakImageConstants.IMAGE_HEADER_MEMORY_SIZE_POSITION;
        writeLong(0); // memory size (yet unknown, see finalizeFileHeader)
        writeLong(image.flags.getOldBaseAddress()); // oldBaseAddress
        writeLong(specialObjectOop);
        writeLong(0xffee); // last hash
        writeLong(image.flags.getSnapshotScreenSize());
        writeLong(image.flags.getHeaderFlags());
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
        assert image.validClassTableRootPages();
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
        if (object instanceof final AbstractSqueakObjectWithHash o && !oopMap.containsKey(o)) {
            reserve(o);
        }
    }

    public void traceAllIfNecessary(final Object[] objects) {
        for (final Object object : objects) {
            traceIfNecessary(object);
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
            assert currentObject.assertNotForwarded();
            currentObject.write(this);
            previousObject = currentObject;
        }
        assert currentOop() == nextChunkAfterTracing;
        /* Write additional large integers and boxed floats. */
        for (final AbstractSqueakObjectWithHash value : additionalBoxedObjects) {
            assert value.assertNotForwarded();
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
     * Memory size and first fragment size (the same value in TruffleSqueak's case) are unknown when
     * the image header is written. This updates both values in the header accordingly.
     */
    private void finalizeImageHeader() throws IOException {
        final TruffleFile truffleFile = image.env.getPublicTruffleFile(image.getImagePath());
        assert truffleFile.exists();
        final EnumSet<StandardOpenOption> options = EnumSet.of(StandardOpenOption.WRITE, StandardOpenOption.READ);
        try (SeekableByteChannel channel = truffleFile.newByteChannel(options)) {
            VarHandleUtils.putLong(byteArrayBuffer, 0, position - SqueakImageConstants.IMAGE_HEADER_SIZE);
            channel.position(SqueakImageConstants.IMAGE_HEADER_MEMORY_SIZE_POSITION);
            channel.write(ByteBuffer.wrap(byteArrayBuffer));
            channel.position(SqueakImageConstants.IMAGE_HEADER_FIRST_FRAGMENT_SIZE_POSITION);
            channel.write(ByteBuffer.wrap(byteArrayBuffer));
        }
    }

    private long currentOop() {
        return image.flags.getOldBaseAddress() + position - SqueakImageConstants.IMAGE_HEADER_SIZE;
    }

    public long toWord(final Object object) {
        assert object != null;
        if (object == NilObject.SINGLETON) {
            return nilOop;
        } else if (object instanceof final Boolean b) {
            return b ? trueOop : falseOop;
        } else if (object instanceof final Character c) {
            return toTaggedCharacter(c);
        } else if (object instanceof final CharacterObject o) {
            return toTaggedCharacter(o.getValue());
        } else if (object instanceof final Long l) {
            return toTaggedSmallInteger(l);
        } else if (object instanceof final Double d) {
            return toTaggedSmallFloat(d);
        } else if (object instanceof final AbstractSqueakObjectWithHash aso) {
            final Long oop = oopMap.get(aso);
            if (oop != null) {
                return oop;
            } else {
                LogUtils.IMAGE.warning(() -> "Unreserved object detected: " + aso + ". Replacing with nil.");
                return nilOop;
            }
        } else {
            /* Nil out any foreign objects. */
            assert !(object instanceof AbstractSqueakObject);
            return nilOop;
        }
    }

    private long reserve(final AbstractSqueakObjectWithHash object) {
        assert object.assertNotForwarded();
        final int numSlots = object.getNumSlots();
        final int headerSlots = numSlots < SqueakImageConstants.OVERFLOW_SLOTS ? 1 : 2;
        final int offset = (headerSlots - 1) * SqueakImageConstants.WORD_SIZE;
        final long oop = nextChunk + offset;
        nextChunk += (headerSlots + Math.max(numSlots, 1 /* at least an alignment word */)) * SqueakImageConstants.WORD_SIZE;

        assert !oopMap.containsKey(object);
        oopMap.put(object, oop);
        allTracedObjects.add(object);
        traceQueue.addLast(object);

        return oop;
    }

    private long reserveLargeInteger(final long value) {
        return reserveBoxedObject(LargeIntegers.toNativeObject(image, BigInteger.valueOf(value)));
    }

    private long reserveBoxedFloat(final double value) {
        return reserveBoxedObject(new FloatObject(value));
    }

    private long reserveBoxedObject(final AbstractSqueakObjectWithHash object) {
        final long oop = nextChunk;
        final int numSlots = object.getNumSlots();
        final int headerSlots = numSlots < 255 ? 1 : 2;
        nextChunk += (headerSlots + numSlots) * SqueakImageConstants.WORD_SIZE /* No padding */;
        additionalBoxedObjects.add(object);
        return oop;
    }

    public void writeBytes(final byte[] bytes) {
        try {
            stream.write(bytes);
        } catch (final IOException e) {
            throw SqueakException.create("Failed to write bytes:", e.getMessage());
        }
        position += bytes.length;
    }

    public void writeShort(final short value) {
        VarHandleUtils.putShort(byteArrayBuffer, 0, value);
        position += writeBytesFromBuffer(Short.BYTES);
    }

    public void writeInt(final int value) {
        VarHandleUtils.putInt(byteArrayBuffer, 0, value);
        position += writeBytesFromBuffer(Integer.BYTES);
    }

    public void writeLong(final long value) {
        VarHandleUtils.putLong(byteArrayBuffer, 0, value);
        position += writeBytesFromBuffer(Long.BYTES);
    }

    private int writeBytesFromBuffer(final int numberOfBytes) {
        try {
            stream.write(byteArrayBuffer, 0, numberOfBytes);
        } catch (final IOException e) {
            throw SqueakException.create("Failed to write bytes:", e.getMessage());
        }
        return numberOfBytes;
    }

    public void writePadding(final int byteLength) {
        try {
            for (int i = 0; i < byteLength; i++) {
                stream.write(0);
            }
        } catch (final IOException e) {
            throw SqueakException.create("Failed to write padding bytes:", e);
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

    public void writeObjects(final Object[] objects) {
        for (final Object object : objects) {
            writeObject(object);
        }
    }

    public void writeObjectIfTracedElseNil(final Object object) {
        writeLong(toWord(object instanceof final AbstractSqueakObjectWithHash aso && oopMap.containsKey(aso) ? object : NilObject.SINGLETON));
    }

    private static long toTaggedCharacter(final long value) {
        return value << SqueakImageConstants.NUM_TAG_BITS | SqueakImageConstants.CHARACTER_TAG;
    }

    private long toTaggedSmallInteger(final long value) {
        if (SqueakImageConstants.SMALL_INTEGER_MIN_VAL <= value && value <= SqueakImageConstants.SMALL_INTEGER_MAX_VAL) {
            return (value < 0 ? 0x8000000000000000L : 0) + value << SqueakImageConstants.NUM_TAG_BITS | SqueakImageConstants.SMALL_INTEGER_TAG;
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
            throw SqueakException.create("Failed to close file:", e.getMessage());
        }
    }
}
