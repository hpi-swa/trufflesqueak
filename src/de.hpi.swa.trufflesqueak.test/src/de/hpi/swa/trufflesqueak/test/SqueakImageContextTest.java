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

    /**
     * A context that was suspended in a method must see changes to the method's bytecodes when it
     * is resumed, even if contexts of that method have been resumed before.
     */
    @Test
    public void testResumeAfterBytecodeChange() {
        patchMethod("Object", "tsResumeProbe:", "tsResumeProbe: sem | r | r := OrderedCollection new. sem == nil ifTrue: [^ 3 squared]. " +
                        "1 to: 3 do: [:i | sem wait. r add: 3 negated]. ^ r");
        assertTrue((boolean) evaluate(String.join(" ",
                        "| sem m sendNegated sendSquared positions out |",
                        "m := Object >> #tsResumeProbe:.",
                        "sendNegated := 16r80 + (m literals indexOf: #negated) - 1.",
                        "sendSquared := 16r80 + (m literals indexOf: #squared) - 1.",
                        "positions := (m initialPC to: m size) select: [:i | (m at: i) = sendNegated].",
                        "sem := Semaphore new.",
                        "[out := 3 tsResumeProbe: sem] fork.",
                        "Processor yield.",
                        "sem signal. Processor yield.",
                        "m at: positions first put: sendSquared.",
                        "sem signal. Processor yield.",
                        "sem signal. Processor yield.",
                        "positions size = 1 and: [out asArray = #(-3 9 9)]")));
    }

    @Test
    public void testParseEmptyFirstLine() {
        assertTrue(context.eval("smalltalk", "").isNull());
        assertEquals(3L, context.eval("smalltalk", "\n1 + 2").asLong());
        assertEquals(3L, context.eval("smalltalk", "\r\n1 + 2").asLong());
    }

    @Test
    public void testUnicodeSourceLiterals() {
        assertEquals(1L, context.eval("smalltalk", "'\u00e9' size").asLong());
        assertEquals(233L, context.eval("smalltalk", "'\u00e9' first asInteger").asLong());
        assertEquals(1L, context.eval("smalltalk", "'\u03bb' size").asLong());
        assertEquals(955L, context.eval("smalltalk", "'\u03bb' first asInteger").asLong());
        assertEquals(1L, context.eval("smalltalk", "'\ud83d\ude00' size").asLong());
        assertEquals(128512L, context.eval("smalltalk", "'\ud83d\ude00' first asInteger").asLong());
        assertEquals(3L, context.eval("smalltalk", "'abc' size").asLong());
    }
}
