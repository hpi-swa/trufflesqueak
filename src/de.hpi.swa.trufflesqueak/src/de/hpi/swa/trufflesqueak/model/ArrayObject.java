/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import org.graalvm.collections.UnmodifiableEconomicMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageConstants;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectWriteNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class ArrayObject extends AbstractSqueakObjectWithClassAndHash {
    public static final byte BOOLEAN_NIL_TAG = 0;
    public static final byte BOOLEAN_TRUE_TAG = 1;
    public static final byte BOOLEAN_FALSE_TAG = -1;
    public static final char CHAR_NIL_TAG = Character.MAX_VALUE - 1; // Rather unlikely char.
    public static final long LONG_NIL_TAG = Long.MIN_VALUE + 42; // Rather unlikely long.
    public static final double DOUBLE_NIL_TAG = Double.longBitsToDouble(0x7ff8000000000001L); // NaN+1.
    public static final long DOUBLE_NIL_TAG_LONG = Double.doubleToRawLongBits(DOUBLE_NIL_TAG);

    private Object storage;

    public ArrayObject() {
        super(); // for special ArrayObjects only
    }

    public ArrayObject(final SqueakImageChunk chunk) {
        super(chunk);
    }

    private ArrayObject(final ClassObject classObject, final Object storage) {
        super(classObject);
        this.storage = storage;
    }

    public ArrayObject(final ArrayObject original, final Object storageCopy) {
        super(original);
        storage = storageCopy;
    }

    public static ArrayObject createEmptyStrategy(final ClassObject classObject, final int size) {
        return new ArrayObject(classObject, size);
    }

    public static ArrayObject createWithStorage(final ClassObject classObject, final Object storage) {
        return new ArrayObject(classObject, storage);
    }

    public static boolean isCharNilTag(final char value) {
        return value == CHAR_NIL_TAG;
    }

    public static boolean isDoubleNilTag(final double value) {
        return Double.doubleToRawLongBits(value) == DOUBLE_NIL_TAG_LONG;
    }

    public static boolean isLongNilTag(final long value) {
        return value == LONG_NIL_TAG;
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        final int valuesLength = chunk.getWordSize();
        storage = valuesLength;
        if (valuesLength > 0) {
            // Use a fresh write node because uncached node is too generic.
            final ArrayObjectWriteNode writeNode = ArrayObjectWriteNode.create();
            for (int i = 0; i < valuesLength; i++) {
                writeNode.execute(writeNode, this, i, chunk.getPointer(i));
            }
        }
    }

    public void become(final ArrayObject other) {
        becomeOtherClass(other);
        final Object otherStorage = other.storage;
        other.setStorage(storage);
        setStorage(otherStorage);
    }

    public byte getByte(final long index) {
        assert isBooleanType();
        return UnsafeUtils.getByte((byte[]) storage, (int) index);
    }

    public void setByte(final long index, final byte value) {
        assert isBooleanType();
        UnsafeUtils.putByte((byte[]) storage, (int) index, value);
    }

    public int getBooleanLength() {
        return getBooleanStorage().length;
    }

    public byte[] getBooleanStorage() {
        assert isBooleanType();
        return (byte[]) storage;
    }

    public char getChar(final long index) {
        assert isCharType();
        return UnsafeUtils.getChar((char[]) storage, index);
    }

    public void setChar(final long index, final char value) {
        assert isCharType();
        UnsafeUtils.putChar((char[]) storage, index, value);
    }

    public int getCharLength() {
        return getCharStorage().length;
    }

    public char[] getCharStorage() {
        assert isCharType();
        return (char[]) storage;
    }

    public double getDouble(final long index) {
        assert isDoubleType();
        return UnsafeUtils.getDouble((double[]) storage, index);
    }

    public void setDouble(final long index, final double value) {
        assert isDoubleType();
        UnsafeUtils.putDouble((double[]) storage, index, value);
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

    public long getLong(final long index) {
        assert isLongType();
        return UnsafeUtils.getLong((long[]) storage, index);
    }

    public void setLong(final long index, final long value) {
        assert isLongType();
        UnsafeUtils.putLong((long[]) storage, index, value);
    }

    public int getLongLength() {
        return getLongStorage().length;
    }

    public long[] getLongStorage() {
        assert isLongType();
        return (long[]) storage;
    }

    public Object getObject(final long index) {
        assert isObjectType();
        return UnsafeUtils.getObject((Object[]) storage, index);
    }

    public void setObject(final long index, final Object value) {
        assert isObjectType();
        UnsafeUtils.putObject((Object[]) storage, index, value);
    }

    public int getObjectLength() {
        return getObjectStorage().length;
    }

    public Object[] getObjectStorage() {
        assert isObjectType();
        return (Object[]) storage;
    }

    @Override
    public int instsize() {
        return 0;
    }

    @Override
    public int size() {
        CompilerAsserts.neverPartOfCompilation();
        return ArrayObjectSizeNode.executeUncached(this);
    }

    public boolean isBooleanType() {
        return storage instanceof byte[];
    }

    public boolean isCharType() {
        return storage instanceof char[];
    }

    public boolean isDoubleType() {
        return storage instanceof double[];
    }

    public boolean isEmptyType() {
        return storage instanceof Integer;
    }

    public boolean isLongType() {
        return storage instanceof long[];
    }

    public boolean isObjectType() {
        return storage instanceof Object[];
    }

    public boolean hasSameStorageType(final ArrayObject other) {
        return storage.getClass() == other.storage.getClass();
    }

    public void setStorage(final Object newStorage) {
        storage = newStorage;
    }

    public static Object toObjectFromBoolean(final byte value, final InlinedBranchProfile isNilTagProfile, final Node node) {
        if (value == BOOLEAN_FALSE_TAG) {
            return BooleanObject.FALSE;
        } else if (value == BOOLEAN_TRUE_TAG) {
            return BooleanObject.TRUE;
        } else {
            isNilTagProfile.enter(node);
            assert value == BOOLEAN_NIL_TAG;
            return NilObject.SINGLETON;
        }
    }

    public static Object toObjectFromChar(final char value, final InlinedConditionProfile isNilTagProfile, final Node node) {
        return isNilTagProfile.profile(node, isCharNilTag(value)) ? NilObject.SINGLETON : value;
    }

    public static Object toObjectFromLong(final long value, final InlinedConditionProfile isNilTagProfile, final Node node) {
        return isNilTagProfile.profile(node, isLongNilTag(value)) ? NilObject.SINGLETON : value;
    }

    public static Object toObjectFromDouble(final double value, final InlinedConditionProfile isNilTagProfile, final Node node) {
        return isNilTagProfile.profile(node, isDoubleNilTag(value)) ? NilObject.SINGLETON : value;
    }

    public void transitionFromBooleansToObjects(final InlinedBranchProfile isNilTagProfile, final Node node) {
        LogUtils.ARRAY_STATEGIES.finer("transition from Booleans to Objects");
        final byte[] booleans = getBooleanStorage();
        final Object[] objects = new Object[booleans.length];
        for (int i = 0; i < booleans.length; i++) {
            objects[i] = toObjectFromBoolean(booleans[i], isNilTagProfile, node);
        }
        storage = objects;
    }

    public void transitionFromCharsToObjects(final InlinedConditionProfile isNilTagProfile, final Node node) {
        LogUtils.ARRAY_STATEGIES.finer("transition from Chars to Objects");
        final char[] chars = getCharStorage();
        final Object[] objects = new Object[chars.length];
        for (int i = 0; i < chars.length; i++) {
            objects[i] = toObjectFromChar(chars[i], isNilTagProfile, node);
        }
        storage = objects;
    }

    public void transitionFromDoublesToObjects(final InlinedConditionProfile isNilTagProfile, final Node node) {
        LogUtils.ARRAY_STATEGIES.finer("transition from Doubles to Objects");
        final double[] doubles = getDoubleStorage();
        final Object[] objects = new Object[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            objects[i] = toObjectFromDouble(doubles[i], isNilTagProfile, node);
        }
        storage = objects;
    }

    public void transitionFromEmptyToBooleans() {
        // Zero-initialized, no need to fill with BOOLEAN_NIL_TAG.
        storage = new byte[getEmptyStorage()];
    }

    public void transitionFromEmptyToChars() {
        final char[] chars = new char[getEmptyStorage()];
        ArrayUtils.fill(chars, CHAR_NIL_TAG);
        storage = chars;
    }

    public void transitionFromEmptyToDoubles() {
        final double[] doubles = new double[getEmptyStorage()];
        ArrayUtils.fill(doubles, DOUBLE_NIL_TAG);
        storage = doubles;
    }

    public void transitionFromEmptyToLongs() {
        final long[] longs = new long[getEmptyStorage()];
        ArrayUtils.fill(longs, LONG_NIL_TAG);
        storage = longs;
    }

    public void transitionFromEmptyToObjects() {
        storage = ArrayUtils.withAll(getEmptyLength(), NilObject.SINGLETON);
    }

    public void transitionFromLongsToObjects(final InlinedConditionProfile isNilTagProfile, final Node node) {
        LogUtils.ARRAY_STATEGIES.finer("transition from Longs to Objects");
        final long[] longs = getLongStorage();
        final Object[] objects = new Object[longs.length];
        for (int i = 0; i < longs.length; i++) {
            objects[i] = toObjectFromLong(longs[i], isNilTagProfile, node);
        }
        storage = objects;
    }

    @Override
    public void pointersBecomeOneWay(final UnmodifiableEconomicMap<Object, Object> fromToMap) {
        super.pointersBecomeOneWay(fromToMap);
        if (isObjectType()) {
            ArrayUtils.replaceAll(getObjectStorage(), fromToMap);
        }
    }

    @Override
    public void tracePointers(final ObjectTracer tracer) {
        super.tracePointers(tracer);
        if (isObjectType()) {
            tracer.addAllIfUnmarked(getObjectStorage());
        }
    }

    @Override
    public void trace(final SqueakImageWriter writer) {
        super.trace(writer);
        if (isObjectType()) {
            writer.traceAllIfNecessary(getObjectStorage());
        }
    }

    @Override
    public void write(final SqueakImageWriter writer) {
        if (!writeHeader(writer)) {
            return;
        }
        if (isEmptyType()) {
            for (int i = 0; i < getEmptyLength(); i++) {
                writer.writeNil();
            }
        } else if (isBooleanType()) {
            for (final byte item : getBooleanStorage()) {
                if (item == BOOLEAN_FALSE_TAG) {
                    writer.writeFalse();
                } else if (item == BOOLEAN_TRUE_TAG) {
                    writer.writeTrue();
                } else {
                    assert item == BOOLEAN_NIL_TAG;
                    writer.writeNil();
                }
            }
        } else if (isCharType()) {
            for (final char item : getCharStorage()) {
                if (isCharNilTag(item)) {
                    writer.writeNil();
                } else {
                    writer.writeChar(item);
                }
            }
        } else if (isDoubleType()) {
            for (final double item : getDoubleStorage()) {
                if (isDoubleNilTag(item)) {
                    writer.writeNil();
                } else {
                    writer.writeSmallFloat(item);
                }
            }
        } else if (isLongType()) {
            for (final long item : getLongStorage()) {
                if (isLongNilTag(item)) {
                    writer.writeNil();
                } else {
                    writer.writeSmallInteger(item);
                }
            }
        } else if (isObjectType()) {
            writer.writeObjects(getObjectStorage());
        }
    }

    public void writeAsHiddenRoots(final SqueakImageWriter writer) {
        assert isObjectType();
        /* Write header. */
        final long numSlots = getObjectLength();
        assert numSlots >= SqueakImageConstants.OVERFLOW_SLOTS;
        writer.writeLong(numSlots | SqueakImageConstants.SLOTS_MASK);
        writer.writeObjectHeader(SqueakImageConstants.OVERFLOW_SLOTS, 0, getSqueakClass().getInstanceSpecification(), 0, SqueakImageConstants.ARRAY_CLASS_INDEX_PUN);
        /* Write content. */
        for (final Object item : getObjectStorage()) {
            writer.writeObject(item);
        }
    }
}
