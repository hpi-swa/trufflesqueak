package de.hpi.swa.trufflesqueak.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.Vector;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.BytesObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.LongsObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.ShortsObject;
import de.hpi.swa.trufflesqueak.model.SqueakObject;
import de.hpi.swa.trufflesqueak.model.WeakPointersObject;
import de.hpi.swa.trufflesqueak.model.WordsObject;

public class SqueakImageChunk {
    Object object;

    private ClassObject sqClass;
    private Object[] pointers;

    final int classid;
    final int pos;

    private final long size;
    private final SqueakImageReader reader;
    protected final int format;
    private final int hash;
    private final Vector<Integer> data;
    private final SqueakImageContext image;

    public SqueakImageChunk(SqueakImageReader reader,
                    SqueakImageContext image,
                    long size,
                    int format,
                    int classid,
                    int hash,
                    int pos) {
        this.reader = reader;
        this.image = image;
        this.size = size;
        this.format = format;
        this.classid = classid;
        this.hash = hash;
        this.pos = pos;
        this.data = new Vector<>();
    }

    public void append(int nextInt) {
        data.add(nextInt);
    }

    public long size() {
        return data.size();
    }

    public void removeLast() {
        data.remove(data.size() - 1);
    }

    public Vector<Integer> data() {
        return data;
    }

    public SqueakObject asClassObject() {
        if (object == null) {
            assert format == 1;
            object = new ClassObject(image);
        } else if (object == SqueakImageReader.NIL_OBJECT_PLACEHOLDER) {
            return null;
        }
        return (ClassObject) object;
    }

    public Object asObject() {
        if (object == null) {
            if (format == 0) { // no fields
                object = new EmptyObject(image);
            } else if (format == 1) { // fixed pointers
                // classes should already be instantiated at this point, check a
                // bit
                assert this.getSqClass() != image.metaclass && (this.getSqClass() == null || this.getSqClass().getSqClass() != image.metaclass);
                object = new PointersObject(image);
            } else if (format == 2) { // indexable fields
                object = new ListObject(image);
            } else if (format == 3) { // fixed and indexable fields
                if (this.getSqClass() == image.methodContextClass) {
                    object = ContextObject.create(image);
                } else if (this.getSqClass() == image.blockClosureClass) {
                    object = new BlockClosureObject(image);
                } else {
                    object = new ListObject(image);
                }
            } else if (format == 4) { // indexable weak fields
                object = new WeakPointersObject(image);
            } else if (format == 5) { // fixed weak fields
                object = new PointersObject(image);
            } else if (format <= 8) {
                assert false; // unused
            } else if (format == 9) { // 64-bit integers
                object = new LongsObject(image);
            } else if (format <= 11) { // 32-bit integers
                if (this.getSqClass() == image.floatClass) {
                    object = WordsObject.bytesAsFloatObject(getBytes());
                } else {
                    object = new WordsObject(image);
                }
            } else if (format <= 15) { // 16-bit integers
                object = new ShortsObject(image);
            } else if (format <= 23) { // bytes
                if (this.getSqClass() == image.largePositiveIntegerClass || this.getSqClass() == image.largeNegativeIntegerClass) {
                    object = new LargeIntegerObject(image);
                } else {
                    object = new BytesObject(image);
                }
            } else if (format <= 31) { // compiled methods
                object = new CompiledMethodObject(image);
            }
        }
        if (object == SqueakImageReader.NIL_OBJECT_PLACEHOLDER) {
            return image.nil;
        } else {
            return object;
        }
    }

    public long getSize() {
        return size;
    }

    public int getHash() {
        return hash;
    }

    public ClassObject getSqClass() {
        return sqClass;
    }

    public void setSqClass(ClassObject baseSqueakObject) {
        this.sqClass = baseSqueakObject;
    }

    public Object[] getPointers() {
        return getPointers(data.size());
    }

    public Object[] getPointers(int end) {
        if (pointers == null) {
            pointers = new Object[end];
            for (int i = 0; i < end; i++) {
                pointers[i] = decodePointer(data.get(i));
            }
        }
        return pointers;
    }

    private Object decodePointer(int ptr) {
        if ((ptr & 3) == 0) {
            SqueakImageChunk chunk = reader.chunktable.get(ptr);
            if (chunk == null) {
                System.err.println("Bogus pointer: " + ptr + ". Treating as smallint.");
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

    public byte[] getBytes() {
        return getBytes(0);
    }

    public byte[] getBytes(int start) {
        byte[] bytes = new byte[((data.size() - start) * 4) - getPadding()];
        List<Integer> subList = data.subList(start, data.size());
        ByteBuffer buf = ByteBuffer.allocate(subList.size() * 4);
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
        short[] shorts = new short[(data.size() * 2) - getPadding()];
        ByteBuffer buf = ByteBuffer.allocate(data.size() * 2);
        buf.order(ByteOrder.nativeOrder());
        for (int i : data) {
            buf.putInt(i);
        }
        ShortBuffer shortBuffer = buf.asShortBuffer();
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = shortBuffer.get(i);
        }
        return shorts;
    }

    public int[] getWords() {
        int[] ints = new int[data.size()];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = data.get(i);
        }
        return ints;
    }

    public long[] getLongs() {
        long[] longs = new long[data.size()];
        for (int i = 0; i < longs.length; i++) {
            longs[i] = data.get(i);
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
