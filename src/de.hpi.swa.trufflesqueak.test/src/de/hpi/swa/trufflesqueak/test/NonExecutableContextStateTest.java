/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;

@SuppressWarnings("static-method")
public final class NonExecutableContextStateTest extends AbstractSqueakTestCaseWithImage {

    @Test
    public void testNonExecutableToFrameRoundTrip() {
        final ContextObject fresh = (ContextObject) evaluate("Context newForMethod: CompiledMethod toReturnSelf");
        assertTrue(fresh.hasNonExecutableState());
        assertFalse(fresh.hasTruffleFrame());

        final ContextObject materialized = (ContextObject) evaluate("""
                        | ctx method |
                        method := Object >> #yourself.
                        ctx := Context newForMethod: CompiledMethod toReturnSelf.
                        ctx instVarAt: 1 put: nil.
                        ctx instVarAt: 2 put: method initialPC.
                        ctx instVarAt: 4 put: method.
                        ctx instVarAt: 3 put: 1.
                        ctx instVarAt: 6 put: #myReceiver.
                        ctx tempAt: 1 put: #myTemp.
                        ctx
                        """);
        assertTrue(materialized.hasTruffleFrame());
        assertFalse(materialized.hasNonExecutableState());
        assertEquals(1, materialized.getStackPointer());
        assertEquals("#myTemp", materialized.atTemp(0).toString());

        final ContextObject dematerialized = (ContextObject) evaluate("""
                        | ctx method |
                        method := Object >> #yourself.
                        ctx := Context newForMethod: CompiledMethod toReturnSelf.
                        ctx instVarAt: 1 put: nil.
                        ctx instVarAt: 2 put: method initialPC.
                        ctx instVarAt: 4 put: method.
                        ctx instVarAt: 3 put: 1.
                        ctx instVarAt: 6 put: #myReceiver.
                        ctx tempAt: 1 put: #myTemp.
                        ctx instVarAt: 4 put: 'not a method'.
                        ctx
                        """);
        assertTrue(dematerialized.hasNonExecutableState());
        assertFalse(dematerialized.hasTruffleFrame());
        assertSame(NilObject.SINGLETON, dematerialized.rawAt(CONTEXT.SENDER_OR_NIL));
        assertEquals(1L, dematerialized.rawAt(CONTEXT.STACKPOINTER));
        assertEquals("'not a method'", dematerialized.rawAt(CONTEXT.METHOD).toString());
        assertEquals("#myReceiver", dematerialized.rawAt(CONTEXT.RECEIVER).toString());
        assertEquals("#myTemp", dematerialized.rawAt(CONTEXT.TEMP_FRAME_START).toString());

        final ContextObject rematerialized = (ContextObject) evaluate("""
                        | ctx method |
                        method := Object >> #yourself.
                        ctx := Context newForMethod: CompiledMethod toReturnSelf.
                        ctx instVarAt: 1 put: nil.
                        ctx instVarAt: 2 put: method initialPC.
                        ctx instVarAt: 4 put: method.
                        ctx instVarAt: 3 put: 1.
                        ctx instVarAt: 4 put: 'not a method'.
                        ctx instVarAt: 4 put: method.
                        ctx
                        """);
        assertTrue(rematerialized.hasTruffleFrame());
        assertFalse(rematerialized.hasNonExecutableState());
    }

    @Test
    public void testMethodThenStackPointerThenClosureSequence() {
        final ContextObject ctx = (ContextObject) evaluate("""
                        | block outerMethod ctx |
                        block := [ :x | x + 1 ].
                        outerMethod := block outerContext method.
                        ctx := Context newForMethod: outerMethod.
                        ctx instVarAt: 1 put: nil.
                        ctx instVarAt: 2 put: outerMethod initialPC.
                        ctx instVarAt: 4 put: outerMethod.
                        ctx instVarAt: 3 put: 1.
                        ctx tempAt: 1 put: 41.
                        ctx instVarAt: 5 put: block.
                        ctx
                        """);
        assertTrue(ctx.hasTruffleFrame());
        assertFalse(ctx.hasNonExecutableState());
        assertTrue(ctx.hasClosure());
        assertEquals(41L, ctx.atTemp(0));
    }

    @Test
    public void testSenderChainWithNonExecutableContext() {
        final ContextObject top = (ContextObject) evaluate("""
                        | bottom nonExecutable top method |
                        method := Object >> #yourself.
                        bottom := Context newForMethod: CompiledMethod toReturnSelf.
                        nonExecutable := Context newForMethod: CompiledMethod toReturnSelf.
                        nonExecutable instVarAt: 1 put: bottom.
                        top := Context newForMethod: CompiledMethod toReturnSelf.
                        top instVarAt: 1 put: nonExecutable.
                        top instVarAt: 2 put: method initialPC.
                        top instVarAt: 4 put: method.
                        top instVarAt: 3 put: 0.
                        top
                        """);
        assertTrue(top.hasTruffleFrame());
        final ContextObject nonExecutable = (ContextObject) top.getSender();
        assertTrue(nonExecutable.hasNonExecutableState());
        assertFalse(nonExecutable.hasTruffleFrame());
        final ContextObject bottom = (ContextObject) nonExecutable.getSender();
        assertTrue(bottom.hasNonExecutableState());
    }

