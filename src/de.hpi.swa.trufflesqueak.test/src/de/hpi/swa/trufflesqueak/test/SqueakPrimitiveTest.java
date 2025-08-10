/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.interop.WrapToSqueakNode;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.plugins.LargeIntegers;

@SuppressWarnings("static-method")
public final class SqueakPrimitiveTest extends AbstractSqueakTestCaseWithDummyImage {
    @Test
    public void testPrimEquivalent() {
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        assertTrue((boolean) runPrimitive(110, rcvr, rcvr));
        assertFalse((boolean) runPrimitive(110, rcvr, NilObject.SINGLETON));
    }

    @Test
    public void testPrimReplaceFromTo() {
        final ArrayObject rcvr = image.asArrayOfObjects(NilObject.SINGLETON, BooleanObject.FALSE, BooleanObject.TRUE, image.characterClass, image.metaClass,
                        image.schedulerAssociation, image.smallIntegerClass, image.smalltalk,
                        image.specialObjectsArray);
        assertSame(NilObject.SINGLETON, rcvr.getObject(0));
        for (int i = 1; i < 8; i++) {
            assertNotSame(NilObject.SINGLETON, rcvr.getObject(i));
        }
        final Object result = runPrimitive(105, rcvr, 1L, 6L, ArrayObject.createEmptyStrategy(image, image.arrayClass, 10), 1L);
        assertSame(result, rcvr);
        for (int i = 0; i < 6; i++) {
            assertSame(NilObject.SINGLETON, rcvr.getObject(i));
        }
        for (int i = 7; i < 8; i++) {
            assertNotSame(NilObject.SINGLETON, rcvr.getObject(i));
        }
    }

    @Test
    public void testAdd() {
        final Object[][] testValues = new Object[][]{
                        {(long) Integer.MAX_VALUE, (long) Integer.MAX_VALUE, 2 * (long) Integer.MAX_VALUE},
                        {Long.MAX_VALUE, Long.MAX_VALUE, LargeIntegers.toNativeObject(image, BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(2)))},
                        {Long.MAX_VALUE, Long.MIN_VALUE, BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(Long.MIN_VALUE)).longValue()}};
        for (final Object[] values : testValues) {
            final Object wrappedValue0 = WrapToSqueakNode.executeUncached(values[0]);
            final Object wrappedValue1 = WrapToSqueakNode.executeUncached(values[1]);
            final Object wrappedValue2 = WrapToSqueakNode.executeUncached(values[2]);
            assertEquals(wrappedValue2, runPrimitive(1, wrappedValue0, wrappedValue1));
        }
    }

    @Test
    public void testSub() {
        final Object[][] testValues = new Object[][]{
                        {(long) Integer.MAX_VALUE, (long) Integer.MAX_VALUE, 0L},
                        {Long.MAX_VALUE, Long.MAX_VALUE, 0L},
                        {Long.MAX_VALUE, Long.MAX_VALUE - 1, 1L},
                        {Long.MAX_VALUE, Long.MAX_VALUE - Integer.MAX_VALUE, (long) Integer.MAX_VALUE},
                        {Long.MIN_VALUE, 1L, LargeIntegers.toNativeObject(image, BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE))},
                        {Long.MAX_VALUE, Long.MAX_VALUE - Integer.MAX_VALUE, (long) Integer.MAX_VALUE}};
        for (final Object[] values : testValues) {
            final Object wrappedValue0 = WrapToSqueakNode.executeUncached(values[0]);
            final Object wrappedValue1 = WrapToSqueakNode.executeUncached(values[1]);
            final Object wrappedValue2 = WrapToSqueakNode.executeUncached(values[2]);
            assertEquals(wrappedValue2, runPrimitive(2, wrappedValue0, wrappedValue1));
        }
        assertEquals(WrapToSqueakNode.executeUncached(Long.MAX_VALUE),
                        runPrimitive(22, WrapToSqueakNode.executeUncached(LargeIntegers.toNativeObject(image, BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE))),
                                        WrapToSqueakNode.executeUncached(1L)));
    }
}
