package de.hpi.swa.graal.squeak.model;

import java.util.Arrays;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.MiscellaneousPrimitives.PrimShallowCopyNode;

public final class ArrayObject extends AbstractSqueakObject {
    public static final byte BOOLEAN_NIL_TAG = 0;
    public static final byte BOOLEAN_TRUE_TAG = 1;
    public static final byte BOOLEAN_FALSE_TAG = -1;
    public static final char CHAR_NIL_TAG = Character.MAX_VALUE;
    public static final long LONG_NIL_TAG = Long.MIN_VALUE;
    public static final double DOUBLE_NIL_TAG = Double.longBitsToDouble(0x7ff8000000000001L);
    public static final long DOUBLE_NIL_TAG_LONG = Double.doubleToRawLongBits(DOUBLE_NIL_TAG);
    public static final boolean ENABLE_STORAGE_STRATEGIES = true;

    public static ArrayObject createEmptyStrategy(final SqueakImageContext image, final ClassObject classObject, final int size) {
        return new ArrayObject(image, classObject, size);
    }

    public static ArrayObject createObjectStrategy(final SqueakImageContext image, final ClassObject classObject, final int size) {
        return new ArrayObject(image, classObject, new Object[size]);
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

    public Object at0Boolean(final long index) {
        final byte value = getBooleanStorage()[(int) index];
        if (value == BOOLEAN_FALSE_TAG) {
            return image.sqFalse;
        } else if (value == BOOLEAN_TRUE_TAG) {
            return image.sqTrue;
        } else {
            assert value == BOOLEAN_NIL_TAG;
            return image.nil;
        }
    }

    public Object at0Char(final long index) {
        final char value = getCharStorage()[(int) index];
        if (value == CHAR_NIL_TAG) {
            return image.nil;
        } else {
            return value;
        }
    }

    public Object at0Double(final long index) {
        final double value = getDoubleStorage()[(int) index];
        if (Double.doubleToRawLongBits(value) == DOUBLE_NIL_TAG_LONG) {
            return image.nil;
        } else {
            return value;
        }
    }

    public Object at0Long(final long index) {
        final long value = getLongStorage()[(int) index];
        if (value == LONG_NIL_TAG) {
            return image.nil;
        } else {
            return value;
        }
    }

    public Object at0Object(final int index) {
        final Object value = getObjectStorage()[index];
        if (value == null) {
            return image.nil;
        } else {
            return value;
        }
    }

    public Object at0Object(final long index) {
        return at0Object((int) index);
    }

    public AbstractSqueakObject at0SqueakObject(final long index) {
        final AbstractSqueakObject value = getAbstractSqueakObjectStorage()[(int) index];
        if (value == null) {
            return image.nil;
        } else {
            return value;
        }
    }

    public void atput0Boolean(final long index, final boolean value) {
        getBooleanStorage()[(int) index] = value ? BOOLEAN_TRUE_TAG : BOOLEAN_FALSE_TAG;
    }

    public void atput0Char(final long index, final char value) {
        getCharStorage()[(int) index] = value;
    }

    public void atput0Double(final long index, final double value) {
        getDoubleStorage()[(int) index] = value;
    }

    public void atput0Long(final long index, final long value) {
        getLongStorage()[(int) index] = value;
    }

    public Object atput0Object(final int index, final Object value) {
        return getObjectStorage()[index] = value;
    }

    public void atput0Object(final long index, final Object value) {
        atput0Object((int) index, value);
    }

    public void atput0SqueakObject(final long index, final AbstractSqueakObject value) {
        getAbstractSqueakObjectStorage()[(int) index] = value;
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

    public void become(final ArrayObject other) {
        becomeOtherClass(other);
        final Object otherStorage = other.storage;
        other.setStorage(this.storage);
        setStorage(otherStorage);
    }

    public int getAbstractSqueakObjectLength() {
        return getAbstractSqueakObjectStorage().length;
    }

    public AbstractSqueakObject[] getAbstractSqueakObjectStorage() {
        assert isAbstractSqueakObjectType();
        return (AbstractSqueakObject[]) storage;
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

    public NilObject getNil() {
        return image.nil;
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

    public int instsize() {
        return getSqueakClass().getBasicInstanceSize();
    }

    public boolean isAbstractSqueakObjectType() {
        return storage.getClass() == AbstractSqueakObject[].class;
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

    public boolean isObjectType() {
        return storage.getClass() == Object[].class;
    }

    public void setStorage(final Object newStorage) {
        storage = newStorage;
    }

    public void setStorageAndSpecializeIfPossible(final Object[] values) {
        if (!ENABLE_STORAGE_STRATEGIES) {
            storage = values;
            return;
        }
        final int valuesLength = values.length;
        if (valuesLength > 0) {
            final Object firstElement = values[0];
            Class<? extends Object> specializedClass = firstElement.getClass();
            if (firstElement == image.nil || firstElement instanceof AbstractSqueakObject || firstElement instanceof Boolean || firstElement instanceof Character || firstElement instanceof Long ||
                            firstElement instanceof Double) {
                for (int i = 1; i < valuesLength; i++) {
                    final Object value = values[i];
                    if (value != null && value != image.nil && specializedClass != value.getClass()) {
                        specializedClass = Object.class;
                        break;
                    }
                }
                if (specializedClass == NilObject.class) {
                    storage = valuesLength;
                } else if (specializedClass == AbstractSqueakObject.class) {
                    final AbstractSqueakObject[] squeakObjects = new AbstractSqueakObject[valuesLength];
                    for (int i = 0; i < valuesLength; i++) {
                        squeakObjects[i] = (AbstractSqueakObject) values[i];
                    }
                    storage = squeakObjects;
                } else if (specializedClass == Boolean.class) {
                    final byte[] booleans = new byte[valuesLength];
                    for (int i = 0; i < valuesLength; i++) {
                        final Object value = values[i];
                        if (value == null || value == image.nil) {
                            booleans[i] = BOOLEAN_NIL_TAG;
                        } else {
                            booleans[i] = (boolean) value ? BOOLEAN_TRUE_TAG : BOOLEAN_FALSE_TAG;
                        }
                    }
                    storage = booleans;
                } else if (specializedClass == Character.class) {
                    final char[] chars = new char[valuesLength];
                    for (int i = 0; i < valuesLength; i++) {
                        final Object value = values[i];
                        if (value == null || value == image.nil) {
                            chars[i] = CHAR_NIL_TAG;
                        } else {
                            chars[i] = (char) value;
                        }
                    }
                    storage = chars;
                } else if (specializedClass == Long.class) {
                    final long[] longs = new long[valuesLength];
                    for (int i = 0; i < valuesLength; i++) {
                        final Object value = values[i];
                        if (value == null || value == image.nil) {
                            longs[i] = LONG_NIL_TAG;
                        } else {
                            longs[i] = (long) value;
                        }
                    }
                    storage = longs;
                } else if (specializedClass == Double.class) {
                    final double[] doubles = new double[valuesLength];
                    for (int i = 0; i < valuesLength; i++) {
                        final Object value = values[i];
                        if (value == null || value == image.nil) {
                            doubles[i] = DOUBLE_NIL_TAG;
                        } else {
                            doubles[i] = (double) value;
                        }
                    }
                    storage = doubles;
                } else {
                    storage = values; // cannot be specialized
                }
            } else {
                storage = values; // cannot be specialized
            }
        } else {
            storage = 0;
        }
    }

    /** Slow, {@link PrimShallowCopyNode} is more efficient. */
    public ArrayObject shallowCopy() {
        if (isAbstractSqueakObjectType()) {
            return new ArrayObject(image, getSqueakClass(), getAbstractSqueakObjectStorage().clone());
        } else if (isBooleanType()) {
            return new ArrayObject(image, getSqueakClass(), getBooleanStorage().clone());
        } else if (isCharType()) {
            return new ArrayObject(image, getSqueakClass(), getCharStorage().clone());
        } else if (isDoubleType()) {
            return new ArrayObject(image, getSqueakClass(), getDoubleStorage().clone());
        } else if (isEmptyType()) {
            return new ArrayObject(image, getSqueakClass(), getEmptyStorage());
        } else if (isLongType()) {
            return new ArrayObject(image, getSqueakClass(), getLongStorage().clone());
        } else {
            assert isObjectType();
            return new ArrayObject(image, getSqueakClass(), getObjectStorage().clone());
        }
    }

    public void transitionFromAbstractSqueakObjectsToObjects() {
        final AbstractSqueakObject[] abstractSqueakObjects = getAbstractSqueakObjectStorage();
        final Object[] objects = new Object[abstractSqueakObjects.length];
        for (int i = 0; i < abstractSqueakObjects.length; i++) {
            objects[i] = abstractSqueakObjects[i];
        }
        storage = objects;
    }

    public void transitionFromBooleansToObjects() {
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
                objects[i] = image.nil;
            }
        }
        storage = objects;
    }

    public void transitionFromCharsToObjects() {
        final char[] chars = getCharStorage();
        final Object[] objects = new Object[chars.length];
        for (int i = 0; i < chars.length; i++) {
            objects[i] = chars[i] == CHAR_NIL_TAG ? null : chars[i];
        }
        storage = objects;
    }

    public void transitionFromDoublesToObjects() {
        final double[] doubles = getDoubleStorage();
        final Object[] objects = new Object[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            objects[i] = doubles[i] == DOUBLE_NIL_TAG ? null : doubles[i];
        }
        storage = objects;
    }

    public void transitionFromEmptyToAbstractSqueakObjects() {
        final AbstractSqueakObject[] squeakObjects = new AbstractSqueakObject[getEmptyStorage()];
        storage = squeakObjects;
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

    public void transitionFromEmptyToObjects() {
        final Object[] objects = new Object[getEmptyStorage()];
        storage = objects;
    }

    public void transitionFromLongsToObjects() {
        final long[] longs = getLongStorage();
        final Object[] objects = new Object[longs.length];
        for (int i = 0; i < longs.length; i++) {
            objects[i] = longs[i] == LONG_NIL_TAG ? null : longs[i];
        }
        storage = objects;
    }
}
