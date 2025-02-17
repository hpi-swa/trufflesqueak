/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model.layout;

import java.util.Arrays;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Idempotent;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class ObjectLayout {
    private final ClassObject squeakClass;
    @CompilationFinal(dimensions = 1) private final SlotLocation[] locations;
    private final int numPrimitiveExtension;
    private final int numObjectExtension;

    private final Assumption isValidAssumption = Truffle.getRuntime().createAssumption("Latest layout assumption");

    public ObjectLayout(final ClassObject classObject, final int instSize) {
        slowPathOperation();
        squeakClass = classObject;
        classObject.updateLayout(this);
        locations = new SlotLocation[instSize];
        Arrays.fill(locations, SlotLocation.UNINITIALIZED_LOCATION);
        numPrimitiveExtension = 0;
        numObjectExtension = 0;
    }

    public ObjectLayout(final ClassObject classObject, final SlotLocation[] locations) {
        slowPathOperation();
        squeakClass = classObject;
        classObject.updateLayout(this);
        this.locations = locations;
        numPrimitiveExtension = countPrimitiveExtension(locations);
        numObjectExtension = countObjectExtension(locations);
    }

    public ObjectLayout evolveLocation(final long longIndex, final Object value) {
        slowPathOperation();
        final int index = Math.toIntExact(longIndex);
        if (!isValid()) {
            throw SqueakException.create("Only the latest layout should be evolved");
        }
        final SlotLocation oldLocation = locations[index];
        assert !oldLocation.isGeneric();
        invalidate();
        final SlotLocation[] newLocations = locations.clone();
        newLocations[index] = SlotLocation.UNINITIALIZED_LOCATION;
        if (oldLocation.isUninitialized()) {
            if (value instanceof Boolean) {
                assignPrimitiveLocation(newLocations, index, SlotLocation.BOOL_LOCATIONS);
            } else if (value instanceof Character) {
                assignPrimitiveLocation(newLocations, index, SlotLocation.CHAR_LOCATIONS);
            } else if (value instanceof Long) {
                assignPrimitiveLocation(newLocations, index, SlotLocation.LONG_LOCATIONS);
            } else if (value instanceof Double) {
                assignPrimitiveLocation(newLocations, index, SlotLocation.DOUBLE_LOCATIONS);
            } else {
                assignGenericLocation(newLocations, index);
            }
        } else {
            assignGenericLocation(newLocations, index);
        }

        if (oldLocation.isPrimitive()) {
            assert newLocations[index].isGeneric();
            compressPrimitivesIfPossible(newLocations, oldLocation);
        }

        assert !newLocations[index].isUninitialized();
        assert slotLocationsAreConsecutive(newLocations) : "Locations are not consecutive";
        return new ObjectLayout(squeakClass, newLocations);
    }

    private static void slowPathOperation() {
        CompilerAsserts.neverPartOfCompilation("Should only happen on slow path");
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

    private static void assignPrimitiveLocation(final SlotLocation[] newLocations, final int index, final SlotLocation[] possibleLocations) {
        for (final SlotLocation possibleLocation : possibleLocations) {
            if (!inUse(newLocations, possibleLocation)) {
                newLocations[index] = possibleLocation;
                return;
            }
        }
        // Primitive locations exhausted, fall back to a generic location.
        assignGenericLocation(newLocations, index);
    }

    private static void compressPrimitivesIfPossible(final SlotLocation[] locations, final SlotLocation freePrimitiveLocation) {
        final int highestPrimitiveField = getHighestPrimitiveField(locations);
        if (highestPrimitiveField < freePrimitiveLocation.getFieldIndex()) {
            return;
        }
        for (int i = 0; i < locations.length; i++) {
            final SlotLocation location = locations[i];
            if (location.isPrimitive() && location.getFieldIndex() == highestPrimitiveField) {
                if (location.isBool()) {
                    locations[i] = SlotLocation.BOOL_LOCATIONS[freePrimitiveLocation.getFieldIndex()];
                } else if (location.isChar()) {
                    locations[i] = SlotLocation.CHAR_LOCATIONS[freePrimitiveLocation.getFieldIndex()];
                } else if (location.isLong()) {
                    locations[i] = SlotLocation.LONG_LOCATIONS[freePrimitiveLocation.getFieldIndex()];
                } else if (location.isDouble()) {
                    locations[i] = SlotLocation.DOUBLE_LOCATIONS[freePrimitiveLocation.getFieldIndex()];
                } else {
                    throw SqueakException.create("Unexpected location type");
                }
            }
        }
    }

    private static int getHighestPrimitiveField(final SlotLocation[] locations) {
        int maxPrimitiveField = -1;
        for (final SlotLocation location : locations) {
            if (location.isPrimitive()) {
                maxPrimitiveField = Math.max(location.getFieldIndex(), maxPrimitiveField);
            }
        }
        return maxPrimitiveField;
    }

    private static int getHighestObjectField(final SlotLocation[] locations) {
        int maxPrimitiveField = -1;
        for (final SlotLocation location : locations) {
            if (location.isGeneric()) {
                maxPrimitiveField = Math.max(location.getFieldIndex(), maxPrimitiveField);
            }
        }
        return maxPrimitiveField;
    }

    private static boolean slotLocationsAreConsecutive(final SlotLocation[] locations) {
        CompilerAsserts.neverPartOfCompilation();
        final int maxPrimitiveField = getHighestPrimitiveField(locations);
        final int maxObjectField = getHighestObjectField(locations);
        for (int i = 0; i <= maxPrimitiveField; i++) {
            boolean found = false;
            for (final SlotLocation location : locations) {
                if (location.isPrimitive() && location.getFieldIndex() == i) {
                    if (found) {
                        return false;
                    } else {
                        found = true;
                    }
                }
            }
            if (!found) {
                return false;
            }
        }
        for (int i = 0; i <= maxObjectField; i++) {
            boolean found = false;
            for (final SlotLocation location : locations) {
                if (location.isGeneric() && location.getFieldIndex() == i) {
                    if (found) {
                        return false;
                    } else {
                        found = true;
                    }
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
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

    public ClassObject getSqueakClass() {
        return squeakClass;
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
                count++;
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
        return (isValid() ? "valid" : "invalid") + " ObjectLayout for " + squeakClass + " @" + Integer.toHexString(hashCode());
    }
}
