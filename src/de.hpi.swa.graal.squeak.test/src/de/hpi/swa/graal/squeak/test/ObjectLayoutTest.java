/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.hpi.swa.graal.squeak.image.SqueakImageChunk;
import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.SlotLocation;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectNewNode;

public class ObjectLayoutTest extends AbstractSqueakTestCaseWithDummyImage {
    @Test
    public void testBasic() {
        final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
        final AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.getUncached();

        final ClassObject dummyClass = createFreshTestClass();
        final PointersObject obj1 = instantiate(dummyClass);

        /* Ensure object is initialized correctly. */
        assertEquals(30, obj1.getNumSlots());
        assertUnsetPrimitiveFields(obj1);
        assertUnsetObjectFields(obj1);

        for (final SlotLocation location : obj1.getLayout().getLocations()) {
            assertTrue("All locations must be uninitialized", location.isUninitialized());
        }

        for (int i = 0; i < obj1.getNumSlots(); i++) {
            assertTrue("All reads should return nil", readNode.execute(obj1, i) == NilObject.SINGLETON);
        }

        /* Ensure nil writes do not change uninitialized locations. */
        for (int i = 0; i < obj1.getNumSlots(); i++) {
            writeNode.executeNil(obj1, i);
        }
        for (final SlotLocation location : obj1.getLayout().getLocations()) {
            assertTrue("All locations should be uninitialized", location.isUninitialized());
        }

        writeAndValidate(obj1, 0, 0L);
        assertTrue(obj1.primitive0 == 0L);

        final PointersObject obj2 = instantiate(dummyClass);
        assertTrue(obj1.getLayout() == obj2.getLayout());

        writeAndValidate(obj2, 0, 42L);
        assertTrue(obj2.primitive0 == 42L);
        assertTrue("Long write should not change layout", obj1.getLayout() == obj2.getLayout());

        writeAndValidate(obj1, 1, dummyClass);
        assertTrue("Write produces valid layout", obj1.getLayout().isValid());
        assertTrue("Write invalidates older layouts", !obj2.getLayout().isValid());
        assertTrue("Layouts should be out of sync", obj1.getLayout() != obj2.getLayout());
        assertTrue(obj1.object0 == dummyClass);

        assertTrue(readNode.execute(obj2, 1) == NilObject.SINGLETON);
        assertTrue("Layouts should be in sync after read", obj1.getLayout() == obj2.getLayout());

        writeAndValidate(obj2, 12, 1234L);
        assertTrue(obj2.primitive1 == 1234L);

        writeAndValidate(obj1, 12, image.bitmapClass);
        assertTrue((long) readNode.execute(obj2, 12) == 1234L);
        assertTrue("Primitive slot should be unset", obj2.primitive1 == 0L);
        assertTrue("Object slot should be used for primitive value", (long) obj2.object1 == 1234L);
    }

    @Test
    public void testTransitions() {
        final PointersObject obj = instantiate(createFreshTestClass());

        /* Fill with three different primitive values. */
        writeAndValidate(obj, 0, true);
        writeAndValidate(obj, 24, '&');
        writeAndValidate(obj, 12, 1234L);
        assertNull(obj.primitiveExtension);
        assertUnsetObjectFields(obj);

        /* Fill with another double primitive value. */
        writeAndValidate(obj, 16, 98.76D);
        assertEquals(1, obj.primitiveExtension.length);

        /* Fill all slots with double values. */
        for (int i = 0; i < obj.getNumSlots(); i++) {
            writeAndValidate(obj, i, (double) i + 1);
        }

        final int expectedGenericLocations = 3;
        assertEquals(obj.getNumSlots() - SlotLocation.NUM_PRIMITIVE_INLINE_LOCATIONS - expectedGenericLocations, obj.primitiveExtension.length);
        assertTrue(obj.object0 != NilObject.SINGLETON);
        assertTrue(obj.object1 != NilObject.SINGLETON);
        assertTrue(obj.object2 != NilObject.SINGLETON);
        assertNull(obj.objectExtension);

        writeAndValidate(obj, 26, '#');
        assertEquals(1, obj.objectExtension.length);
        assertEquals('#', obj.objectExtension[0]);
        writeAndValidate(obj, 22, false);
        assertEquals(2, obj.objectExtension.length);
        assertEquals(false, obj.objectExtension[1]);
    }

