/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model.layout;

import org.graalvm.collections.EconomicMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.IntValueProfile;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.UnsafeUtils;

public abstract class SlotLocation {
    public static final int NUM_PRIMITIVE_INLINE_LOCATIONS = 3;
    public static final int NUM_PRIMITIVE_EXTENSION_LOCATIONS = Integer.SIZE - NUM_PRIMITIVE_INLINE_LOCATIONS;
    public static final int NUM_OBJECT_INLINE_LOCATIONS = 3;

    @CompilationFinal(dimensions = 1) private static final long[] PRIMITIVE_ADDRESSES = new long[NUM_PRIMITIVE_INLINE_LOCATIONS];
    @CompilationFinal(dimensions = 1) private static final long[] OBJECT_ADDRESSES = new long[NUM_OBJECT_INLINE_LOCATIONS];

    public static final UninitializedSlotLocation UNINITIALIZED_LOCATION = new UninitializedSlotLocation();
    public static final SlotLocation[] BOOL_LOCATIONS = new SlotLocation[NUM_PRIMITIVE_INLINE_LOCATIONS + NUM_PRIMITIVE_EXTENSION_LOCATIONS];
    public static final SlotLocation[] CHAR_LOCATIONS = new SlotLocation[NUM_PRIMITIVE_INLINE_LOCATIONS + NUM_PRIMITIVE_EXTENSION_LOCATIONS];
    public static final SlotLocation[] LONG_LOCATIONS = new SlotLocation[NUM_PRIMITIVE_INLINE_LOCATIONS + NUM_PRIMITIVE_EXTENSION_LOCATIONS];
    public static final SlotLocation[] DOUBLE_LOCATIONS = new SlotLocation[NUM_PRIMITIVE_INLINE_LOCATIONS + NUM_PRIMITIVE_EXTENSION_LOCATIONS];
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
        PRIMITIVE_ADDRESSES[2] = AbstractPointersObject.PRIMITIVE_2_ADDRESS;

        for (int i = 0; i < NUM_PRIMITIVE_INLINE_LOCATIONS; i++) {
            BOOL_LOCATIONS[i] = new BoolInlineSlotLocation(i);
            CHAR_LOCATIONS[i] = new CharInlineSlotLocation(i);
            LONG_LOCATIONS[i] = new LongInlineSlotLocation(i);
            DOUBLE_LOCATIONS[i] = new DoubleInlineSlotLocation(i);
        }

        for (int i = NUM_PRIMITIVE_INLINE_LOCATIONS; i < NUM_PRIMITIVE_INLINE_LOCATIONS + NUM_PRIMITIVE_EXTENSION_LOCATIONS; i++) {
            BOOL_LOCATIONS[i] = new BoolExtensionSlotLocation(i);
            CHAR_LOCATIONS[i] = new CharExtensionSlotLocation(i);
            LONG_LOCATIONS[i] = new LongExtensionSlotLocation(i);
            DOUBLE_LOCATIONS[i] = new DoubleExtensionSlotLocation(i);
        }

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

    public static final class IllegalWriteException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private static final IllegalWriteException SINGLETON = new IllegalWriteException();