    @Test
    public void testNonContextSenderStaysInFrame() {
        final ContextObject ctx = (ContextObject) evaluate("""
                        | ctx |
                        ctx := Context sender: nil receiver: 3 method: Object >> #yourself arguments: #().
                        ctx instVarAt: 1 put: 42.
                        (ctx instVarAt: 1) = 42 ifFalse: [ self error: 'expected sender 42' ].
                        ctx sender = 42 ifFalse: [ self error: 'expected sender 42 via accessor' ].
                        ctx privSender: #notAContext.
                        ctx
                        """);
        assertTrue(ctx.hasTruffleFrame());
        assertFalse(ctx.hasNonExecutableState());
        assertEquals("#notAContext", ctx.getSender().toString());
        assertEquals("#notAContext", ctx.getFrameSender().toString());
    }

    @Test
    public void testStackPointerBeyondFrameCapacity() {
        final ContextObject ctx = (ContextObject) evaluate("""
                        | ctx |
                        ctx := Context sender: nil receiver: 3 method: Object >> #yourself arguments: #().
                        ctx stackp: 10.
                        ctx at: 10 put: #top.
                        ctx
                        """);
        /* The frame of Object>>#yourself is smaller than 10 slots, but the context has 16. */
        assertTrue(ctx.hasNonExecutableState());
        assertEquals(10, ctx.getStackPointerOrZero());
        assertEquals("#top", ctx.rawAt(CONTEXT.TEMP_FRAME_START + 9).toString());
        assertEquals("Object#yourself", ctx.rawAt(CONTEXT.METHOD).toString());

        final ContextObject shrunk = (ContextObject) evaluate("""
                        | ctx |
                        ctx := Context sender: nil receiver: 3 method: Object >> #yourself arguments: #().
                        ctx stackp: 10.
                        ctx stackp: 0.
                        ([ ctx stackp: 17. false ] on: Error do: [ :ex | true ]) ifFalse: [ self error: 'stackp: 17 should fail' ].
                        ctx
                        """);
        assertTrue(shrunk.hasTruffleFrame());
        assertEquals(0, shrunk.getStackPointer());
    }

    @Test
    public void testInvalidStackPointerStoredByBytecode() {
        final Object result = evaluate("""
                        | ctx result |
                        Context compileSilently: 'storeStackPointerForTest: n stackp := n' classified: 'test'.
                        ctx := Context sender: nil receiver: 3 method: Object >> #yourself arguments: #().
                        result := [
                            "Must neither fail a primitive further up the stack nor skip this handler."
                            [ :c | c storeStackPointerForTest: 100. c instVarAt: 3 ] value: ctx ]
                                on: Error do: [ :ex | ex messageText ].
                        Context removeSelectorSilently: #storeStackPointerForTest:.
                        { result. ctx size. ctx method == (Object >> #yourself) } printString
                        """);
        assertEquals("'#(100 0 true)'", result.toString());
    }

    @Test
    public void testNonExecutableContextsCannotBeReturnedTo() {
        final ContextObject fresh = (ContextObject) evaluate("Context newForMethod: CompiledMethod toReturnSelf");
        assertFalse(fresh.canBeExecuted());
        assertTrue(fresh.isDead());

        final ContextObject withNonExecutableSender = (ContextObject) evaluate("""
                        | ctx |
                        ctx := Context sender: nil receiver: 3 method: Object >> #yourself arguments: #().
                        ctx privSender: (Context newForMethod: CompiledMethod toReturnSelf).
                        ctx
                        """);
        assertFalse(withNonExecutableSender.isDead());
        assertFalse(withNonExecutableSender.canReturnToSender());

        /* A non-local return whose home context is non-executable ends in BlockCannotReturn. */
        assertEquals("#BlockCannotReturn", evaluate("""
                        [ | block |
                            block := [ ^ 3 ].
                            block instVarNamed: 'outerContext' put: (Context newForMethod: CompiledMethod toReturnSelf).
                            block value ] on: Error do: [ :ex | ex class name ]
                        """).toString());
    }

    @Test
    public void testTerminateToOnNonExecutableContext() {
        assertEquals(true, evaluate("""
                        | ctx target |
                        ctx := Context newForMethod: CompiledMethod toReturnSelf.
                        ctx terminateTo: nil.
                        ctx sender isNil ifFalse: [ self error: 'expected nil sender' ].
                        target := Context sender: nil receiver: 3 method: Object >> #yourself arguments: #().
                        ctx terminateTo: target.
                        ctx sender == target
                        """));
    }

