/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.SlotLocation;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectNewNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;

@SuppressWarnings("static-method")
public final class ObjectLayoutTest extends AbstractSqueakTestCaseWithDummyImage {
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
            assertSame("All reads should return nil", NilObject.SINGLETON, readNode.execute(obj1, i));
        }

        /* Ensure nil writes do not change uninitialized locations. */
        for (int i = 0; i < obj1.getNumSlots(); i++) {
            writeNode.executeNil(obj1, i);
        }
        for (final SlotLocation location : obj1.getLayout().getLocations()) {
            assertTrue("All locations should be uninitialized", location.isUninitialized());
        }

        writeAndValidate(obj1, 0, 0L);
        assertEquals(0L, obj1.primitive0);

        final PointersObject obj2 = instantiate(dummyClass);
        assertSame(obj1.getLayout(), obj2.getLayout());

        writeAndValidate(obj2, 0, 42L);
        assertEquals(42L, obj2.primitive0);
        assertSame("Long write should not change layout", obj1.getLayout(), obj2.getLayout());

        writeAndValidate(obj1, 1, dummyClass);
        assertTrue("Write produces valid layout", obj1.getLayout().isValid());
        assertFalse("Write invalidates older layouts", obj2.getLayout().isValid());
        assertNotSame("Layouts should be out of sync", obj1.getLayout(), obj2.getLayout());
        assertSame(obj1.object0, dummyClass);

        assertSame(NilObject.SINGLETON, readNode.execute(obj2, 1));
        assertFalse("Read does not update layout", obj2.getLayout().isValid());
        assertNotSame("Layouts should still be out of sync after read", obj1.getLayout(), obj2.getLayout());

        writeAndValidate(obj2, 12, 1234L);
        assertEquals(1234L, obj2.primitive1);

        writeAndValidate(obj1, 12, image.bitmapClass);
        assertFalse("obj2 should have outdated layout", obj2.getLayout().isValid());
        assertEquals(1234L, (long) readNode.execute(obj2, 12));
        writeAndValidate(obj2, 11, image.bitmapClass);
        assertTrue("obj2 should have valid layout after write", obj2.getLayout().isValid());
        assertEquals("Primitive slot should be unset", 0L, obj2.primitive1);
        assertEquals("Object slot should be used for primitive value", 1234L, (long) obj2.object1);
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
        assertNotSame(NilObject.SINGLETON, obj.object0);
        assertNotSame(NilObject.SINGLETON, obj.object1);
        assertNotSame(NilObject.SINGLETON, obj.object2);
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
        assertEquals(42L, obj.primitive0);
        assertEquals(43L, obj.primitive1);
        assertEquals(44L, obj.primitive2);
        assertEquals(obj.getNumSlots() - SlotLocation.NUM_PRIMITIVE_INLINE_LOCATIONS, obj.primitiveExtension.length);
        for (int i = 0; i < obj.primitiveExtension.length; i++) {
            assertEquals(obj.primitiveExtension[i], SlotLocation.NUM_PRIMITIVE_INLINE_LOCATIONS + i + 42L);
        }

        /* Fill entirely with specialObjectsArray. */
        for (int i = 0; i < obj.getNumSlots(); i++) {
            writeAndValidate(obj, i, image.specialObjectsArray);
        }

        for (final SlotLocation location : obj.getLayout().getLocations()) {
            assertTrue("All locations should be generic", !location.isPrimitive() && !location.isUninitialized());
            assertTrue("All locations should be set", location.isSet(obj));
        }
        assertSame(obj.object0, image.specialObjectsArray);
        assertSame(obj.object1, image.specialObjectsArray);
        assertSame(obj.object2, image.specialObjectsArray);
        assertEquals(obj.getNumSlots() - SlotLocation.NUM_OBJECT_INLINE_LOCATIONS, obj.objectExtension.length);
        for (final Object object : obj.objectExtension) {
            assertSame(object, image.specialObjectsArray);
        }
        assertUnsetPrimitiveFields(obj);
    }

    private static void assertUnsetPrimitiveFields(final AbstractPointersObject obj) {
        assertEquals(0L, obj.primitive0);
        assertEquals(0L, obj.primitive1);
        assertEquals(0L, obj.primitive2);
        assertNull(obj.primitiveExtension);
    }

    private static void assertUnsetObjectFields(final AbstractPointersObject obj) {
        assertSame(NilObject.SINGLETON, obj.object0);
        assertSame(NilObject.SINGLETON, obj.object1);
        assertSame(NilObject.SINGLETON, obj.object2);
        assertNull(obj.objectExtension);
    }

    private static ClassObject createFreshTestClass() {
        final ClassObject dummyClass = new ClassObject(image);
        dummyClass.setFormat(65542L /* `Morph format` */ | 24 /* + 24 slot = 30 slots in total. */);
        dummyClass.setOtherPointers(ArrayUtils.EMPTY_ARRAY);
        return dummyClass;
    }

    private static PointersObject instantiate(final ClassObject dummyClass) {
        return (PointersObject) SqueakObjectNewNode.executeUncached(dummyClass);
    }

    private static void writeAndValidate(final AbstractPointersObject obj, final int index, final Object value) {
        AbstractPointersObjectWriteNode.executeUncached(obj, index, value);
        assertEquals("Write failed", AbstractPointersObjectReadNode.executeUncached(obj, index), value);
    }
}
