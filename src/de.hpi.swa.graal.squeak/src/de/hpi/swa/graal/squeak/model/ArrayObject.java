package de.hpi.swa.graal.squeak.model;

import java.util.Arrays;

import com.oracle.truffle.api.TruffleLogger;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.reading.SqueakImageChunk;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectWriteNode;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public final class ArrayObject extends AbstractSqueakObjectWithClassAndHash {
    public static final byte BOOLEAN_NIL_TAG = 0;
    public static final byte BOOLEAN_TRUE_TAG = 1;
    public static final byte BOOLEAN_FALSE_TAG = -1;
    public static final char CHAR_NIL_TAG = Character.MAX_VALUE - 1; // Rather unlikely char.
    public static final long LONG_NIL_TAG = Long.MIN_VALUE + 42; // Rather unlikely long.
    public static final double DOUBLE_NIL_TAG = Double.longBitsToDouble(0x7ff8000000000001L); // NaN+1.
    public static final long DOUBLE_NIL_TAG_LONG = Double.doubleToRawLongBits(DOUBLE_NIL_TAG);
    public static final boolean ENABLE_STORAGE_STRATEGIES = true;
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, ArrayObject.class);

    private Object storage;

    public ArrayObject(final SqueakImageContext image) {
        super(image); // for special ArrayObjects only
    }

    private ArrayObject(final SqueakImageContext image, final ClassObject classObject, final Object storage) {
        super(image, classObject);
        this.storage = storage;
    }

    public ArrayObject(final SqueakImageContext img, final long hash, final ClassObject klass) {
        super(img, hash, klass);
    }

    public static ArrayObject createEmptyStrategy(final SqueakImageContext image, final ClassObject classObject, final int size) {
        return new ArrayObject(image, classObject, size);
    }

    public static ArrayObject createObjectStrategy(final SqueakImageContext image, final ClassObject classObject, final int size) {
        final Object[] objects = new Object[size];
        Arrays.fill(objects, NilObject.SINGLETON);
        return new ArrayObject(image, classObject, objects);
    }

    public static ArrayObject createWithStorage(final SqueakImageContext image, final ClassObject classObject, final Object storage) {
        return new ArrayObject(image, classObject, storage);
    }

    public static boolean isDoubleNilTag(final double value) {
        return Double.doubleToRawLongBits(value) == DOUBLE_NIL_TAG_LONG;
    }

    public static boolean isLongNilTag(final long value) {
        return value == LONG_NIL_TAG;
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        final Object[] pointers = chunk.getPointers();
        if (!ENABLE_STORAGE_STRATEGIES) {
            storage = pointers;
            return;
        }
        final int valuesLength = pointers.length;
        storage = valuesLength;
        final ArrayObjectWriteNode writeNode = ArrayObjectWriteNode.getUncached();
        if (valuesLength > 0) {
            storage = valuesLength;
            for (int i = 0; i < pointers.length; i++) {
                writeNode.execute(this, i, pointers[i]);
            }
        }
    }

    public Object at0Boolean(final long index) {
        final byte value = getBooleanStorage()[(int) index];
        if (value == BOOLEAN_FALSE_TAG) {
            return image.sqFalse;
        } else if (value == BOOLEAN_TRUE_TAG) {
            return image.sqTrue;
        } else {
            assert value == BOOLEAN_NIL_TAG;
            return NilObject.SINGLETON;
        }
    }

    public Object at0Char(final long index) {
        final char value = getCharStorage()[(int) index];
        return value == CHAR_NIL_TAG ? NilObject.SINGLETON : value;
    }

    public Object at0Double(final long index) {
        final double value = getDoubleStorage()[(int) index];
        return Double.doubleToRawLongBits(value) == DOUBLE_NIL_TAG_LONG ? NilObject.SINGLETON : value;
    }

    public Object at0Long(final long index) {
        final long value = getLongStorage()[(int) index];
        return value == LONG_NIL_TAG ? NilObject.SINGLETON : value;
    }

    public AbstractSqueakObject at0NativeObject(final long index) {
        return NilObject.nullToNil(getNativeObjectStorage()[(int) index]);
    }

    public Object at0Object(final int index) {
        final Object value = getObjectStorage()[index];
        assert value != null;
        return value;
    }

    public Object at0Object(final long index) {
        return at0Object((int) index);
    }

    public void atput0Boolean(final long index, final boolean value) {
        getBooleanStorage()[(int) index] = value ? BOOLEAN_TRUE_TAG : BOOLEAN_FALSE_TAG;
    }

    public void atput0Char(final long index, final char value) {
        if (value == CHAR_NIL_TAG) {
            transitionFromCharsToObjects();
            atput0Object(index, value);
        } else {
            getCharStorage()[(int) index] = value;
        }
    }

    public void atput0Double(final long index, final double value) {
        if (value == DOUBLE_NIL_TAG) {
            transitionFromDoublesToObjects();
            atput0Object(index, value);
        } else {
            getDoubleStorage()[(int) index] = value;
        }
    }

    public void atput0Long(final long index, final long value) {
        if (value == LONG_NIL_TAG) {
            transitionFromLongsToObjects();
            atput0Object(index, value);
        } else {
            getLongStorage()[(int) index] = value;
        }
    }

    public void atput0NativeObject(final long index, final NativeObject value) {
        getNativeObjectStorage()[(int) index] = value;
    }

    public void atput0Object(final int index, final Object value) {
        getObjectStorage()[index] = value;
    }

    public void atput0Object(final long index, final Object value) {
        atput0Object((int) index, value);
    }

    public void atputNil0Boolean(final long index) {
        getBooleanStorage()[(int) index] = BOOLEAN_NIL_TAG;
    }

    public void atputNil0Char(final long index) {
        getCharStorage()[(int) index] = CHAR_NIL_TAG;
    }

    public void atputNil0Double(final long index) {
        getDoubleStorage()[(int) index] = DOUBLE_NIL_TAG;
    }

    public void atputNil0Long(final long index) {
        getLongStorage()[(int) index] = LONG_NIL_TAG;
    }

    public void atputNil0NativeObject(final long index) {
        getNativeObjectStorage()[(int) index] = null;
    }

    public void become(final ArrayObject other) {
        becomeOtherClass(other);
        final Object otherStorage = other.storage;
        other.setStorage(storage);
        setStorage(otherStorage);
    }

    public int getBooleanLength() {
        return getBooleanStorage().length;
    }

    public byte[] getBooleanStorage() {
        assert isBooleanType();
        return (byte[]) storage;
    }

    public int getCharLength() {
        return getCharStorage().length;
    }

    public char[] getCharStorage() {
        assert isCharType();
        return (char[]) storage;
    }

    public int getDoubleLength() {
        return getDoubleStorage().length;
    }

    public double[] getDoubleStorage() {
        assert isDoubleType();
        return (double[]) storage;
    }

    public int getEmptyLength() {
        return getEmptyStorage();
    }

    public int getEmptyStorage() {
        assert isEmptyType();
        return (int) storage;
    }

    public int getLongLength() {
        return getLongStorage().length;
    }

    public long[] getLongStorage() {
        assert isLongType();
        return (long[]) storage;
    }

    public int getNativeObjectLength() {
        return getNativeObjectStorage().length;
    }

    public NativeObject[] getNativeObjectStorage() {
        assert isNativeObjectType();
        return (NativeObject[]) storage;
    }

    public int getObjectLength() {
        return getObjectStorage().length;
    }

    public Object[] getObjectStorage() {
        assert isObjectType();
        return (Object[]) storage;
    }

    public Class<? extends Object> getStorageType() {
        return storage.getClass();
    }

    @Override
    public int instsize() {
        return getSqueakClass().getBasicInstanceSize();
    }

    @Override
    public int size() {
        throw SqueakException.create("Use ArrayObjectSizeNode");
    }

    public boolean isBooleanType() {
        return storage.getClass() == byte[].class;
    }

    public boolean isCharType() {
        return storage.getClass() == char[].class;
    }

    public boolean isDoubleType() {
        return storage.getClass() == double[].class;
    }

    public boolean isEmptyType() {
        return storage instanceof Integer;
    }

    public boolean isLongType() {
        return storage.getClass() == long[].class;
    }

    public boolean isNativeObjectType() {
        return storage.getClass() == NativeObject[].class;
    }

    public boolean isObjectType() {
        return storage.getClass() == Object[].class;
    }

    public boolean isTraceableObjectType() {
        return isObjectType();
    }

    public boolean hasSameStorageType(final ArrayObject other) {
        return storage.getClass() == other.storage.getClass();
    }

    public void setStorage(final Object newStorage) {
        storage = newStorage;
    }

    public void transitionFromBooleansToObjects() {
        LOG.finer("transition from Booleans to Objects");
        final byte[] booleans = getBooleanStorage();
        final Object[] objects = new Object[booleans.length];
        for (int i = 0; i < booleans.length; i++) {
            final byte value = booleans[i];
            if (value == BOOLEAN_FALSE_TAG) {
                objects[i] = image.sqFalse;
            } else if (value == BOOLEAN_TRUE_TAG) {
                objects[i] = image.sqTrue;
            } else {
                assert value == BOOLEAN_NIL_TAG;
                objects[i] = NilObject.SINGLETON;
            }
        }
        storage = objects;
    }

    public void transitionFromCharsToObjects() {
        LOG.finer("transition from Chars to Objects");
        final char[] chars = getCharStorage();
        final Object[] objects = new Object[chars.length];
        for (int i = 0; i < chars.length; i++) {
            objects[i] = chars[i] == CHAR_NIL_TAG ? NilObject.SINGLETON : chars[i];
        }
        storage = objects;
    }

    public void transitionFromDoublesToObjects() {
        LOG.finer("transition from Doubles to Objects");
        final double[] doubles = getDoubleStorage();
        final Object[] objects = new Object[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            objects[i] = doubles[i] == DOUBLE_NIL_TAG ? NilObject.SINGLETON : doubles[i];
        }
        storage = objects;
    }

    public void transitionFromEmptyToBooleans() {
        // Zero-initialized, no need to fill with BOOLEAN_NIL_TAG.
        storage = new byte[getEmptyStorage()];
    }

    public void transitionFromEmptyToChars() {
        final char[] chars = new char[getEmptyStorage()];
        Arrays.fill(chars, CHAR_NIL_TAG);
        storage = chars;
    }

    public void transitionFromEmptyToDoubles() {
        final double[] doubles = new double[getEmptyStorage()];
        Arrays.fill(doubles, DOUBLE_NIL_TAG);
        storage = doubles;
    }

    public void transitionFromEmptyToLongs() {
        final long[] longs = new long[getEmptyStorage()];
        Arrays.fill(longs, LONG_NIL_TAG);
        storage = longs;
    }

    public void transitionFromEmptyToNatives() {
        storage = new NativeObject[getEmptyStorage()];
    }

    public void transitionFromEmptyToObjects() {
        storage = ArrayUtils.withAll(getEmptyLength(), NilObject.SINGLETON);
    }

    public void transitionFromLongsToObjects() {
        LOG.finer("transition from Longs to Objects");
        final long[] longs = getLongStorage();
        final Object[] objects = new Object[longs.length];
        for (int i = 0; i < longs.length; i++) {
            objects[i] = longs[i] == LONG_NIL_TAG ? NilObject.SINGLETON : longs[i];
        }
        storage = objects;
    }

    public void transitionFromNativesToObjects() {
        LOG.finer("transition from NativeObjects to Objects");
        final NativeObject[] natives = getNativeObjectStorage();
        final Object[] objects = new Object[natives.length];
        for (int i = 0; i < natives.length; i++) {
            objects[i] = NilObject.nullToNil(natives[i]);
        }
        storage = objects;
    }
}
