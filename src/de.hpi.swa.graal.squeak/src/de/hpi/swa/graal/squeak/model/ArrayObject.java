package de.hpi.swa.graal.squeak.model;

import java.util.Arrays;

import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class ArrayObject extends AbstractSqueakObject {
    private static final long LONG_NIL_TAG = Long.MIN_VALUE;
    private static final double DOUBLE_NIL_TAG = Double.MIN_VALUE;

    private final BranchProfile longNilTagStore = BranchProfile.create();
    private final BranchProfile doubleNilTagStore = BranchProfile.create();

    private Object storage;

    public ArrayObject(final SqueakImageContext img) {
        super(img, -1, null); // for special ArrayObjects only
    }

    public ArrayObject(final SqueakImageContext image, final ClassObject classObject, final Object storage) {
        super(image, classObject);
        this.storage = storage;
    }

    public ArrayObject(final SqueakImageContext img, final long hash, final ClassObject klass) {
        super(img, hash, klass);
    }

    public Object at0Double(final long index) {
        final double value = getDoubleStorage()[(int) index];
        if (value == DOUBLE_NIL_TAG) {
            return image.nil;
        }
        return value;
    }

    public Object at0Long(final long index) {
        final long value = getLongStorage()[(int) index];
        if (value == LONG_NIL_TAG) {
            return image.nil;
        }
        return value;
    }

    public Object at0Object(final int index) {
        final Object value = getObjectStorage()[index];
        if (value == null) {
            return image.nil;
        }
        return value;
    }

    public Object at0Object(final long index) {
        return at0Object((int) index);
    }

    public AbstractSqueakObject at0SqueakObject(final long index) {
        final AbstractSqueakObject value = getAbstractSqueakObjectStorage()[(int) index];
        if (value == null) {
            return image.nil;
        }
        return value;
    }

    public void atput0Double(final long index, final double value) {
        if (value == DOUBLE_NIL_TAG) {
            doubleNilTagStore.enter();
            transitionFromDoublesToObjects();
            atput0Object(index, value);
        } else {
            getDoubleStorage()[(int) index] = value;
        }
    }

    public void atput0Long(final long index, final long value) {
        if (value == LONG_NIL_TAG) {
            longNilTagStore.enter();
            transitionFromLongsToObjects();
            atput0Object(index, value);
        } else {
            getLongStorage()[(int) index] = value;
        }
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

    public AbstractSqueakObject[] getAbstractSqueakObjectStorage() {
        assert isAbstractSqueakObjectType();
        return (AbstractSqueakObject[]) storage;
    }

    public double[] getDoubleStorage() {
        assert isDoubleType();
        return (double[]) storage;
    }

    public int getEmptyStorage() {
        assert isEmptyType();
        return (int) storage;
    }

    public long[] getLongStorage() {
        assert isLongType();
        return (long[]) storage;
    }

    public NilObject getNil() {
        return image.nil;
    }

    public Object[] getObjectStorage() {
        assert isObjectType();
        return (Object[]) storage;
    }

    public Class<? extends Object> getStorageType() {
        return storage.getClass();
    }

    public int instsize() {
        return getSqClass().getBasicInstanceSize();
    }

    public boolean isAbstractSqueakObjectType() {
        return storage.getClass() == AbstractSqueakObject[].class;
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
        final int valuesLength = values.length;
        if (valuesLength > 0) {
            final Object firstElement = values[0];
            Class<? extends Object> specializedClass = firstElement.getClass();
            if (firstElement instanceof AbstractSqueakObject || firstElement instanceof Long || firstElement instanceof Double) {
                for (int i = 1; i < valuesLength; i++) {
                    if (specializedClass != values[i].getClass()) {
                        specializedClass = Object.class;
                        break;
                    }
                }
                if (specializedClass == AbstractSqueakObject.class) {
                    final AbstractSqueakObject[] squeakObjects = new AbstractSqueakObject[valuesLength];
                    for (int i = 0; i < valuesLength; i++) {
                        squeakObjects[i] = (AbstractSqueakObject) values[i];
                    }
                    storage = squeakObjects;
                } else if (specializedClass == Long.class) {
                    final long[] longs = new long[valuesLength];
                    for (int i = 0; i < valuesLength; i++) {
                        longs[i] = (long) values[i];
                    }
                    storage = longs;
                } else if (specializedClass == Double.class) {
                    final double[] doubles = new double[valuesLength];
                    for (int i = 0; i < valuesLength; i++) {
                        doubles[i] = (double) values[i];
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

    public void transitionFromAbstractSqueakObjectsToObjects() {
        final AbstractSqueakObject[] abstractSqueakObjects = getAbstractSqueakObjectStorage();
        final Object[] objects = new Object[abstractSqueakObjects.length];
        for (int i = 0; i < abstractSqueakObjects.length; i++) {
            objects[i] = abstractSqueakObjects[i];
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
