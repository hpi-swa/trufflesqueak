/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model.layout;

import org.graalvm.collections.EconomicMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.IntValueProfile;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.layout.SlotLocationFactory.ReadSlotLocationNodeGen;
import de.hpi.swa.graal.squeak.model.layout.SlotLocationFactory.WriteSlotLocationNodeGen;
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

    public static final class IllegalWriteException extends ControlFlowException {
        private static final long serialVersionUID = 1L;
        public static final IllegalWriteException SINGLETON = new IllegalWriteException();

        private IllegalWriteException() {
        }
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

    public void unset(@SuppressWarnings("unused") final AbstractPointersObject object) {
        /* Do nothing by default. */
    }

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

    @GenerateUncached
    @NodeInfo(cost = NodeCost.NONE)
    public abstract static class ReadSlotLocationNode extends Node {
        public abstract Object execute(SlotLocation location, AbstractPointersObject object);

        public static ReadSlotLocationNode getUncached() {
            return ReadSlotLocationNodeGen.getUncached();
        }

        @Specialization
        protected static final Object doPrimitive(final PrimitiveLocation location, final AbstractPointersObject object,
                        @Cached("createIdentityProfile()") final IntValueProfile primitiveUsedMapProfile) {
            return location.readProfiled(object, primitiveUsedMapProfile);
        }

        @Fallback
        protected static final Object doGeneric(final SlotLocation location, final AbstractPointersObject object) {
            return location.read(object);
        }
    }

    @GenerateUncached
    public abstract static class WriteSlotLocationNode extends Node {
        public abstract void execute(SlotLocation location, AbstractPointersObject object, Object value);

        public static WriteSlotLocationNode getUncached() {
            return WriteSlotLocationNodeGen.getUncached();
        }

        @Specialization
        protected static final void doPrimitive(final PrimitiveLocation location, final AbstractPointersObject object, final Object value,
                        @Cached("createIdentityProfile()") final IntValueProfile primitiveUsedMapProfile) {
            location.writeProfiled(object, value, primitiveUsedMapProfile);
        }

        @Fallback
        protected static final void doGeneric(final SlotLocation location, final AbstractPointersObject object, final Object value) {
            location.write(object, value);
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
                throw IllegalWriteException.SINGLETON;
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
        public boolean canStore(final Object value) {
            return false;
        }
    }

    protected abstract static class PrimitiveLocation extends SlotLocation {
        public abstract Object readProfiled(AbstractPointersObject object, IntValueProfile primitiveUsedMapProfile);

        public abstract void writeProfiled(AbstractPointersObject object, Object value, IntValueProfile primitiveUsedMapProfile);

        @Override
        public final boolean isPrimitive() {
            return true;
        }

    }

    private abstract static class BoolLocation extends PrimitiveLocation {
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
        private final int usedMask;

        private BoolInlineSlotLocation(final int index) {
            address = PRIMITIVE_ADDRESSES[index];
            usedMask = getPrimitiveUsedMask(index);
        }

        @Override
        public Object readProfiled(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile) {
            if (isSet(object, primitiveUsedMapProfile)) {
                return UnsafeUtils.getBoolAt(object, address);
            } else {
                return NilObject.SINGLETON;
            }
        }

        private boolean isSet(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile) {
            return primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object) & usedMask) != 0;
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
        public boolean isSet(final AbstractPointersObject object) {
            return (getPrimitiveUsedMap(object) & usedMask) != 0;
        }

        @Override
        public void unset(final AbstractPointersObject object) {
            putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) & ~usedMask);
        }

        @Override
        public void writeProfiled(final AbstractPointersObject object, final Object value, final IntValueProfile primitiveUsedMapProfile) {
            if (value instanceof Boolean) {
                putPrimitiveUsedMap(object, primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object)) | usedMask);
                UnsafeUtils.putBoolAt(object, address, (boolean) value);
            } else {
                throw IllegalWriteException.SINGLETON;
            }
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            CompilerAsserts.neverPartOfCompilation();
            if (value instanceof Boolean) {
                putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) | usedMask);
                UnsafeUtils.putBoolAt(object, address, (boolean) value);
            } else {
                throw IllegalWriteException.SINGLETON;
            }
        }

        @Override
        public int getFieldIndex() {
            return ArrayUtils.indexOf(PRIMITIVE_ADDRESSES, address);
        }
    }

    private static final class BoolExtensionSlotLocation extends BoolLocation {
        private final int index;
        private final int usedMask;

        private BoolExtensionSlotLocation(final int index) {
            this.index = index - NUM_PRIMITIVE_INLINE_LOCATIONS;
            usedMask = getPrimitiveUsedMask(index);
        }

        @Override
        public Object readProfiled(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile) {
            if (isSet(object, primitiveUsedMapProfile)) {
                return UnsafeUtils.getBoolFromLongs(object.primitiveExtension, index);
            } else {
                return NilObject.SINGLETON;
            }
        }

        private boolean isSet(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile) {
            return (primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object)) & usedMask) != 0;
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
        public boolean isSet(final AbstractPointersObject object) {
            return (getPrimitiveUsedMap(object) & usedMask) != 0;
        }

        @Override
        public void unset(final AbstractPointersObject object) {
            putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) & ~usedMask);
        }

        @Override
        public void writeProfiled(final AbstractPointersObject object, final Object value, final IntValueProfile primitiveUsedMapProfile) {
            if (value instanceof Boolean) {
                putPrimitiveUsedMap(object, primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object)) | usedMask);
                UnsafeUtils.putBoolIntoLongs(object.primitiveExtension, index, (boolean) value);
            } else {
                throw IllegalWriteException.SINGLETON;
            }
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            CompilerAsserts.neverPartOfCompilation();
            if (value instanceof Boolean) {
                putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) | usedMask);
                object.primitiveExtension[index] = (boolean) value ? 1 : 0;
                UnsafeUtils.putBoolIntoLongs(object.primitiveExtension, index, (boolean) value);
            } else {
                throw IllegalWriteException.SINGLETON;
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
        private final int usedMask;

        private CharInlineSlotLocation(final int index) {
            address = PRIMITIVE_ADDRESSES[index];
            usedMask = getPrimitiveUsedMask(index);
        }

        @Override
        public Object readProfiled(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile) {
            if (isSet(object, primitiveUsedMapProfile)) {
                return UnsafeUtils.getCharAt(object, address);
            } else {
                return NilObject.SINGLETON;
            }
        }

        private boolean isSet(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile) {
            return (primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object)) & usedMask) != 0;
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
        public boolean isSet(final AbstractPointersObject object) {
            return (getPrimitiveUsedMap(object) & usedMask) != 0;
        }

        @Override
        public void unset(final AbstractPointersObject object) {
            putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) & ~usedMask);
        }

        @Override
        public void writeProfiled(final AbstractPointersObject object, final Object value, final IntValueProfile primitiveUsedMapProfile) {
            if (value instanceof Character) {
                putPrimitiveUsedMap(object, primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object)) | usedMask);
                UnsafeUtils.putCharAt(object, address, (char) value);
            } else {
                throw IllegalWriteException.SINGLETON;
            }
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            CompilerAsserts.neverPartOfCompilation();
            if (value instanceof Character) {
                putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) | usedMask);
                UnsafeUtils.putCharAt(object, address, (char) value);
            } else {
                throw IllegalWriteException.SINGLETON;
            }
        }

        @Override
        public int getFieldIndex() {
            return ArrayUtils.indexOf(PRIMITIVE_ADDRESSES, address);
        }
    }

    private static final class CharExtensionSlotLocation extends CharLocation {
        private final int index;
        private final int usedMask;

        private CharExtensionSlotLocation(final int index) {
            this.index = index - NUM_PRIMITIVE_INLINE_LOCATIONS;
            usedMask = getPrimitiveUsedMask(index);
        }

        @Override
        public Object readProfiled(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile) {
            if (isSet(object, primitiveUsedMapProfile)) {
                return UnsafeUtils.getCharFromLongs(object.primitiveExtension, index);
            } else {
                return NilObject.SINGLETON;
            }
        }

        private boolean isSet(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile) {
            return (primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object)) & usedMask) != 0;
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
        public boolean isSet(final AbstractPointersObject object) {
            return (getPrimitiveUsedMap(object) & usedMask) != 0;
        }

        @Override
        public void unset(final AbstractPointersObject object) {
            putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) & ~usedMask);
        }

        @Override
        public void writeProfiled(final AbstractPointersObject object, final Object value, final IntValueProfile primitiveUsedMapProfile) {
            if (value instanceof Character) {
                putPrimitiveUsedMap(object, primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object)) | usedMask);
                UnsafeUtils.putCharIntoLongs(object.primitiveExtension, index, (char) value);
            } else {
                throw IllegalWriteException.SINGLETON;
            }
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            CompilerAsserts.neverPartOfCompilation();
            if (value instanceof Character) {
                putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) | usedMask);
                UnsafeUtils.putCharIntoLongs(object.primitiveExtension, index, (char) value);
            } else {
                throw IllegalWriteException.SINGLETON;
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
        private final int usedMask;

        private LongInlineSlotLocation(final int index) {
            address = PRIMITIVE_ADDRESSES[index];
            usedMask = getPrimitiveUsedMask(index);
        }

        @Override
        public Object readProfiled(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile) {
            if (isSet(object, primitiveUsedMapProfile)) {
                return UnsafeUtils.getLongAt(object, address);
            } else {
                return NilObject.SINGLETON;
            }
        }

        private boolean isSet(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile) {
            return (primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object)) & usedMask) != 0;
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
        public boolean isSet(final AbstractPointersObject object) {
            return (getPrimitiveUsedMap(object) & usedMask) != 0;
        }

        @Override
        public void unset(final AbstractPointersObject object) {
            putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) & ~usedMask);
        }

        @Override
        public void writeProfiled(final AbstractPointersObject object, final Object value, final IntValueProfile primitiveUsedMapProfile) {
            if (value instanceof Long) {
                putPrimitiveUsedMap(object, primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object)) | usedMask);
                UnsafeUtils.putLongAt(object, address, (long) value);
            } else {
                throw IllegalWriteException.SINGLETON;
            }
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            CompilerAsserts.neverPartOfCompilation();
            if (value instanceof Long) {
                putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) | usedMask);
                UnsafeUtils.putLongAt(object, address, (long) value);
            } else {
                throw IllegalWriteException.SINGLETON;
            }
        }

        @Override
        public int getFieldIndex() {
            return ArrayUtils.indexOf(PRIMITIVE_ADDRESSES, address);
        }
    }

    private static final class LongExtensionSlotLocation extends LongLocation {
        private final int index;
        private final int usedMask;

        private LongExtensionSlotLocation(final int index) {
            this.index = index - NUM_PRIMITIVE_INLINE_LOCATIONS;
            usedMask = getPrimitiveUsedMask(index);
        }

        @Override
        public Object readProfiled(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile) {
            if (isSet(object, primitiveUsedMapProfile)) {
                return UnsafeUtils.getLong(object.primitiveExtension, index);
            } else {
                return NilObject.SINGLETON;
            }
        }

        private boolean isSet(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile) {
            return (primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object)) & usedMask) != 0;
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
        public boolean isSet(final AbstractPointersObject object) {
            return (getPrimitiveUsedMap(object) & usedMask) != 0;
        }

        @Override
        public void unset(final AbstractPointersObject object) {
            putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) & ~usedMask);
        }

        @Override
        public void writeProfiled(final AbstractPointersObject object, final Object value, final IntValueProfile primitiveUsedMapProfile) {
            if (value instanceof Long) {
                putPrimitiveUsedMap(object, primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object)) | usedMask);
                UnsafeUtils.putLong(object.primitiveExtension, index, (long) value);
            } else {
                throw IllegalWriteException.SINGLETON;
            }
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            CompilerAsserts.neverPartOfCompilation();
            if (value instanceof Long) {
                putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) | usedMask);
                UnsafeUtils.putLong(object.primitiveExtension, index, (long) value);
            } else {
                throw IllegalWriteException.SINGLETON;
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
        private final int usedMask;

        private DoubleInlineSlotLocation(final int index) {
            address = PRIMITIVE_ADDRESSES[index];
            usedMask = getPrimitiveUsedMask(index);
        }

        @Override
        public Object readProfiled(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile) {
            if (isSet(object, primitiveUsedMapProfile)) {
                return UnsafeUtils.getDoubleAt(object, address);
            } else {
                return NilObject.SINGLETON;
            }
        }

        private boolean isSet(final AbstractPointersObject object, final IntValueProfile primitiveUsedMapProfile) {
            return (primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object)) & usedMask) != 0;
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
        public boolean isSet(final AbstractPointersObject object) {
            return (getPrimitiveUsedMap(object) & usedMask) != 0;
        }

        @Override
        public void unset(final AbstractPointersObject object) {
            putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) & ~usedMask);
        }

        @Override
        public void writeProfiled(final AbstractPointersObject object, final Object value, final IntValueProfile primitiveUsedMapProfile) {
            if (value instanceof Double) {
                putPrimitiveUsedMap(object, primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object)) | usedMask);
                UnsafeUtils.putDoubleAt(object, address, (double) value);
            } else {
                throw IllegalWriteException.SINGLETON;
            }
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            CompilerAsserts.neverPartOfCompilation();
            if (value instanceof Double) {
                putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) | usedMask);
                UnsafeUtils.putDoubleAt(object, address, (double) value);
            } else {
                throw IllegalWriteException.SINGLETON;
            }
        }

        @Override
        public int getFieldIndex() {
            return ArrayUtils.indexOf(PRIMITIVE_ADDRESSES, address);
        }
    }

    private static final class DoubleExtensionSlotLocation extends DoubleLocation {
        private final int index;
        private final int usedMask;

        private DoubleExtensionSlotLocation(final int index) {
            this.index = index - NUM_PRIMITIVE_INLINE_LOCATIONS;
            usedMask = getPrimitiveUsedMask(index);
        }

        @Override
        public Object readProfiled(final AbstractPointersObject obj, final IntValueProfile primitiveUsedMapProfile) {
            if (isSet(obj, primitiveUsedMapProfile)) {
                return UnsafeUtils.getDoubleFromLongs(obj.primitiveExtension, index);
            } else {
                return NilObject.SINGLETON;
            }
        }

        private boolean isSet(final AbstractPointersObject obj, final IntValueProfile primitiveUsedMapProfile) {
            return (primitiveUsedMapProfile.profile(getPrimitiveUsedMap(obj)) & usedMask) != 0;
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
        public boolean isSet(final AbstractPointersObject obj) {
            return (getPrimitiveUsedMap(obj) & usedMask) != 0;
        }

        @Override
        public void unset(final AbstractPointersObject object) {
            putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) & ~usedMask);
        }

        @Override
        public void writeProfiled(final AbstractPointersObject object, final Object value, final IntValueProfile primitiveUsedMapProfile) {
            if (value instanceof Double) {
                putPrimitiveUsedMap(object, primitiveUsedMapProfile.profile(getPrimitiveUsedMap(object)) | usedMask);
                UnsafeUtils.putDoubleIntoLongs(object.primitiveExtension, index, (double) value);
            } else {
                throw IllegalWriteException.SINGLETON;
            }
        }

        @Override
        public void write(final AbstractPointersObject object, final Object value) {
            CompilerAsserts.neverPartOfCompilation();
            if (value instanceof Double) {
                putPrimitiveUsedMap(object, getPrimitiveUsedMap(object) | usedMask);
                UnsafeUtils.putDoubleIntoLongs(object.primitiveExtension, index, (double) value);
            } else {
                throw IllegalWriteException.SINGLETON;
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
            return UnsafeUtils.getObjectAt(object, OBJECT_ADDRESSES[index]) != null;
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
            return object.objectExtension != null && UnsafeUtils.getObject(object.objectExtension, index) != null;
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
