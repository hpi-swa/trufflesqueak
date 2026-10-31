/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

@SuppressWarnings("static-method")
public final class SqueakImageContextTest extends AbstractSqueakTestCaseWithImage {
    @Test
    public void testFloatSubtractLargeFraction() {
        assertEquals(1.0, (double) evaluate("1.0 - (1 / (1 bitShift: 100))"), 0.0);
        assertEquals(1.0 - 0x1.0p100 / 3.0, (double) evaluate("1.0 - ((1 bitShift: 100) / 3)"), 0.0);
    }
}