    @Test
    public void testRemoveClosure() {
        final ContextObject ctx = (ContextObject) evaluate("""
                        | ctx |
                        ctx := Context sender: nil receiver: 3 method: Object >> #yourself arguments: #().
                        ctx instVarAt: 5 put: [ :x | x ].
                        ctx closure isNil ifTrue: [ self error: 'expected a closure' ].
                        ctx instVarAt: 5 put: nil.
                        ctx closure isNil ifFalse: [ self error: 'expected no closure' ].
                        ctx receiver = 3 ifFalse: [ self error: 'expected receiver 3' ].
                        ctx
                        """);
        assertTrue(ctx.hasTruffleFrame());
        assertFalse(ctx.hasClosure());
        /* A context that is running cannot give up its frame; the primitive fails instead. */
        assertEquals(true, evaluate("[ [ thisContext instVarAt: 5 put: nil. false ] on: Error do: [ :ex | true ] ] value"));
    }

    @Test
    public void testOutOfBoundsWritesAndStackPointerValidation() {
        final ContextObject ctx = (ContextObject) evaluate("""
                        | ctx method |
                        ctx := Context newForMethod: CompiledMethod toReturnSelf.
                        (ctx instVarAt: 3) isNil ifFalse: [ self error: 'initial stackp should be nil' ].

                        ([ ctx tempAt: 0 put: #bad. false ] on: Error do: [ :ex | true ]) ifFalse: [ self error: 'tempAt: 0 put: should fail' ].
                        ([ ctx tempAt: 100 put: #bad. false ] on: Error do: [ :ex | true ]) ifFalse: [ self error: 'tempAt: 100 put: should fail' ].

                        "Invalid stackp: and reflective stackp writes fail with Error and leave stackp nil"
                        ([ ctx stackp: -1. false ] on: Error do: [ :ex | true ]) ifFalse: [ self error: 'stackp: -1 should fail' ].
                        (ctx instVarAt: 3) isNil ifFalse: [ self error: 'stackp should remain nil after stackp: -1' ].
                        ([ ctx stackp: 100. false ] on: Error do: [ :ex | true ]) ifFalse: [ self error: 'stackp: 100 should fail' ].
                        (ctx instVarAt: 3) isNil ifFalse: [ self error: 'stackp should remain nil after stackp: 100' ].
                        ([ ctx stackp: 'notAnInt'. false ] on: Error do: [ :ex | true ]) ifFalse: [ self error: 'stackp: non-integer should fail' ].
                        (ctx instVarAt: 3) isNil ifFalse: [ self error: 'stackp should remain nil after invalid stackp:' ].
                        ([ ctx instVarAt: 3 put: -5. false ] on: Error do: [ :ex | true ]) ifFalse: [ self error: 'instVarAt: 3 put: -5 should fail' ].
                        (ctx instVarAt: 3) isNil ifFalse: [ self error: 'stackp should remain nil after instVarAt: 3 put: -5' ].
                        "Mirror writes bypass Context>>#stackp: and store any value; the effective stack pointer is then zero"
                        thisContext object: ctx instVarAt: 3 put: -5.
                        (ctx instVarAt: 3) = -5 ifFalse: [ self error: 'expected raw stackp -5' ].
                        ctx size = 0 ifFalse: [ self error: 'expected size 0 for stackp -5' ].
                        thisContext object: ctx instVarAt: 3 put: 16r100000000.
                        (ctx instVarAt: 3) = 16r100000000 ifFalse: [ self error: 'expected raw stackp 2^32' ].
                        ctx size = 0 ifFalse: [ self error: 'expected size 0 for stackp 2^32' ].
                        thisContext object: ctx instVarAt: 3 put: 'notAnInt'.
                        (ctx instVarAt: 3) = 'notAnInt' ifFalse: [ self error: 'expected raw non-integer stackp' ].
                        ctx size = 0 ifFalse: [ self error: 'expected size 0 for non-integer stackp' ].
                        thisContext object: ctx instVarAt: 3 put: nil.

                        "Valid stackp: from nil initializes new slots to nil"
                        ctx stackp: 2.
                        (ctx instVarAt: 3) = 2 ifFalse: [ self error: 'expected stackp 2' ].
                        (ctx tempAt: 1) isNil ifFalse: [ self error: 'expected temp 1 nil' ].
                        (ctx tempAt: 2) isNil ifFalse: [ self error: 'expected temp 2 nil' ].

                        "Reading back a non-object sender from a non-executable context returns the raw stored value"
                        ctx instVarAt: 1 put: 42.
                        (ctx instVarAt: 1) = 42 ifFalse: [ self error: 'expected raw sender 42' ].

                        "Bare-sender thisContext materializes cleanly on reflective access"
                        (thisContext instVarAt: 4) == thisContext method ifFalse: [ self error: 'expected bare-sender method match' ].

                        method := Object >> #yourself.
                        ctx instVarAt: 1 put: nil.
                        ctx instVarAt: 2 put: method initialPC.
                        ctx instVarAt: 4 put: method.
                        ctx instVarAt: 3 put: 0.

                        "Invalid mirror stackp writes on a context with a frame are kept and make it non-executable"
                        thisContext object: ctx instVarAt: 3 put: -1.
                        (ctx instVarAt: 3) = -1 ifFalse: [ self error: 'expected raw stackp -1' ].
                        thisContext object: ctx instVarAt: 3 put: 'notAnInt'.
                        (ctx instVarAt: 3) = 'notAnInt' ifFalse: [ self error: 'expected raw non-integer stackp' ].
                        (ctx instVarAt: 4) == method ifFalse: [ self error: 'method should be kept' ].
                        "A valid value makes it executable again"
                        thisContext object: ctx instVarAt: 3 put: 0.
                        (ctx instVarAt: 3) = 0 ifFalse: [ self error: 'expected stackp 0' ].

                        "instVarAt: 7 (0-based slot 6 = TEMP_FRAME_START) is in [numStackSlots, size) and is a silent no-op"
                        ctx instVarAt: 7 put: #ignored.
                        (ctx instVarAt: 7) isNil ifFalse: [ self error: 'expected dead-space write ignored' ].
                        ([ ctx tempAt: 0 put: #bad. false ] on: Error do: [ :ex | true ]) ifFalse: [ self error: 'frame tempAt: 0 put: should fail' ].
                        ([ ctx tempAt: 100 put: #bad. false ] on: Error do: [ :ex | true ]) ifFalse: [ self error: 'frame tempAt: 100 put: should fail' ].
                        ctx
                        """);
        assertTrue(ctx.hasTruffleFrame());
        assertFalse(ctx.hasNonExecutableState());
    }