    @Test
    public void testFullObject() {
        final PointersObject obj = instantiate(createFreshTestClass());

        /* Fill entirely with primitive values. */
        for (int i = 0; i < obj.getNumSlots(); i++) {
            writeAndValidate(obj, i, (long) i + 42);
        }

        assertUnsetObjectFields(obj);

        for (final SlotLocation location : obj.getLayout().getLocations()) {
            assertTrue("All locations should be primitive", location.isPrimitive());
            assertTrue("All locations should be set", location.isSet(obj));
        }
        assertTrue(obj.primitive0 == 42L);
        assertTrue(obj.primitive1 == 43L);
        assertTrue(obj.primitive2 == 44L);
        assertEquals(obj.getNumSlots() - SlotLocation.NUM_PRIMITIVE_INLINE_LOCATIONS, obj.primitiveExtension.length);
        for (int i = 0; i < obj.primitiveExtension.length; i++) {
            assertTrue(obj.primitiveExtension[i] == SlotLocation.NUM_PRIMITIVE_INLINE_LOCATIONS + i + 42L);
        }

        /* Fill entirely with specialObjectsArray. */
        for (int i = 0; i < obj.getNumSlots(); i++) {
            writeAndValidate(obj, i, image.specialObjectsArray);
        }

        for (final SlotLocation location : obj.getLayout().getLocations()) {
            assertTrue("All locations should be generic", !location.isPrimitive() && !location.isUninitialized());
            assertTrue("All locations should be set", location.isSet(obj));
        }
        assertTrue(obj.object0 == image.specialObjectsArray);
        assertTrue(obj.object1 == image.specialObjectsArray);
        assertTrue(obj.object2 == image.specialObjectsArray);
        assertEquals(obj.getNumSlots() - SlotLocation.NUM_OBJECT_INLINE_LOCATIONS, obj.objectExtension.length);
        for (final Object object : obj.objectExtension) {
            assertTrue(object == image.specialObjectsArray);
        }
        assertUnsetPrimitiveFields(obj);
    }

    private static void assertUnsetPrimitiveFields(final AbstractPointersObject obj) {
        assertTrue(obj.primitive0 == 0L);
        assertTrue(obj.primitive1 == 0L);
        assertTrue(obj.primitive2 == 0L);
        assertNull(obj.primitiveExtension);
    }

    private static void assertUnsetObjectFields(final AbstractPointersObject obj) {
        assertTrue(obj.object0 == NilObject.SINGLETON);
        assertTrue(obj.object1 == NilObject.SINGLETON);
        assertTrue(obj.object2 == NilObject.SINGLETON);
        assertNull(obj.objectExtension);
    }

    private static ClassObject createFreshTestClass() {
        final ClassObject dummyClass = new ClassObject(image);
        final SqueakImageChunk dummyChunk = SqueakImageChunk.createDummyChunk(image, new Object[]{
                        image.nilClass.getSuperclass(), null,
                        // Format:
                        65542L /* `Morph format` */ | 24 /* + 24 slot = 30 slots in total. */,
                        null, null
        });
        dummyClass.fillin(dummyChunk);
        return dummyClass;
    }

    private static PointersObject instantiate(final ClassObject dummyClass) {
        return (PointersObject) SqueakObjectNewNode.getUncached().execute(image, dummyClass);
    }

    private static void writeAndValidate(final AbstractPointersObject obj, final int index, final Object value) {
        AbstractPointersObjectWriteNode.getUncached().execute(obj, index, value);
        assertTrue("Write failed", AbstractPointersObjectReadNode.getUncached().execute(obj, index).equals(value));
    }
}
