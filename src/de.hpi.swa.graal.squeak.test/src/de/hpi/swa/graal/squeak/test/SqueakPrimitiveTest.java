package de.hpi.swa.graal.squeak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.WrapToSqueakNode;
import de.hpi.swa.graal.squeak.util.BigInt;

public class SqueakPrimitiveTest extends AbstractSqueakTestCaseWithDummyImage {
    @Test
    public void testPrimEquivalent() {
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        assertTrue((boolean) runBinaryPrimitive(110, rcvr, rcvr));
        assertFalse((boolean) runBinaryPrimitive(110, rcvr, image.nil));
    }

    @Test
    public void testPrimReplaceFromTo() {
        final PointersObject rcvr = new PointersObject(image, image.arrayClass,
                        new Object[]{image.nil, image.sqFalse, image.sqTrue, image.characterClass, image.metaClass,
                                        image.schedulerAssociation, image.smallIntegerClass, image.smalltalk,
                                        image.specialObjectsArray});
        assertSame(image.nil, rcvr.at0(0));
        for (int i = 1; i < 8; i++) {
            assertNotSame(image.nil, rcvr.at0(i));
        }
        final Object result = runQuinaryPrimitive(105, rcvr, 1L, 6L, new PointersObject(image, image.nilClass, 10), 1L);
        assertSame(result, rcvr);
        for (int i = 0; i < 6; i++) {
            assertSame(image.nil, rcvr.at0(i));
        }
        for (int i = 7; i < 8; i++) {
            assertNotSame(image.nil, rcvr.at0(i));
        }
    }

    @Test
    public void testAdd() {
        final BigInt maxtimestwo = new BigInt(Long.MAX_VALUE);
        maxtimestwo.mul(2);
        final BigInt maxplusmin = new BigInt(Long.MAX_VALUE);
        maxplusmin.add(new BigInt(Long.MIN_VALUE));
        final Object[][] testValues = new Object[][]{
                        {(long) Integer.MAX_VALUE, (long) Integer.MAX_VALUE, 2 * (long) Integer.MAX_VALUE},
                        {Long.MAX_VALUE, Long.MAX_VALUE, maxtimestwo},
                        {Long.MAX_VALUE, Long.MIN_VALUE, maxplusmin.longValue()}};
        final WrapToSqueakNode wrapNode = WrapToSqueakNode.create(image);
        for (int i = 0; i < testValues.length; i++) {
            final Object[] values = testValues[i];
            assertEquals(wrapNode.executeWrap(values[2]), runBinaryPrimitive(21, wrapNode.executeWrap(values[0]), wrapNode.executeWrap(values[1])));
        }
    }

    @Test
    public void testSub() {
        final BigInt minoverflow = new BigInt(Long.MIN_VALUE);
        minoverflow.sub(1);
        final BigInt maxoverflow = new BigInt(Long.MAX_VALUE);
        maxoverflow.add(1);
        final Object[][] testValues = new Object[][]{
                        {(long) Integer.MAX_VALUE, (long) Integer.MAX_VALUE, 0L},
                        {Long.MAX_VALUE, Long.MAX_VALUE, 0L},
                        {Long.MAX_VALUE, Long.MAX_VALUE - 1, 1L},
                        {Long.MAX_VALUE, Long.MAX_VALUE - Integer.MAX_VALUE, (long) Integer.MAX_VALUE},
                        {Long.MIN_VALUE, 1L, minoverflow},
                        {Long.MAX_VALUE, Long.MAX_VALUE - Integer.MAX_VALUE, (long) Integer.MAX_VALUE},
                        {maxoverflow, 1L, Long.MAX_VALUE}};
        final WrapToSqueakNode wrapNode = WrapToSqueakNode.create(image);
        for (int i = 0; i < testValues.length; i++) {
            final Object[] values = testValues[i];
            assertEquals(wrapNode.executeWrap(values[2]), runBinaryPrimitive(22, wrapNode.executeWrap(values[0]), wrapNode.executeWrap(values[1])));
        }
    }
}
