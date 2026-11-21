/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model.layout;

import java.io.Serial;

import org.graalvm.collections.EconomicMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.IntValueProfile;

import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public abstract class SlotLocation {
    public static final int NUM_PRIMITIVE_INLINE_LOCATIONS = 2;
    public static final int MAX_PRIMITIVE_LOCATIONS = Integer.SIZE;
    public static final int NUM_OBJECT_INLINE_LOCATIONS = 3;

    @CompilationFinal(dimensions = 1) private static final long[] PRIMITIVE_ADDRESSES = new long[NUM_PRIMITIVE_INLINE_LOCATIONS];
    @CompilationFinal(dimensions = 1) private static final long[] OBJECT_ADDRESSES = new long[NUM_OBJECT_INLINE_LOCATIONS];

    public static final UninitializedSlotLocation UNINITIALIZED_LOCATION = new UninitializedSlotLocation();
    public static final EconomicMap<Integer, SlotLocation> OBJECT_LOCATIONS = EconomicMap.create();

    /**
     * Initializes slot locations. Slot locations use Unsafe to read inline fields of
     * {@link AbstractPointersObject}. Delaying their initialization allows SubstrateVM to intercept
     * Unsafe access in order to recalculate field offsets/addresses.
     */
    public static void initialize() {
        if (PRIMITIVE_ADDRESSES[0] != 0) {
            return; /* Already initialized */
        }

        PRIMITIVE_ADDRESSES[0] = AbstractPointersObject.PRIMITIVE_0_ADDRESS;
        PRIMITIVE_ADDRESSES[1] = AbstractPointersObject.PRIMITIVE_1_ADDRESS;

        OBJECT_ADDRESSES[0] = AbstractPointersObject.OBJECT_0_ADDRESS;
        OBJECT_ADDRESSES[1] = AbstractPointersObject.OBJECT_1_ADDRESS;
        OBJECT_ADDRESSES[2] = AbstractPointersObject.OBJECT_2_ADDRESS;
        for (int i = 0; i < NUM_OBJECT_INLINE_LOCATIONS; i++) {
            OBJECT_LOCATIONS.put(i, new ObjectInlineSlotLocation(i));
        }
    }

    public static SlotLocation getObjectLocation(final int index) {
        SlotLocation location = OBJECT_LOCATIONS.get(index);
        if (location == null) {
            location = new ObjectExtensionSlotLocation(index);
            OBJECT_LOCATIONS.put(index, location);
        }
        return location;
    }

    public static final class IllegalWriteException extends SlowPathException {
        @Serial private static final long serialVersionUID = 1L;
        private static final IllegalWriteException SINGLETON = new IllegalWriteException();

        private IllegalWriteException() {
        }
    }

    private static void transferToInterpreterAndThrowIllegalWriteException() throws IllegalWriteException {
        CompilerDirectives.transferToInterpreter();
        throw IllegalWriteException.SINGLETON;
    }

    public abstract Object read(AbstractPointersObject obj);

    public abstract void write(AbstractPointersObject obj, Object value) throws IllegalWriteException;

    public final void writeMustSucceed(final AbstractPointersObject obj, final Object value) {
        try {
            write(obj, value);
        } catch (final IllegalWriteException e) {
            CompilerDirectives.transferToInterpreter();
            LogUtils.MAIN.warning(e.toString());
        }
    }

    public abstract boolean canStore(Object value);

    public abstract boolean isSet(AbstractPointersObject object);

    public abstract void unset(AbstractPointersObject object);

    public boolean isUninitialized() {
        return false;
    }

    public boolean isPrimitive() {
        return false;
    }

    public boolean isGeneric() {
        return false;
    }

    public boolean isBool() {
        return false;
    }

    public boolean isChar() {
        return false;
    }

    public boolean isLong() {
        return false;
    }

    public boolean isDouble() {
        return false;
    }

    public boolean isExtension() {
        return false;
    }

    protected int getFieldIndex() {
        return -1;
    }

    public abstract static class AbstractSlotLocationAccessorNode extends Node {
        public static final AbstractSlotLocationAccessorNode create(final SlotLocation location, final boolean isReading) {
            if (location.isPrimitive()) {
                return new PrimitiveSlotLocationAccessorNode((PrimitiveLocation) location, isReading);
            } else if (location.isUninitialized()) {
                return new UninitializedSlotLocationAccessorNode((UninitializedSlotLocation) location);
            } else {
                return new GenericSlotLocationAccessorNode((GenericLocation) location);
            }
        }

        public abstract Object executeRead(AbstractPointersObject object);

        public abstract void executeWrite(AbstractPointersObject object, Object value) throws IllegalWriteException;

        public abstract boolean canStore(Object value);
    }

    private static final class GenericSlotLocationAccessorNode extends AbstractSlotLocationAccessorNode {
        private final GenericLocation location;

        private GenericSlotLocationAccessorNode(final GenericLocation location) {
            this.location = location;
        }

        @Override
        public Object executeRead(final AbstractPointersObject object) {
            return location.read(object);
        }

        @Override
        public void executeWrite(final AbstractPointersObject object, final Object value) throws IllegalWriteException {
            location.write(object, value);
        }

        @Override
        public boolean canStore(final Object value) {
            return location.canStore(value);
        }
    }

    private static final class UninitializedSlotLocationAccessorNode extends AbstractSlotLocationAccessorNode {
        private final UninitializedSlotLocation location;

        private UninitializedSlotLocationAccessorNode(final UninitializedSlotLocation location) {
            this.location = location;
        }

        @Override
        public Object executeRead(final AbstractPointersObject object) {
            return location.read(object);
        }

        @Override
        public void executeWrite(final AbstractPointersObject object, final Object value) throws IllegalWriteException {
            location.write(object, value);
        }

        @Override
        public boolean canStore(final Object value) {
            return location.canStore(value);
        }
    }

    private static final class PrimitiveSlotLocationAccessorNode extends AbstractSlotLocationAccessorNode {
        private final PrimitiveLocation location;
        private final BranchProfile nilProfile;
        private final IntValueProfile primitiveUsedMapProfile = IntValueProfile.createIdentityProfile();

        private PrimitiveSlotLocationAccessorNode(final PrimitiveLocation location, final boolean isReading) {
            this.location = location;
            nilProfile = isReading ? BranchProfile.create() : null;
        }

        @Override
        public Object executeRead(final AbstractPointersObject object) {
            return location.readProfiled(object, primitiveUsedMapProfile, nilProfile);
        }

        @Override
        public void executeWrite(final AbstractPointersObject object, final Object value) throws IllegalWriteException {
            location.writeProfiled(object, value, primitiveUsedMapProfile);
        }

        @Override
        public boolean canStore(final Object value) {
            return location.canStore(value);
        }
    }

    private static final class UninitializedSlotLocation extends SlotLocation {
        @Override
        public Object read(final AbstractPointersObject obj) {
            return NilObject.SINGLETON;
        }

        @Override
        public void write(final AbstractPointersObject obj, final Object value) throws IllegalWriteException {
            if (value != NilObject.SINGLETON) {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        @Override
        public boolean isUninitialized() {
            return true;
        }

        @Override
        public boolean isSet(final AbstractPointersObject obj) {
            return false;
        }

        @Override
        public void unset(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            // Nothing to do.
        }

        @Override
        public boolean canStore(final Object value) {
            return value == NilObject.SINGLETON;
        }
    }

    enum PrimitiveKind {
        BOOL(Byte.BYTES),
        CHAR(Character.BYTES),
        INT(Integer.BYTES),
        LONG(Long.BYTES),
        DOUBLE(Double.BYTES);

        private final int byteSize;

        PrimitiveKind(final int byteSize) {
            this.byteSize = byteSize;
        }

        int getByteSize() {
            return byteSize;
        }
    }

    /**
     * A naturally aligned byte range within a primitive word. Presence bits belong to logical
     * locations, independently of the word containing their payload.
     */
    protected static final class PrimitiveLocation extends SlotLocation {
        private final PrimitiveKind kind;
        private final int byteIndex;
        private final int usedMask;
        private final long offset;

        PrimitiveLocation(final PrimitiveKind kind, final int byteIndex, final int usedIndex) {
            assert byteIndex % kind.getByteSize() == 0;
            this.kind = kind;
            this.byteIndex = byteIndex;
            usedMask = getPrimitiveUsedMask(usedIndex);
            final int fieldIndex = getFieldIndex();
            final int byteOffset = byteIndex % Long.BYTES;
            offset = isExtension() ? UnsafeUtils.toLongsOffset(fieldIndex - NUM_PRIMITIVE_INLINE_LOCATIONS) + byteOffset : PRIMITIVE_ADDRESSES[fieldIndex] + byteOffset;
        }

        int getByteIndex() {
            return byteIndex;
        }

        PrimitiveKind getKind() {
            return kind;
        }

        public Object readProfiled(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile, final BranchProfile nilProfile) {
            if ((primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object)) & usedMask) != 0) {
                return readValue(object);
            } else {
                nilProfile.enter();
                return NilObject.SINGLETON;
            }
        }

        public void writeProfiled(final AbstractPointersObject object, final Object value, final IntValueProfile primitiveUsedMapProfile) throws IllegalWriteException {
            if (canStore(value)) {
                putPrimitiveUsedMap(object, primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object)) | usedMask);
                writeValue(object, value);
            } else {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        @Override
        public Object read(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            return isSet(object) ? readValue(object) : NilObject.SINGLETON;
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) throws IllegalWriteException {
            CompilerAsserts.neverPartOfCompilation();
            if (canStore(value)) {
                putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) | usedMask);
                writeValue(object, value);
            } else {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        private Object getStorage(final AbstractPointersObject object) {
            assert !isExtension() || object.primitiveExtension != null && getFieldIndex() - NUM_PRIMITIVE_INLINE_LOCATIONS < object.primitiveExtension.length;
            return isExtension() ? object.primitiveExtension : object;
        }

        private Object readValue(final AbstractPointersObject object) {
            final Object storage = getStorage(object);
            return switch (kind) {
                case BOOL -> UnsafeUtils.getBoolAt(storage, offset);
                case CHAR -> UnsafeUtils.getCharAt(storage, offset);
                case INT -> (long) UnsafeUtils.getIntAt(storage, offset);
                case LONG -> UnsafeUtils.getLongAt(storage, offset);
                case DOUBLE -> UnsafeUtils.getDoubleAt(storage, offset);
            };
        }

        private void writeValue(final AbstractPointersObject object, final Object value) {
            final Object storage = getStorage(object);
            switch (kind) {
                case BOOL -> UnsafeUtils.putBoolAt(storage, offset, (boolean) value);
                case CHAR -> UnsafeUtils.putCharAt(storage, offset, (char) value);
                case INT -> UnsafeUtils.putIntAt(storage, offset, (int) (long) value);
                case LONG -> UnsafeUtils.putLongAt(storage, offset, (long) value);
                case DOUBLE -> UnsafeUtils.putDoubleAt(storage, offset, (double) value);
            }
        }

        @Override
        public boolean canStore(final Object value) {
            return switch (kind) {
                case BOOL -> value instanceof Boolean;
                case CHAR -> value instanceof Character;
                case INT -> value instanceof final Long v && v == (int) (long) v;
                case LONG -> value instanceof Long;
                case DOUBLE -> value instanceof Double;
            };
        }

        @Override
        public boolean isPrimitive() {
            return true;
        }

        @Override
        public boolean isBool() {
            return kind == PrimitiveKind.BOOL;
        }

        @Override
        public boolean isChar() {
            return kind == PrimitiveKind.CHAR;
        }

        @Override
        public boolean isLong() {
            return kind == PrimitiveKind.LONG;
        }

        @Override
        public boolean isDouble() {
            return kind == PrimitiveKind.DOUBLE;
        }

        @Override
        public boolean isSet(final AbstractPointersObject object) {
            return (getPrimitiveUsedMap(object) & usedMask) != 0;
        }

        @Override
        public void unset(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) & ~usedMask);
            // Clear only this location's bytes; neighboring locations may still need to be read.
            final Object storage = getStorage(object);
            switch (kind) {
                case BOOL -> UnsafeUtils.putBoolAt(storage, offset, false);
                case CHAR -> UnsafeUtils.putCharAt(storage, offset, (char) 0);
                case INT -> UnsafeUtils.putIntAt(storage, offset, 0);
                case LONG, DOUBLE -> UnsafeUtils.putLongAt(storage, offset, 0L);
            }
        }

        @Override
        public boolean isExtension() {
            return getFieldIndex() >= NUM_PRIMITIVE_INLINE_LOCATIONS;
        }

        @Override
        public int getFieldIndex() {
            return byteIndex / Long.BYTES;
        }
    }

    private abstract static class GenericLocation extends SlotLocation {
        @Override
        public final boolean isGeneric() {
            return true;
        }

        @Override
        public final boolean canStore(final Object value) {
            return true;
        }

        @Override
        public final void unset(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            writeMustSucceed(object, NilObject.SINGLETON);
        }
    }

    private static int getPrimitiveUsedMask(final int index) {
        assert 0 <= index && index < Integer.SIZE;
        return 1 << index;
    }

    private static int getPrimitiveUsedMap(final AbstractPointersObject object) {
        return UnsafeUtils.getIntAt(object, AbstractPointersObject.PRIMITIVE_USED_MAP_ADDRESS);
    }

    private static void putPrimitiveUsedMap(final AbstractPointersObject object, final int value) {
        UnsafeUtils.putIntAt(object, AbstractPointersObject.PRIMITIVE_USED_MAP_ADDRESS, value);
    }

    private static final class ObjectInlineSlotLocation extends GenericLocation {
        private final long address;

        private ObjectInlineSlotLocation(final int index) {
            address = OBJECT_ADDRESSES[index];
        }

        @Override
        public Object read(final AbstractPointersObject object) {
            return UnsafeUtils.getObjectAt(object, address);
        }

        @Override
        public boolean isSet(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            assert UnsafeUtils.getObjectAt(object, address) != null : "Unexpected null value (initialized with nil)";
            return true;
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            UnsafeUtils.putObjectAt(object, address, value);
        }

        @Override
        public int getFieldIndex() {
            return ArrayUtils.indexOf(OBJECT_ADDRESSES, address);
        }
    }

    private static final class ObjectExtensionSlotLocation extends GenericLocation {
        private final long offset;

        private ObjectExtensionSlotLocation(final int index) {
            offset = UnsafeUtils.toObjectsOffset(index - NUM_OBJECT_INLINE_LOCATIONS);
        }

        @Override
        public Object read(final AbstractPointersObject object) {
            return UnsafeUtils.getObjectOffset(object.objectExtension, offset);
        }

        @Override
        public boolean isSet(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            assert object.objectExtension == null || UnsafeUtils.getObjectOffset(object.objectExtension, offset) != null : "Unexpected null value (initialized with nil)";
            return object.objectExtension != null;
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            UnsafeUtils.putObjectOffset(object.objectExtension, offset, value);
        }

        @Override
        public boolean isExtension() {
            return true;
        }

        @Override
        public int getFieldIndex() {
            return SlotLocation.NUM_OBJECT_INLINE_LOCATIONS + (int) UnsafeUtils.fromObjectsOffset(offset);
        }
    }
}