        private IllegalWriteException() {
            super(null, null);
        }
    }

    private static void transferToInterpreterAndThrowIllegalWriteException() {
        CompilerDirectives.transferToInterpreter();
        throw IllegalWriteException.SINGLETON;
    }

    public abstract Object read(AbstractPointersObject obj);

    public abstract void write(AbstractPointersObject obj, Object value);

    public final void writeMustSucceed(final AbstractPointersObject obj, final Object value) {
        try {
            write(obj, value);
        } catch (final IllegalWriteException e) {
            throw SqueakException.illegalState(e);
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

    protected boolean isGeneric() {
        return false;
    }

    protected boolean isBool() {
        return false;
    }

    protected boolean isChar() {
        return false;
    }

    protected boolean isLong() {
        return false;
    }

    protected boolean isDouble() {
        return false;
    }

    protected boolean isExtension() {
        return false;
    }

    protected int getFieldIndex() {
        return -1;
    }

    @NodeInfo(cost = NodeCost.NONE)
    public abstract static class AbstractSlotLocationAccessorNode extends Node {
        public static final AbstractSlotLocationAccessorNode create(final SlotLocation location) {
            if (location.isPrimitive()) {
                return new PrimitiveSlotLocationAccessorNode((PrimitiveLocation) location);
            } else {
                return new NonPrimitiveSlotLocationAccessorNode(location);
            }
        }

        public abstract Object executeRead(AbstractPointersObject object);

        public abstract void executeWrite(AbstractPointersObject object, Object value);

        public abstract boolean canStore(Object value);
    }

    private static final class NonPrimitiveSlotLocationAccessorNode extends AbstractSlotLocationAccessorNode {
        private final SlotLocation location;

        private NonPrimitiveSlotLocationAccessorNode(final SlotLocation location) {
            this.location = location;
        }

        @Override
        public Object executeRead(final AbstractPointersObject object) {
            return location.read(object);
        }

        @Override
        public void executeWrite(final AbstractPointersObject object, final Object value) {
            location.write(object, value);
        }

        @Override
        public boolean canStore(final Object value) {
            return location.canStore(value);
        }
    }

    private static final class PrimitiveSlotLocationAccessorNode extends AbstractSlotLocationAccessorNode {
        private final PrimitiveLocation location;
        private final BranchProfile nilProfile = BranchProfile.create();
        private final IntValueProfile primitiveUsedMapProfile = IntValueProfile.createIdentityProfile();

        private PrimitiveSlotLocationAccessorNode(final PrimitiveLocation location) {
            this.location = location;
        }

        @Override
        public Object executeRead(final AbstractPointersObject object) {
            return location.readProfiled(object, primitiveUsedMapProfile, nilProfile);
        }

        @Override
        public void executeWrite(final AbstractPointersObject object, final Object value) {
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
        public void write(final AbstractPointersObject obj, final Object value) {
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

    protected abstract static class PrimitiveLocation extends SlotLocation {
        private final int usedMask;

        public PrimitiveLocation(final int index) {
            usedMask = getPrimitiveUsedMask(index);
        }

        public abstract Object readProfiled(AbstractPointersObject object, IntValueProfile primitiveUsedMapProfile, BranchProfile nilProfile);

        public abstract void writeProfiled(AbstractPointersObject object, Object value, IntValueProfile primitiveUsedMapProfile);

        @Override
        public final boolean isPrimitive() {
            return true;
        }

        @Override
        public final boolean isSet(final AbstractPointersObject object) {
            return (getPrimitiveUsedMap(object) & usedMask) != 0;
        }

        public final boolean isSet(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile) {
            return (primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object)) & usedMask) != 0;
        }

        public final void unsetMask(final AbstractPointersObject object) {
            putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) & ~usedMask);
        }

        public final void setMask(final AbstractPointersObject object) {
            putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) | usedMask);
        }

        public final void setMask(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile) {
            putPrimitiveUsedMap(object, primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object)) | usedMask);
        }
    }

    private abstract static class BoolLocation extends PrimitiveLocation {
        private BoolLocation(final int index) {
            super(index);
        }

        @Override
        public final boolean isBool() {
            return true;
        }

        @Override
        public final boolean canStore(final Object value) {
            return value instanceof Boolean;
        }
    }

    private abstract static class CharLocation extends PrimitiveLocation {
        private CharLocation(final int index) {
            super(index);
        }

        @Override
        public final boolean isChar() {
            return true;
        }

        @Override
        public final boolean canStore(final Object value) {
            return value instanceof Character;
        }
    }

    private abstract static class LongLocation extends PrimitiveLocation {
        private LongLocation(final int index) {
            super(index);
        }

        @Override
        public final boolean isLong() {
            return true;
        }

        @Override
        public final boolean canStore(final Object value) {
            return value instanceof Long;
        }
    }

    private abstract static class DoubleLocation extends PrimitiveLocation {
        private DoubleLocation(final int index) {
            super(index);
        }

        @Override
        public final boolean isDouble() {
            return true;
        }

        @Override
        public final boolean canStore(final Object value) {
            return value instanceof Double;
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
            write(object, NilObject.SINGLETON);
        }
    }

    protected static int getPrimitiveUsedMask(final int index) {
        assert 0 <= index && index < Integer.SIZE;
        return 1 << index;
    }

    private static int getPrimitiveUsedMap(final AbstractPointersObject object) {
        return UnsafeUtils.getIntAt(object, AbstractPointersObject.PRIMITIVE_USED_MAP_ADDRESS);
    }

    private static void putPrimitiveUsedMap(final AbstractPointersObject object, final int value) {
        UnsafeUtils.putIntAt(object, AbstractPointersObject.PRIMITIVE_USED_MAP_ADDRESS, value);
    }

    private static final class BoolInlineSlotLocation extends BoolLocation {
        private final long address;

        private BoolInlineSlotLocation(final int index) {
            super(index);
            address = PRIMITIVE_ADDRESSES[index];
        }

        @Override
        public Object readProfiled(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile, final BranchProfile nilProfile) {
            if (isSet(object, primitiveUsedMapProfile)) {
                return UnsafeUtils.getBoolAt(object, address);
            } else {
                nilProfile.enter();
                return NilObject.SINGLETON;
            }
        }

        @Override
        public Object read(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            if (isSet(object)) {
                return UnsafeUtils.getBoolAt(object, address);
            } else {
                return NilObject.SINGLETON;
            }
        }

        @Override
        public void writeProfiled(final AbstractPointersObject object, final Object value, final IntValueProfile primitiveUsedMapProfile) {
            if (value instanceof Boolean) {
                setMask(object, primitiveUsedMapProfile);
                UnsafeUtils.putBoolAt(object, address, (boolean) value);
            } else {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            CompilerAsserts.neverPartOfCompilation();
            if (value instanceof Boolean) {
                setMask(object);
                UnsafeUtils.putBoolAt(object, address, (boolean) value);
            } else {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        @Override
        public void unset(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            unsetMask(object);
            UnsafeUtils.putLongAt(object, address, 0L);
        }

        @Override
        public int getFieldIndex() {
            return ArrayUtils.indexOf(PRIMITIVE_ADDRESSES, address);
        }
    }

    private static final class BoolExtensionSlotLocation extends BoolLocation {
        private final int index;

        private BoolExtensionSlotLocation(final int index) {
            super(index);
            this.index = index - NUM_PRIMITIVE_INLINE_LOCATIONS;
        }

        @Override
        public Object readProfiled(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile, final BranchProfile nilProfile) {
            if (isSet(object, primitiveUsedMapProfile)) {
                return UnsafeUtils.getBoolFromLongs(object.primitiveExtension, index);
            } else {
                nilProfile.enter();
                return NilObject.SINGLETON;
            }
        }

        @Override
        public Object read(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            if (isSet(object)) {
                return UnsafeUtils.getBoolFromLongs(object.primitiveExtension, index);
            } else {
                return NilObject.SINGLETON;
            }
        }

        @Override
        public void writeProfiled(final AbstractPointersObject object, final Object value, final IntValueProfile primitiveUsedMapProfile) {
            if (canStore(value)) {
                setMask(object, primitiveUsedMapProfile);
                UnsafeUtils.putBoolIntoLongs(object.primitiveExtension, index, (boolean) value);
            } else {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            CompilerAsserts.neverPartOfCompilation();
            if (canStore(value)) {
                setMask(object);
                UnsafeUtils.putBoolIntoLongs(object.primitiveExtension, index, (boolean) value);
            } else {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        @Override
        public void unset(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            unsetMask(object);
            if (object.primitiveExtension != null) {
                object.primitiveExtension[index] = 0L;
            }
        }

        @Override
        protected boolean isExtension() {
            return true;
        }

        @Override
        public int getFieldIndex() {
            return SlotLocation.NUM_PRIMITIVE_INLINE_LOCATIONS + index;
        }
    }

    private static final class CharInlineSlotLocation extends CharLocation {
        private final long address;

        private CharInlineSlotLocation(final int index) {
            super(index);
            address = PRIMITIVE_ADDRESSES[index];
        }

        @Override
        public Object readProfiled(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile, final BranchProfile nilProfile) {
            if (isSet(object, primitiveUsedMapProfile)) {
                return UnsafeUtils.getCharAt(object, address);
            } else {
                nilProfile.enter();
                return NilObject.SINGLETON;
            }
        }

        @Override
        public Object read(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            if (isSet(object)) {
                return UnsafeUtils.getCharAt(object, address);
            } else {
                return NilObject.SINGLETON;
            }
        }

        @Override
        public void writeProfiled(final AbstractPointersObject object, final Object value, final IntValueProfile primitiveUsedMapProfile) {
            if (canStore(value)) {
                setMask(object, primitiveUsedMapProfile);
                UnsafeUtils.putCharAt(object, address, (char) value);
            } else {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            CompilerAsserts.neverPartOfCompilation();
            if (canStore(value)) {
                setMask(object);
                UnsafeUtils.putCharAt(object, address, (char) value);
            } else {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        @Override
        public void unset(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            unsetMask(object);
            UnsafeUtils.putLongAt(object, address, 0L);
        }

        @Override
        public int getFieldIndex() {
            return ArrayUtils.indexOf(PRIMITIVE_ADDRESSES, address);
        }
    }

    private static final class CharExtensionSlotLocation extends CharLocation {
        private final int index;

        private CharExtensionSlotLocation(final int index) {
            super(index);
            this.index = index - NUM_PRIMITIVE_INLINE_LOCATIONS;
        }

        @Override
        public Object readProfiled(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile, final BranchProfile nilProfile) {
            if (isSet(object, primitiveUsedMapProfile)) {
                return UnsafeUtils.getCharFromLongs(object.primitiveExtension, index);
            } else {
                nilProfile.enter();
                return NilObject.SINGLETON;
            }
        }

        @Override
        public Object read(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            if (isSet(object)) {
                return UnsafeUtils.getCharFromLongs(object.primitiveExtension, index);
            } else {
                return NilObject.SINGLETON;
            }
        }

        @Override
        public void writeProfiled(final AbstractPointersObject object, final Object value, final IntValueProfile primitiveUsedMapProfile) {
            if (canStore(value)) {
                setMask(object, primitiveUsedMapProfile);
                UnsafeUtils.putCharIntoLongs(object.primitiveExtension, index, (char) value);
            } else {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            CompilerAsserts.neverPartOfCompilation();
            if (canStore(value)) {
                setMask(object);
                UnsafeUtils.putCharIntoLongs(object.primitiveExtension, index, (char) value);
            } else {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        @Override
        public void unset(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            unsetMask(object);
            if (object.primitiveExtension != null) {
                object.primitiveExtension[index] = 0L;
            }
        }

        @Override
        protected boolean isExtension() {
            return true;
        }

        @Override
        public int getFieldIndex() {
            return SlotLocation.NUM_PRIMITIVE_INLINE_LOCATIONS + index;
        }
    }

    private static final class LongInlineSlotLocation extends LongLocation {
        private final long address;

        private LongInlineSlotLocation(final int index) {
            super(index);
            address = PRIMITIVE_ADDRESSES[index];
        }

        @Override
        public Object readProfiled(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile, final BranchProfile nilProfile) {
            if (isSet(object, primitiveUsedMapProfile)) {
                return UnsafeUtils.getLongAt(object, address);
            } else {
                nilProfile.enter();
                return NilObject.SINGLETON;
            }
        }

        @Override
        public Object read(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            if (isSet(object)) {
                return UnsafeUtils.getLongAt(object, address);
            } else {
                return NilObject.SINGLETON;
            }
        }

        @Override
        public void writeProfiled(final AbstractPointersObject object, final Object value, final IntValueProfile primitiveUsedMapProfile) {
            if (canStore(value)) {
                setMask(object, primitiveUsedMapProfile);
                UnsafeUtils.putLongAt(object, address, (long) value);
            } else {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            CompilerAsserts.neverPartOfCompilation();
            if (canStore(value)) {
                setMask(object);
                UnsafeUtils.putLongAt(object, address, (long) value);
            } else {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        @Override
        public void unset(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            unsetMask(object);
            UnsafeUtils.putLongAt(object, address, 0L);
        }

        @Override
        public int getFieldIndex() {
            return ArrayUtils.indexOf(PRIMITIVE_ADDRESSES, address);
        }
    }

    private static final class LongExtensionSlotLocation extends LongLocation {
        private final int index;

        private LongExtensionSlotLocation(final int index) {
            super(index);
            this.index = index - NUM_PRIMITIVE_INLINE_LOCATIONS;
        }

        @Override
        public Object readProfiled(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile, final BranchProfile nilProfile) {
            if (isSet(object, primitiveUsedMapProfile)) {
                return UnsafeUtils.getLong(object.primitiveExtension, index);
            } else {
                nilProfile.enter();
                return NilObject.SINGLETON;
            }
        }

        @Override
        public Object read(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            if (isSet(object)) {
                return UnsafeUtils.getLong(object.primitiveExtension, index);
            } else {
                return NilObject.SINGLETON;
            }
        }

        @Override
        public void writeProfiled(final AbstractPointersObject object, final Object value, final IntValueProfile primitiveUsedMapProfile) {
            if (canStore(value)) {
                setMask(object, primitiveUsedMapProfile);
                UnsafeUtils.putLong(object.primitiveExtension, index, (long) value);
            } else {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            CompilerAsserts.neverPartOfCompilation();
            if (canStore(value)) {
                setMask(object);
                UnsafeUtils.putLong(object.primitiveExtension, index, (long) value);
            } else {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        @Override
        public void unset(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            unsetMask(object);
            if (object.primitiveExtension != null) {
                object.primitiveExtension[index] = 0L;
            }
        }

        @Override
        protected boolean isExtension() {
            return true;
        }

        @Override
        public int getFieldIndex() {
            return SlotLocation.NUM_PRIMITIVE_INLINE_LOCATIONS + index;
        }
    }

    private static final class DoubleInlineSlotLocation extends DoubleLocation {
        private final long address;

        private DoubleInlineSlotLocation(final int index) {
            super(index);
            address = PRIMITIVE_ADDRESSES[index];
        }

        @Override
        public Object readProfiled(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile, final BranchProfile nilProfile) {
            if (isSet(object, primitiveUsedMapProfile)) {
                return UnsafeUtils.getDoubleAt(object, address);
            } else {
                nilProfile.enter();
                return NilObject.SINGLETON;
            }
        }

        @Override
        public Object read(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            if (isSet(object)) {
                return UnsafeUtils.getDoubleAt(object, address);
            } else {
                return NilObject.SINGLETON;
            }
        }

        @Override
        public void writeProfiled(final AbstractPointersObject object, final Object value, final IntValueProfile primitiveUsedMapProfile) {
            if (canStore(value)) {
                setMask(object, primitiveUsedMapProfile);
                UnsafeUtils.putDoubleAt(object, address, (double) value);
            } else {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            CompilerAsserts.neverPartOfCompilation();
            if (canStore(value)) {
                setMask(object);
                UnsafeUtils.putDoubleAt(object, address, (double) value);
            } else {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        @Override
        public void unset(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            unsetMask(object);
            UnsafeUtils.putLongAt(object, address, 0L);
        }

        @Override
        public int getFieldIndex() {
            return ArrayUtils.indexOf(PRIMITIVE_ADDRESSES, address);
        }
    }

    private static final class DoubleExtensionSlotLocation extends DoubleLocation {
        private final int index;

        private DoubleExtensionSlotLocation(final int index) {
            super(index);
            this.index = index - NUM_PRIMITIVE_INLINE_LOCATIONS;
        }

        @Override
        public Object readProfiled(final AbstractPointersObject obj, final IntValueProfile primitiveUsedMapProfile, final BranchProfile nilProfile) {
            if (isSet(obj, primitiveUsedMapProfile)) {
                return UnsafeUtils.getDoubleFromLongs(obj.primitiveExtension, index);
            } else {
                nilProfile.enter();
                return NilObject.SINGLETON;
            }
        }

        @Override
        public Object read(final AbstractPointersObject obj) {
            CompilerAsserts.neverPartOfCompilation();
            if (isSet(obj)) {
                return UnsafeUtils.getDoubleFromLongs(obj.primitiveExtension, index);
            } else {
                return NilObject.SINGLETON;
            }
        }

        @Override
        public void writeProfiled(final AbstractPointersObject object, final Object value, final IntValueProfile primitiveUsedMapProfile) {
            if (canStore(value)) {
                setMask(object, primitiveUsedMapProfile);
                UnsafeUtils.putDoubleIntoLongs(object.primitiveExtension, index, (double) value);
            } else {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            CompilerAsserts.neverPartOfCompilation();
            if (canStore(value)) {
                setMask(object);
                UnsafeUtils.putDoubleIntoLongs(object.primitiveExtension, index, (double) value);
            } else {
                transferToInterpreterAndThrowIllegalWriteException();
            }
        }

        @Override
        public void unset(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            unsetMask(object);
            if (object.primitiveExtension != null) {
                object.primitiveExtension[index] = 0L;
            }
        }

        @Override
        protected boolean isExtension() {
            return true;
        }

        @Override
        public int getFieldIndex() {
            return SlotLocation.NUM_PRIMITIVE_INLINE_LOCATIONS + index;
        }
    }

    private static final class ObjectInlineSlotLocation extends GenericLocation {
        private final int index;

        private ObjectInlineSlotLocation(final int index) {
            this.index = index;
        }

        @Override
        public Object read(final AbstractPointersObject object) {
            assert isSet(object);
            return UnsafeUtils.getObjectAt(object, OBJECT_ADDRESSES[index]);
        }

        @Override
        public boolean isSet(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            assert UnsafeUtils.getObjectAt(object, OBJECT_ADDRESSES[index]) != null : "Unexpected null value (initialized with nil)";
            return true;
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            UnsafeUtils.putObjectAt(object, OBJECT_ADDRESSES[index], value);
            assert isSet(object);
        }

        @Override
        public int getFieldIndex() {
            return index;
        }
    }

    private static final class ObjectExtensionSlotLocation extends GenericLocation {
        private final int index;

        private ObjectExtensionSlotLocation(final int index) {
            this.index = index - NUM_OBJECT_INLINE_LOCATIONS;
        }

        @Override
        public Object read(final AbstractPointersObject object) {
            assert isSet(object);
            return UnsafeUtils.getObject(object.objectExtension, index);
        }

        @Override
        public boolean isSet(final AbstractPointersObject object) {
            CompilerAsserts.neverPartOfCompilation();
            assert object.objectExtension == null || UnsafeUtils.getObject(object.objectExtension, index) != null : "Unexpected null value (initialized with nil)";
            return object.objectExtension != null;
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            UnsafeUtils.putObject(object.objectExtension, index, value);
            assert isSet(object);
        }

        @Override
        protected boolean isExtension() {
            return true;
        }

        @Override
        public int getFieldIndex() {
            return SlotLocation.NUM_OBJECT_INLINE_LOCATIONS + index;
        }
    }
}
