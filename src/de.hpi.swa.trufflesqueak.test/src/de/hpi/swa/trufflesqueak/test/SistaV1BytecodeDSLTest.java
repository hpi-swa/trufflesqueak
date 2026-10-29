/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SmalltalkSistaV1Interpreter;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@SuppressWarnings("static-method")
public final class SistaV1BytecodeDSLTest extends AbstractSqueakTestCaseWithDummyImage {
    private static CompiledCodeObject method(final int numArgs, final int numTemps, final int... bytes) {
        return makeMethod(CompiledCodeObject.CompiledCodeHeaderUtils.makeHeaderWord(false, numArgs, numTemps, 2, false, false), new Object[0], bytes);
    }

    private static Object execute(final CompiledCodeObject code, final Object receiver, final Object... args) {
        final var target = SmalltalkSistaV1Interpreter.build(code).getCallTarget();
        Object result = null;
        // Exercise both the uncached and cached DSL interpreters.
        for (int i = 0; i < 20; i++) {
            result = target.call(FrameAccess.newWith(NilObject.SINGLETON, null, receiver, args));
        }
        return result;
    }

    @Test
    public void conditionalExpressionMergesBothValues() {
        // ^ self ifTrue: [ 1 ] ifFalse: [ 0 ]
        final CompiledCodeObject code = method(0, 0, 0x4C, 0xC1, 0x51, 0xB0, 0x50, 0x5C);
        assertEquals(1L, execute(code, true));
        assertEquals(0L, execute(code, false));
    }

    @Test
    public void initializesArgumentsAndTemporaries() {
        assertEquals(42L, execute(method(1, 2, 0x40, 0x5C), NilObject.SINGLETON, 42L));
        assertEquals(NilObject.SINGLETON, execute(method(1, 2, 0x41, 0x5C), NilObject.SINGLETON, 42L));
    }

    @Test
    public void evaluatesTemporaryReadsBeforeStores() {
        // t := 1. Push t. t := 0. Return the earlier value of t.
        assertEquals(1L, execute(method(0, 1, 0x51, 0xD0, 0x40, 0x50, 0xD0, 0x5C), NilObject.SINGLETON));
    }

    @Test
    public void duplicateKeepsIndependentStackSlots() {
        assertEquals(2L, execute(method(0, 0, 0x51, 0x53, 0x60, 0x5C), NilObject.SINGLETON));
    }

    @Test
    public void popDiscardsOnlyTheTopValue() {
        assertEquals(1L, execute(method(0, 0, 0x51, 0x50, 0xD8, 0x5C), NilObject.SINGLETON));
    }

    @Test
    public void indexedAccessUsesOneBasedIndicesAndReturnsStoredValue() {
        final ArrayObject array = ArrayObject.createWithStorage(image.arrayClass, new Object[]{10L, 20L});
        assertEquals(10L, execute(method(0, 0, 0x4C, 0x51, 0x70, 0x5C), array));
        assertEquals(20L, execute(method(0, 0, 0x4C, 0xE8, 2, 0x70, 0x5C), array));
        assertEquals(42L, execute(method(0, 0, 0x4C, 0x51, 0xE8, 42, 0x71, 0x5C), array));
        assertEquals(42L, execute(method(0, 0, 0x4C, 0x51, 0x70, 0x5C), array));
    }

    @Test
    public void loopReadsUpdatedTemporaries() {
        // t := self. [t > 0] whileTrue: [t := t - 1]. ^t
        final CompiledCodeObject code = method(0, 1, 0x4C, 0xD0, 0x40, 0x50, 0x63, 0xC7, 0x40, 0x51, 0x61, 0xD0, 0xE1, 0xFF, 0xED, 0xF4, 0x40, 0x5C);
        assertEquals(0L, execute(code, 0L));
        assertEquals(0L, execute(code, 10L));
    }
}
