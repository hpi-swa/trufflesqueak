package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions;
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class NativeObject extends SqueakObject {
    @CompilationFinal protected AbstractNativeObjectStorage storage;

    public static NativeObject newNativeBytes(SqueakImageContext img, ClassObject klass, int size) {
        return new NativeObject(img, klass, new NativeBytesStorage(size));
    }

    public static NativeObject newNativeBytes(SqueakImageContext img, ClassObject klass, byte[] bytes) {
        return new NativeObject(img, klass, new NativeBytesStorage(bytes));
    }

    public static NativeObject newNativeShorts(SqueakImageContext img, ClassObject klass, int size) {
        return new NativeObject(img, klass, new NativeShortsStorage(size));
    }

    public static NativeObject newNativeShorts(SqueakImageContext img, ClassObject klass, short[] shorts) {
        return new NativeObject(img, klass, new NativeShortsStorage(shorts));
    }

    public static NativeObject newNativeWords(SqueakImageContext img, ClassObject klass, int size) {
        return new NativeObject(img, klass, new NativeWordsStorage(size));
    }

    public static NativeObject newNativeWords(SqueakImageContext img, ClassObject klass, int[] words) {
        return new NativeObject(img, klass, new NativeWordsStorage(words));
    }

    public static NativeObject newNativeLongs(SqueakImageContext img, ClassObject klass, int size) {
        return new NativeObject(img, klass, new NativeLongsStorage(size));
    }

    public static NativeObject newNativeLongs(SqueakImageContext img, ClassObject klass, long[] longs) {
        return new NativeObject(img, klass, new NativeLongsStorage(longs));
    }

    public NativeObject(SqueakImageContext img) {
        super(img);
    }

    public NativeObject(SqueakImageContext img, AbstractNativeObjectStorage storage) {
        super(img);
        this.storage = storage;
    }

    public NativeObject(SqueakImageContext image, ClassObject classObject) {
        super(image, classObject);
    }

    protected NativeObject(SqueakImageContext image, ClassObject classObject, AbstractNativeObjectStorage storage) {
        this(image, classObject);
        this.storage = storage;
    }

    protected NativeObject(NativeObject original) {
        this(original.image, original.getSqClass(), original.storage.shallowCopy());
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        super.fillin(chunk);
        storage.fillin(chunk);
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (!(other instanceof NativeObject)) {
            throw new PrimitiveExceptions.PrimitiveFailed();
        }
        if (!super.become(other)) {
            throw new SqueakException("Should not fail");
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        NativeObject otherNativeObject = (NativeObject) other;
        AbstractNativeObjectStorage otherStorage = otherNativeObject.storage;
        otherNativeObject.storage = this.storage;
        this.storage = otherStorage;
        return true;
    }

    @TruffleBoundary
    @Override
    public String toString() {
        return new String(getBytes());
    }

    @Override
    public Object at0(long index) {
        return getNativeAt0(index);
    }

    @Override
    public void atput0(long index, Object object) {
        if (object instanceof LargeIntegerObject) {
            long longValue;
            try {
                longValue = ((LargeIntegerObject) object).reduceToLong();
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException(e.toString());
            }
            storage.setNativeAt0(index, longValue);
        } else {
            storage.setNativeAt0(index, (long) object);
        }
    }

    public long getNativeAt0(long index) {
        return storage.getNativeAt0(index);
    }

    public void setNativeAt0(long index, long value) {
        storage.setNativeAt0(index, value);
    }

    public long shortAt0(long longIndex) {
        return storage.shortAt0(longIndex);
    }

    public void shortAtPut0(long longIndex, long value) {
        storage.shortAtPut0(longIndex, value);
    }

    @Override
    public final int instsize() {
        return 0;
    }

    public byte[] getBytes() {
        return storage.getBytes();
    }

    public int[] getWords() {
        return storage.getWords();
    }

    public void fillWith(Object value) {
        storage.fillWith(value);
    }

    @Override
    public int size() {
        return storage.size();
    }

    public byte getElementSize() {
        return storage.getElementSize();
    }

    public LargeIntegerObject normalize() {
        return new LargeIntegerObject(image, getSqClass(), getBytes());
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new NativeObject(this);
    }

    public void setByte(int index, byte value) {
        storage.setByte(index, value);
    }

    public int getInt(int index) {
        return storage.getInt(index);
    }

    public void setInt(int index, int value) {
        storage.setInt(index, value);
    }

    public void convertStorage(NativeObject argument) {
        if (getElementSize() == argument.getElementSize()) {
            return; // no need to covert storage
        }
        byte[] oldBytes = getBytes();
        CompilerDirectives.transferToInterpreterAndInvalidate();
        switch (argument.getElementSize()) {
            case 1:
                storage = new NativeBytesStorage(oldBytes);
                return;
            case 2:
                storage = new NativeShortsStorage(0);
                break;
            case 4:
                storage = new NativeWordsStorage(0);
                break;
            case 8:
                storage = new NativeLongsStorage(0);
                break;
            default:
                throw new SqueakException("Should not happen");
        }
        storage.setBytes(oldBytes);
    }
}