    @Test
    public void testActiveContextInvalidReflectiveWritesFailCleanly() {
        final Object failedAll = evaluate("""
                        | f1 f2 f3 f4 |
                        f1 := [ thisContext instVarAt: 4 put: 'bad'. false ] on: Error do: [ :ex | true ].
                        f2 := [ thisContext instVarAt: 2 put: -1. false ] on: Error do: [ :ex | true ].
                        f3 := [ thisContext slotAt: 4 put: 'bad'. false ] on: Error do: [ :ex | true ].
                        f4 := [ thisContext object: thisContext instVarAt: 4 put: 'bad'. false ] on: Error do: [ :ex | true ].
                        f1 and: [ f2 and: [ f3 and: [ f4 ] ] ]
                        """);
        assertEquals(true, failedAll);
    }

    @Test
    public void testNonExecutableAndBareSenderImageSerialization() throws Exception {
        final Path tempImage = Files.createTempFile("trufflesqueak-context-test", ".image");
        final String originalImagePath = image.getImagePath();
        try {
            image.setImagePath(tempImage.toAbsolutePath().toString());
            final Object result = evaluate("""
                            | nonExecutable err snapResult |
                            nonExecutable := Context newForMethod: CompiledMethod toReturnSelf.
                            nonExecutable instVarAt: 3 put: 1.
                            nonExecutable instVarAt: 4 put: 'unmaterializable'.
                            nonExecutable instVarAt: 6 put: #receiverMarker.
                            nonExecutable tempAt: 1 put: #tempMarker.
                            [ snapResult := Smalltalk snapshotPrimitive ] on: Exception do: [ :ex | err := ex printString ].
                            err ifNotNil: [ ^ err ].
                            snapResult == false ifFalse: [ ^ 'snapshotPrimitive returned: ', snapResult printString ].
                            nonExecutable
                            """);
            assertTrue("Snapshot failed: " + result, result instanceof ContextObject);
            assertTrue(((ContextObject) result).hasNonExecutableState());
            assertTrue(Files.size(tempImage) > 0);
        } finally {
            image.setImagePath(originalImagePath);
            Files.deleteIfExists(tempImage);
        }
    }

    @Test
    public void testRecursiveBareSenderMaterialization() {
        final Object result = evaluate("""
                        | recurse |
                        recurse := [ :n |
                            n = 0
                                ifTrue: [
                                    | c1 c2 |
                                    c1 := thisContext sender.
                                    c2 := c1 instVarAt: 1.
                                    c1 ~~ c2
                                        and: [ (c1 instVarAt: 4) == c1 method
                                        and: [ (c2 instVarAt: 4) == c2 method ] ] ]
                                ifFalse: [ recurse value: n - 1 ] ].
                        recurse value: 2
                        """);
        assertEquals(true, result);
    }
}
