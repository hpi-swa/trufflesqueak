package de.hpi.swa.trufflesqueak.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Vector;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.LargeInteger;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.SqueakObject;

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

    public Object asFloatObject() {
        assert format == 10 || format == 11;
        ByteBuffer buf = ByteBuffer.allocate(8); // 2 * 32 bit
        buf.order(ByteOrder.nativeOrder());
        buf.put(getBytes());
        buf.rewind();
        long low = Integer.toUnsignedLong(buf.asIntBuffer().get(0));
        long high = Integer.toUnsignedLong(buf.asIntBuffer().get(1));
        return Double.longBitsToDouble(high << 32 | low);
    }

    public Object asObject() {
        if (object == null) {
            if (format == 0) {
                // no fields
                object = new EmptyObject(image);
            } else if (format == 1) {
                // fixed pointers
                // classes should already be instantiated at this point, check a
                // bit
                assert this.getSqClass() != image.metaclass && (this.getSqClass() == null || this.getSqClass().getSqClass() != image.metaclass);
                object = new PointersObject(image);
            } else if (format == 2) {
                // indexable fields
                object = new ListObject(image);
            } else if (format == 3) {
                if (this.getSqClass() == image.methodContextClass) {
                    object = ContextObject.createWriteableContextObject(image);
                } else {
                    // fixed and indexable fields
                    object = new ListObject(image);
                }
            } else if (format == 4) {
                // indexable weak fields // TODO: Weak
                object = new ListObject(image);
            } else if (format == 5) {
                // fixed weak fields // TODO: Weak
                object = new PointersObject(image);
            } else if (format <= 8) {
                assert false; // unused
            } else if (format == 9) {
                // 64-bit integers
                object = new NativeObject(image, (byte) 8);
            } else if (format <= 11) {
                // 32-bit integers
                if (this.getSqClass() == image.floatClass) {
                    object = asFloatObject();
                } else {
                    object = new NativeObject(image, (byte) 4);
                }
            } else if (format <= 15) {
                // 16-bit integers
                object = new NativeObject(image, (byte) 2);
            } else if (format <= 23) {
                // bytes
                if (this.getSqClass() == image.largePositiveIntegerClass || this.getSqClass() == image.largeNegativeIntegerClass) {
                    object = new LargeInteger(image);
                } else {
                    object = new NativeObject(image, (byte) 1);
                }
            } else if (format <= 31) {
                // compiled methods
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
            return ptr >> 1;
        } else {
            assert ((ptr & 3) == 2);
            return ptr >> 2;
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
        IntBuffer intBuf = buf.asIntBuffer();
        for (int i : subList) {
            intBuf.put(i);
        }
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = buf.get(i);
        }
        return bytes;
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
