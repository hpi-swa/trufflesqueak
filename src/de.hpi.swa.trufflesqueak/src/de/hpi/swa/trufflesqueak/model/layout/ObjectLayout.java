/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model.layout;

import java.util.Arrays;
import java.util.BitSet;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Idempotent;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.SlotLocation.PrimitiveKind;
import de.hpi.swa.trufflesqueak.model.layout.SlotLocation.PrimitiveLocation;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class ObjectLayout {
    @CompilationFinal(dimensions = 1) private final SlotLocation[] locations;
    private final int numPrimitiveExtension;
    private final int numObjectExtension;

    private final Assumption isValidAssumption = Truffle.getRuntime().createAssumption("Latest layout assumption");

    public ObjectLayout(final int instSize) {
        locations = new SlotLocation[instSize];
        Arrays.fill(locations, SlotLocation.UNINITIALIZED_LOCATION);
        numPrimitiveExtension = 0;
        numObjectExtension = 0;
    }

    private ObjectLayout(final SlotLocation[] locations) {
        this.locations = locations;
        numPrimitiveExtension = countPrimitiveExtension(locations);
        numObjectExtension = countObjectExtension(locations);
    }

    public ObjectLayout evolveLocation(final ClassObject squeakClass, final long longIndex, final Object value) {
        CompilerAsserts.neverPartOfCompilation("Should only happen on slow path");
        final int index = Math.toIntExact(longIndex);
        if (!isValid()) {
            throw SqueakException.create("Only the latest layout should be evolved");
        }
        final SlotLocation oldLocation = locations[index];
        assert !oldLocation.isGeneric();
        invalidate();
        final SlotLocation[] newLocations = locations.clone();
        newLocations[index] = SlotLocation.UNINITIALIZED_LOCATION;
        PrimitiveKind newKind = null;
        if (oldLocation.isUninitialized()) {
            newKind = switch (value) {
                case Boolean _ -> PrimitiveKind.BOOL;
                case Character _ -> PrimitiveKind.CHAR;
                case Long v -> v == (int) (long) v ? PrimitiveKind.INT : PrimitiveKind.LONG;
                case Double _ -> PrimitiveKind.DOUBLE;
                case null, default -> null;
            };
        } else if (oldLocation instanceof final PrimitiveLocation primitive && primitive.getKind() == PrimitiveKind.INT && value instanceof Long) {
            // Widen integer storage permanently instead of boxing an out-of-range value.
            newKind = PrimitiveKind.LONG;
        }
        if (newKind != null && countPrimitiveLocations(newLocations) < SlotLocation.MAX_PRIMITIVE_LOCATIONS) {
            packPrimitives(newLocations, index, newKind);
        } else {
            assignGenericLocation(newLocations, index);
            packPrimitives(newLocations, -1, null);
        }

        assert !newLocations[index].isUninitialized();
        assert slotLocationsAreConsecutive(newLocations) : "Locations overlap or contain gaps";
        final ObjectLayout newLayout = new ObjectLayout(newLocations);
        squeakClass.updateLayout(newLayout);
        return newLayout;
    }

    private static void assignGenericLocation(final SlotLocation[] newLocations, final int index) {
        for (final SlotLocation possibleLocation : SlotLocation.OBJECT_LOCATIONS.getValues()) {
            if (!inUse(newLocations, possibleLocation)) {
                newLocations[index] = possibleLocation;
                return;
            }
        }
        newLocations[index] = SlotLocation.getObjectLocation(SlotLocation.OBJECT_LOCATIONS.size());
    }

    private static int countPrimitiveLocations(final SlotLocation[] locations) {
        int count = 0;
        for (final SlotLocation location : locations) {
            if (location.isPrimitive()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Repack on the slow path, widest locations first. Sizes are powers of two, so this produces
     * aligned locations without holes, and no location crosses a primitive word boundary.
     */
    private static void packPrimitives(final SlotLocation[] locations, final int changedIndex, final PrimitiveKind newKind) {
        int byteIndex = 0;
        int usedIndex = 0;
        for (int byteSize = Long.BYTES; byteSize > 0; byteSize /= 2) {
            for (int i = 0; i < locations.length; i++) {
                final PrimitiveKind kind;
                if (i == changedIndex) {
                    kind = newKind;
                } else if (locations[i] instanceof final PrimitiveLocation primitive) {
                    kind = primitive.getKind();
                } else {
                    continue;
                }
                if (kind.getByteSize() == byteSize) {
                    locations[i] = new PrimitiveLocation(kind, byteIndex, usedIndex++);
                    byteIndex += byteSize;
                }
            }
        }
    }

    private static boolean slotLocationsAreConsecutive(final SlotLocation[] locations) {
        CompilerAsserts.neverPartOfCompilation();
        final BitSet primitiveBytes = new BitSet();
        final BitSet objectFields = new BitSet();
        for (final SlotLocation location : locations) {
            if (location instanceof final PrimitiveLocation primitive) {
                final int byteIndex = primitive.getByteIndex();
                final int byteSize = primitive.getKind().getByteSize();
                final int nextUsedByte = primitiveBytes.nextSetBit(byteIndex);
                if (byteIndex % byteSize != 0 || nextUsedByte >= 0 && nextUsedByte < byteIndex + byteSize) {
                    return false;
                }
                primitiveBytes.set(byteIndex, byteIndex + byteSize);
            } else if (location.isGeneric()) {
                final int fieldIndex = location.getFieldIndex();
                if (objectFields.get(fieldIndex)) {
                    return false;
                }
                objectFields.set(fieldIndex);
            }
        }
        return primitiveBytes.nextClearBit(0) == primitiveBytes.length() && objectFields.nextClearBit(0) == objectFields.length();
    }

    private static boolean inUse(final SlotLocation[] locations, final SlotLocation targetlocation) {
        for (final SlotLocation location : locations) {
            if (location.isGeneric() == targetlocation.isGeneric() && location.getFieldIndex() == targetlocation.getFieldIndex()) {
                return true;
            }
        }
        return false;
    }

    public Assumption getValidAssumption() {
        return isValidAssumption;
    }

    public void invalidate() {
        isValidAssumption.invalidate("Layout no longer valid");
    }

    public boolean isValid() {
        return isValidAssumption.isValid();
    }

    public SlotLocation getLocation(final long index) {
        return (SlotLocation) UnsafeUtils.getObject(locations, index);
    }

    public SlotLocation[] getLocations() {
        return locations;
    }

    @Idempotent
    public int getInstSize() {
        return locations.length;
    }

    public int getNumPrimitiveExtension() {
        return numPrimitiveExtension;
    }

    public int getNumObjectExtension() {
        return numObjectExtension;
    }

    public long[] getFreshPrimitiveExtension() {
        final int primitiveExtUsed = getNumPrimitiveExtension();
        return primitiveExtUsed > 0 ? new long[primitiveExtUsed] : null;
    }

    public Object[] getFreshObjectExtension() {
        final int objectExtUsed = getNumObjectExtension();
        return objectExtUsed > 0 ? ArrayUtils.withAll(objectExtUsed, NilObject.SINGLETON) : null;
    }

    private static int countPrimitiveExtension(final SlotLocation[] locations) {
        int count = 0;
        for (final SlotLocation location : locations) {
            if (location.isPrimitive() && location.isExtension()) {
                count = Math.max(count, location.getFieldIndex() - SlotLocation.NUM_PRIMITIVE_INLINE_LOCATIONS + 1);
            }
        }
        return count;
    }

    private static int countObjectExtension(final SlotLocation[] locations) {
        int count = 0;
        for (final SlotLocation location : locations) {
            if (location.isGeneric() && location.isExtension()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return (isValid() ? "valid" : "invalid") + " ObjectLayout @" + Integer.toHexString(hashCode());
    }
}
