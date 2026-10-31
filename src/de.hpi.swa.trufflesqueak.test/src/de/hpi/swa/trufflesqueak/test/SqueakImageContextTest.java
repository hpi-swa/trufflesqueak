/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

@SuppressWarnings("static-method")
public final class SqueakImageContextTest extends AbstractSqueakTestCaseWithImage {
    @Test
    public void testFloatSubtractLargeFraction() {
        assertEquals(1.0, (double) evaluate("1.0 - (1 / (1 bitShift: 100))"), 0.0);
        assertEquals(1.0 - 0x1.0p100 / 3.0, (double) evaluate("1.0 - ((1 bitShift: 100) / 3)"), 0.0);
    }

    @Test
    public void testFractionSignNormalizationAtLongMinValue() {
        assertTrue((boolean) evaluate("(-9223372036854775808 / -3) numerator = 9223372036854775808"));
        assertTrue((boolean) evaluate("(-9223372036854775808 / -6) numerator = 4611686018427387904"));
        assertTrue((boolean) evaluate("(1 / -9223372036854775808) numerator = -1"));
        assertTrue((boolean) evaluate("(1 / -9223372036854775808) denominator = 9223372036854775808"));
        assertTrue((boolean) evaluate("(2 / -9223372036854775808) denominator = 4611686018427387904"));
        assertTrue((boolean) evaluate("(-9223372036854775808 / 3) numerator = -9223372036854775808"));
    }

    @Test
    public void testParseEmptyFirstLine() {
        assertTrue(context.eval("smalltalk", "").isNull());
        assertEquals(3L, context.eval("smalltalk", "\n1 + 2").asLong());
        assertEquals(3L, context.eval("smalltalk", "\r\n1 + 2").asLong());
    }
}
