/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.oracle.truffle.api.Truffle;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SistaV1ResumeRootNode;
import de.hpi.swa.trufflesqueak.nodes.ResumeContextRootNode;
import de.hpi.swa.trufflesqueak.nodes.StartContextRootNode;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
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
    public void selectsInterpreterForMethodsAndResumptions() {
        final boolean useBytecodeDSL = Boolean.parseBoolean(System.getProperty("trufflesqueak.UseBytecodeDSL", "true"));
        final CompiledCodeObject code = method(0, 0, 0x4C, 0x5C);
        final var target = code.getCallTarget();
        assertTrue(useBytecodeDSL ? target.getRootNode() instanceof SmalltalkSistaV1Interpreter : target.getRootNode() instanceof StartContextRootNode);
        assertEquals(42L, target.call(FrameAccess.newWith(NilObject.SINGLETON, null, 42L)));

        final var frame = Truffle.getRuntime().createMaterializedFrame(FrameAccess.newWith(NilObject.SINGLETON, null, 42L), code.getFrameDescriptor());
        FrameAccess.setInstructionPointer(frame, 0);
        FrameAccess.setStackPointer(frame, 0);
        final var resumeTarget = code.getResumptionCallTarget(new ContextObject(frame));
        assertTrue(useBytecodeDSL ? resumeTarget.getRootNode() instanceof SistaV1ResumeRootNode : resumeTarget.getRootNode() instanceof ResumeContextRootNode);
        assertEquals(42L, resumeTarget.call());
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
    public void materializesNativeDSLContext() {
        final CompiledCodeObject code = method(0, 0, 0x52, 0x5C);
        final ContextObject result = (ContextObject) execute(code, 42L);
        assertSame(code, result.getCodeObject());
        assertEquals(42L, result.getReceiver());
        assertTrue(result.isDead());
    }

    @Test
    public void resumesSnapshotInsideLoop() {
        final CompiledCodeObject code = method(0, 1, 0x4C, 0xD0, 0x40, 0x50, 0x63, 0xC7, 0x40, 0x51, 0x61, 0xD0, 0xE1, 0xFF, 0xED, 0xF4, 0x40, 0x5C);
        final var frame = Truffle.getRuntime().createMaterializedFrame(FrameAccess.newWith(NilObject.SINGLETON, null, 99L), code.getFrameDescriptor());
        FrameAccess.setInstructionPointer(frame, 6);
        FrameAccess.setStackPointer(frame, 1);
        FrameAccess.setStackValue(frame, 0, 3L);
        final ContextObject suspended = new ContextObject(frame);
        assertEquals(0L, new SistaV1ResumeRootNode(suspended).getCallTarget().call());
        assertTrue(suspended.isDead());
    }

    @Test
    public void loopReadsUpdatedTemporaries() {
        // t := self. [t > 0] whileTrue: [t := t - 1]. ^t
        final CompiledCodeObject code = method(0, 1, 0x4C, 0xD0, 0x40, 0x50, 0x63, 0xC7, 0x40, 0x51, 0x61, 0xD0, 0xE1, 0xFF, 0xED, 0xF4, 0x40, 0x5C);
        assertEquals(0L, execute(code, 0L));
        assertEquals(0L, execute(code, 10L));
    }
}
