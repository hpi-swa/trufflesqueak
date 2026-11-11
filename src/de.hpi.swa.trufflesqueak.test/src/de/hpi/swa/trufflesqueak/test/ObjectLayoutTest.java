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

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;
import de.hpi.swa.trufflesqueak.model.layout.SlotLocation;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodesFactory.AbstractPointersObjectReadNodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodesFactory.AbstractPointersObjectWriteNodeGen;
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
        assertEquals(42L, (long) readNode.execute(obj2, 0));
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
        assertEquals(1234L, (long) readNode.execute(obj2, 12));
        assertNull(obj2.primitiveExtension);

        writeAndValidate(obj1, 12, image.bitmapClass);
        assertFalse("obj2 should have outdated layout", obj2.getLayout().isValid());
        assertEquals(1234L, (long) readNode.execute(obj2, 12));
        writeAndValidate(obj2, 11, image.bitmapClass);
        assertTrue("obj2 should have valid layout after write", obj2.getLayout().isValid());
        assertEquals("Neighbor survives migration", 42L, (long) readNode.execute(obj2, 0));
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
        assertNull(obj.primitiveExtension);

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

        /* Fill entirely with full-width primitive values. */
        final long firstValue = (long) Integer.MAX_VALUE + 42;
        for (int i = 0; i < obj.getNumSlots(); i++) {
            writeAndValidate(obj, i, firstValue + i);
        }

        assertUnsetObjectFields(obj);

        for (final SlotLocation location : obj.getLayout().getLocations()) {
            assertTrue("All locations should be primitive", location.isPrimitive());
            assertTrue("All locations should be set", location.isSet(obj));
        }
        assertEquals(firstValue, obj.primitive0);
        assertEquals(firstValue + 1, obj.primitive1);
        assertEquals(firstValue + 2, obj.primitive2);
        assertEquals(obj.getNumSlots() - SlotLocation.NUM_PRIMITIVE_INLINE_LOCATIONS, obj.primitiveExtension.length);
        for (int i = 0; i < obj.primitiveExtension.length; i++) {
            assertEquals(obj.primitiveExtension[i], SlotLocation.NUM_PRIMITIVE_INLINE_LOCATIONS + i + firstValue);
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

    @Test
    public void testPackedCapacity() {
        for (final Object value : new Object[]{true, '\uffff', 42L}) {
            final PointersObject obj = instantiate(createFreshTestClass());
            final int byteSize = value instanceof Boolean ? Byte.BYTES : value instanceof Character ? Character.BYTES : Integer.BYTES;
            for (int i = 0; i < obj.getNumSlots(); i++) {
                writeAndValidate(obj, i, value);
            }
            final int expectedExtension = Math.max(0, (obj.getNumSlots() * byteSize + Long.BYTES - 1) / Long.BYTES - SlotLocation.NUM_PRIMITIVE_INLINE_LOCATIONS);
            assertEquals(expectedExtension, obj.getLayout().getNumPrimitiveExtension());
            assertEquals(expectedExtension, obj.primitiveExtension == null ? 0 : obj.primitiveExtension.length);
            for (int i = 0; i < obj.getNumSlots(); i++) {
                assertEquals(value, AbstractPointersObjectReadNode.executeUncached(obj, i));
            }
        }
    }

    @Test
    public void testIndependentPresenceAndUnset() {
        for (final Object value : new Object[]{false, '\0', 0L}) {
            final ClassObject cls = createFreshTestClass();
            final PointersObject initialized = instantiate(cls);
            for (int i = 0; i < initialized.getNumSlots(); i++) {
                writeAndValidate(initialized, i, value);
            }
            final PointersObject obj = instantiate(cls);
            for (int i = 0; i < obj.getNumSlots(); i += 2) {
                writeAndValidate(obj, i, value);
            }
            for (int i = 0; i < obj.getNumSlots(); i++) {
                assertEquals(i % 2 == 0 ? value : NilObject.SINGLETON, AbstractPointersObjectReadNode.executeUncached(obj, i));
            }
            // Clearing a packed location must preserve both neighbors and their presence bits.
            for (int i = 2; i < obj.getNumSlots(); i += 4) {
                obj.getLayout().getLocation(i).unset(obj);
            }
            for (int i = 0; i < obj.getNumSlots(); i++) {
                assertEquals(i % 4 == 0 ? value : NilObject.SINGLETON, AbstractPointersObjectReadNode.executeUncached(obj, i));
            }
        }
    }

    @Test
    public void testIntegerWidening() {
        for (final long wide : new long[]{(long) Integer.MAX_VALUE + 1, (long) Integer.MIN_VALUE - 1, Long.MIN_VALUE, Long.MAX_VALUE}) {
            final ClassObject cls = createFreshTestClass();
            final PointersObject obj = instantiate(cls);
            writeAndValidate(obj, 0, (long) Integer.MIN_VALUE);
            writeAndValidate(obj, 1, (long) Integer.MAX_VALUE);
            final PointersObject other = instantiate(cls);
            writeAndValidate(other, 0, -1L);
            writeAndValidate(other, 1, 0L);
            writeAndValidate(obj, 0, wide);
            assertTrue(obj.getLayout().getLocation(0).isLong());
            assertEquals((long) Integer.MAX_VALUE, AbstractPointersObjectReadNode.executeUncached(obj, 1));
            // Migrate an older object that still has two ints sharing one word.
            writeAndValidate(other, 1, 1L);
            assertEquals(-1L, AbstractPointersObjectReadNode.executeUncached(other, 0));
            final ObjectLayout wideLayout = obj.getLayout();
            writeAndValidate(obj, 0, 1L);
            assertSame("Wide integer locations never narrow", wideLayout, obj.getLayout());
            writeAndValidate(obj, 0, NilObject.SINGLETON);
            assertTrue(obj.getLayout().getLocation(0).isGeneric());
            assertEquals((long) Integer.MAX_VALUE, AbstractPointersObjectReadNode.executeUncached(obj, 1));
        }
    }

    @Test
    public void testPrimitiveLimit() {
        final ClassObject cls = createFreshTestClass(41);
        final PointersObject obj = instantiate(cls);
        for (int i = 0; i < 40; i++) {
            writeAndValidate(obj, i, true);
        }
        for (int i = 0; i < 40; i++) {
            assertEquals(i < SlotLocation.MAX_PRIMITIVE_LOCATIONS, obj.getLayout().getLocation(i).isPrimitive());
            assertEquals(true, AbstractPointersObjectReadNode.executeUncached(obj, i));
        }
        // Exercise the sign bit of the presence map, independently from the other packed values.
        obj.getLayout().getLocation(31).unset(obj);
        assertSame(NilObject.SINGLETON, AbstractPointersObjectReadNode.executeUncached(obj, 31));
        assertEquals(true, AbstractPointersObjectReadNode.executeUncached(obj, 30));
        writeAndValidate(obj, 31, false);
        // Releasing one primitive location makes its presence capacity available again.
        writeAndValidate(obj, 0, cls);
        writeAndValidate(obj, 40, -1L);
        assertTrue(obj.getLayout().getLocation(40).isPrimitive());
        final PointersObject other = instantiate(cls);
        assertSame(NilObject.SINGLETON, AbstractPointersObjectReadNode.executeUncached(other, 31));
    }

    @Test
    public void testMixedLayoutEvolution() {
        final ClassObject cls = createFreshTestClass();
        final PointersObject[] objects = new PointersObject[4];
        final Object[][] expected = new Object[objects.length][30];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = instantiate(cls);
            Arrays.fill(expected[i], NilObject.SINGLETON);
        }
        final Random random = new Random(42);
        final Object[] values = {false, true, '\0', '\uffff', -1L, 0L, (long) Integer.MIN_VALUE, (long) Integer.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE,
                        -0.0D, Double.NaN, Double.POSITIVE_INFINITY, cls, NilObject.SINGLETON};
        for (int step = 0; step < 400; step++) {
            final int receiver = random.nextInt(objects.length);
            final int slot = random.nextInt(30);
            final Object value = step < 120 ? values[slot % 10] : values[random.nextInt(values.length)];
            writeAndValidate(objects[receiver], slot, value);
            expected[receiver][slot] = value;
            for (int i = 0; i < objects.length; i++) {
                for (int j = 0; j < 30; j++) {
                    assertEquals("Value changed during layout evolution", expected[i][j], AbstractPointersObjectReadNode.executeUncached(objects[i], j));
                }
            }
        }
    }

    @Test
    public void testCachedPackedAccess() {
        final PointersObject obj = instantiate(createFreshTestClass());
        final Object[] values = {true, '\uffff', -1L, Long.MAX_VALUE, -0.0D};
        final AbstractPointersObjectReadNode[] readers = new AbstractPointersObjectReadNode[30];
        final AbstractPointersObjectWriteNode[] writers = new AbstractPointersObjectWriteNode[30];
        for (int i = 0; i < 30; i++) {
            readers[i] = AbstractPointersObjectReadNodeGen.create();
            writers[i] = AbstractPointersObjectWriteNodeGen.create();
            writeAndValidate(obj, i, values[i % values.length]);
        }
        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < 30; i++) {
                writers[i].execute(obj, i, values[i % values.length]);
                assertEquals(values[i % values.length], readers[i].execute(obj, i));
            }
        }
        // Invalidate cached offsets by widening one int and generalizing one boolean.
        writers[2].execute(obj, 2, Long.MIN_VALUE);
        writers[0].execute(obj, 0, NilObject.SINGLETON);
        for (int i = 0; i < 30; i++) {
            assertEquals(i == 0 ? NilObject.SINGLETON : i == 2 ? Long.MIN_VALUE : values[i % values.length], readers[i].execute(obj, i));
        }
    }

    @Test
    public void testPackedCopyAndClassChange() {
        final PointersObject obj = instantiate(createFreshTestClass());
        final Object[] values = {true, '\uffff', -1L, Long.MAX_VALUE, Double.longBitsToDouble(0x7ff8000000000001L)};
        for (int i = 0; i < 30; i++) {
            writeAndValidate(obj, i, values[i % values.length]);
        }
        final PointersObject copy = new PointersObject(obj);
        final PointersObject copiedValues = instantiate(obj.getSqueakClass());
        copiedValues.copyLayoutValuesFrom(obj);
        final ClassObject targetClass = createFreshTestClass();
        final PointersObject target = instantiate(targetClass);
        for (int i = 0; i < 30; i++) {
            writeAndValidate(target, i, 0L);
        }
        // Migration must widen target int locations for old full-width integer values.
        obj.changeClassTo(targetClass);
        for (final PointersObject receiver : new PointersObject[]{obj, copy, copiedValues}) {
            for (int i = 0; i < 30; i++) {
                final Object actual = AbstractPointersObjectReadNode.executeUncached(receiver, i);
                assertEquals(values[i % values.length], actual);
                if (actual instanceof final Double v) {
                    assertEquals(0x7ff8000000000001L, Double.doubleToRawLongBits(v));
                }
            }
        }
        writeAndValidate(obj, 0, false);
        assertEquals(true, AbstractPointersObjectReadNode.executeUncached(copy, 0));
        assertEquals(true, AbstractPointersObjectReadNode.executeUncached(copiedValues, 0));
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
        return createFreshTestClass(30);
    }

    private static ClassObject createFreshTestClass(final int numSlots) {
        final ClassObject dummyClass = new ClassObject(image);
        dummyClass.setFormat(65536L | numSlots);
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
