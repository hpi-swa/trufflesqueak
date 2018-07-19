package de.hpi.swa.graal.squeak.image;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;

public final class SqueakImageChunk {
    @CompilationFinal protected Object object;

    @CompilationFinal private ClassObject sqClass;
    @CompilationFinal(dimensions = 1) private Object[] pointers;

    protected final int classid;
    protected final int pos;

    public final SqueakImageContext image;
    private final SqueakImageReaderNode reader;
    protected final int format;
    private final int hash;
    @CompilationFinal(dimensions = 1) private final int[] data;

    public SqueakImageChunk(final SqueakImageReaderNode reader,
                    final SqueakImageContext image,
                    final int[] data,
                    final int format,
                    final int classid,
                    final int hash,
                    final int pos) {
        this.reader = reader;
        this.image = image;
        this.format = format;
        this.classid = classid;
        this.hash = hash;
        this.pos = pos;
        this.data = data;
    }

    public static SqueakImageChunk createDummyChunk(final Object[] pointers) {
        final SqueakImageChunk chunk = new SqueakImageChunk(null, null, new int[0], 0, 0, 0, 0);
        chunk.pointers = pointers;
        return chunk;
    }

    public int[] data() {
        return data;
    }

    public ClassObject asClassObject() {
        if (object == null) {
            assert format == 1;
            CompilerDirectives.transferToInterpreterAndInvalidate();
            object = new ClassObject(image);
        } else if (object == SqueakImageReaderNode.NIL_OBJECT_PLACEHOLDER) {
            return null;
        }
        return (ClassObject) object;
    }

    public Object asObject() {
        if (object == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (format == 0) { // no fields
                object = new EmptyObject(image);
            } else if (format == 1) { // fixed pointers
                // classes should already be instantiated at this point, check a
                // bit
                assert this.getSqClass() != image.metaclass && (this.getSqClass() == null || this.getSqClass().getSqClass() != image.metaclass);
                object = new PointersObject(image);
            } else if (format == 2) { // indexable fields
                object = new PointersObject(image);
            } else if (format == 3) { // fixed and indexable fields
                if (this.getSqClass() == image.methodContextClass) {
                    object = ContextObject.create(image);
                } else if (this.getSqClass() == image.blockClosureClass) {
                    object = new BlockClosureObject(image);
                } else {
                    object = new PointersObject(image);
                }
            } else if (format == 4) { // indexable weak fields
                object = new WeakPointersObject(image);
            } else if (format == 5) { // fixed weak fields
                object = new PointersObject(image);
            } else if (format <= 8) {
                assert false; // unused
            } else if (format == 9) { // 64-bit integers
                object = NativeObject.newNativeLongs(image, null, 0);
            } else if (format <= 11) { // 32-bit integers
                if (this.getSqClass() == image.floatClass) {
                    object = FloatObject.newFromChunkWords(image, getWords());
                } else {
                    object = NativeObject.newNativeInts(image, null, 0);
                }
            } else if (format <= 15) { // 16-bit integers
                object = NativeObject.newNativeShorts(image, null, 0);
            } else if (format <= 23) { // bytes
                if (this.getSqClass() == image.largePositiveIntegerClass || this.getSqClass() == image.largeNegativeIntegerClass) {
                    object = new LargeIntegerObject(image, null, getBytes());
                } else {
                    object = NativeObject.newNativeBytes(image, null, 0);
                }
            } else if (format <= 31) { // compiled methods
                object = new CompiledMethodObject(image);
            }
        }
        if (object == SqueakImageReaderNode.NIL_OBJECT_PLACEHOLDER) {
            return image.nil;
        } else {
            return object;
        }
    }

    public int getFormat() {
        return format;
    }

    public int getHash() {
        return hash;
    }

    public ClassObject getSqClass() {
        return sqClass;
    }

    public void setSqClass(final ClassObject baseSqueakObject) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.sqClass = baseSqueakObject;
    }

    @ExplodeLoop
    public Object[] getPointers() {
        if (pointers == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            pointers = new Object[data.length];
            for (int i = 0; i < data.length; i++) {
                pointers[i] = decodePointer(data[i]);
            }
        }
        return pointers;
    }

    public Object[] getPointers(final int end) {
        if (pointers == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            pointers = new Object[end];
            for (int i = 0; i < end; i++) {
                pointers[i] = decodePointer(data[i]);
            }
        }
        return pointers;
    }

    private Object decodePointer(final int ptr) {
        if ((ptr & 3) == 0) {
            final SqueakImageChunk chunk = reader.getChunk(ptr);
            if (chunk == null) {
                logBogusPointer(ptr);
                return image.wrap(ptr >> 1);
            } else {
                return chunk.asObject();
            }
        } else if ((ptr & 1) == 1) {
            return (long) ptr >> 1;
        } else {
            assert ((ptr & 3) == 2);
            return (char) (ptr >> 2);
        }
    }

    @TruffleBoundary
    private void logBogusPointer(final int ptr) {
        image.getError().println("Bogus pointer: " + ptr + ". Treating as smallint.");
    }

    public byte[] getBytes() {
        return getBytes(0);
    }

    public byte[] getBytes(final int start) {
        final byte[] bytes = new byte[((data.length - start) * 4) - getPadding()];
        final int[] subList = Arrays.copyOfRange(data, start, data.length);
        final ByteBuffer buf = ByteBuffer.allocate(subList.length * 4);
        buf.order(ByteOrder.nativeOrder());
        for (int i : subList) {
            buf.putInt(i);
        }
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = buf.get(i);
        }
        return bytes;
    }

    public short[] getShorts() {
        final short[] shorts = new short[(data.length * 2) - getPadding()];
        final ByteBuffer buf = ByteBuffer.allocate(data.length * 2);
        buf.order(ByteOrder.nativeOrder());
        for (int i : data) {
            buf.putInt(i);
        }
        final ShortBuffer shortBuffer = buf.asShortBuffer();
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = shortBuffer.get(i);
        }
        return shorts;
    }

    public int[] getWords() {
        return data.clone();
    }

    public long[] getLongs() {
        final long[] longs = new long[data.length];
        for (int i = 0; i < longs.length; i++) {
            longs[i] = data[i];
        }
        return longs;
    }

    public int getPadding() {
        if ((16 <= format) && (format <= 31)) {
            return format & 3;
        } else if (format == 11) {
            // 32-bit words with 1 word padding
            return 4;
        } else if ((12 <= format) && (format <= 15)) {
            // 16-bit words with 2, 4, or 6 bytes padding
            return (format & 3) * 2;
        } else {
            return 0;
        }
    }

    public byte getElementSize() {
        if ((16 <= format) && (format <= 23)) {
            return 1;
        } else {
            return 4;
        }
    }
}
