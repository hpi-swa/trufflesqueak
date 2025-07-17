/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.oracle.truffle.api.TruffleFile;

import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageOptions;

@SuppressWarnings("static-method")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class SqueakBasicImageTest extends AbstractSqueakTestCaseWithImage {

    @Test
    public void test01AsSymbol() {
        assertSame(evaluate("'asSymbol' asSymbol"), evaluate("'asSymbol' asSymbol"));
    }

    @Test
    public void test02Numerical() {
        // Evaluate a few simple expressions to ensure that methodDictionaries grow correctly.
        for (long i = 0; i < 10; i++) {
            assertEquals(i + 1, evaluate(i + " + 1"));
        }
        assertEquals(4L, evaluate("-1 \\\\ 5"));
        assertEquals(Long.MIN_VALUE, evaluate("SmallInteger minVal"));
        assertEquals(Long.MAX_VALUE, evaluate("SmallInteger maxVal"));
        // Long.MIN_VALUE / -1
        assertEquals("9223372036854775808", evaluate("-9223372036854775808 / -1").toString());
    }

    @Test
    public void test03ThisContext() {
        assertEquals(42L, compilerEvaluate("thisContext return: 42"));
    }

    @Test
    public void test04Ensure() {
        assertEquals(21L, compilerEvaluate("[21] ensure: [42]"));
        assertEquals(42L, compilerEvaluate("[21] ensure: [^42]"));
        assertEquals(21L, compilerEvaluate("[^21] ensure: [42]"));
        assertEquals(42L, compilerEvaluate("[^21] ensure: [^42]"));
    }

    @Test
    public void test05OnError() {
        final NativeObject result = (NativeObject) compilerEvaluate("[[self error: 'foobar'] on: Error do: [:err| ^ err messageText]]value");
        assertEquals("foobar", result.asStringUnsafe());
        assertEquals("foobar", ((NativeObject) compilerEvaluate("[[self error: 'foobar'] value] on: Error do: [:err| ^ err messageText]")).asStringUnsafe());
        assertEquals(BooleanObject.TRUE, compilerEvaluate("[[self error: 'foobar'] on: ZeroDivide do: [:e|]] on: Error do: [:err| ^ true]"));
        assertEquals(BooleanObject.TRUE, compilerEvaluate("[self error: 'foobar'. false] on: Error do: [:err| ^ err return: true]"));
    }

    @Test
    public void test06Value() {
        assertEquals(42L, evaluate("[42] value"));
        assertEquals(21L, evaluate("[[21] value] value"));
    }

    @Test
    public void test07SUnitTest() {
        assertEquals(BooleanObject.TRUE, evaluate("(TestCase new should: [1/0] raise: ZeroDivide) isKindOf: TestCase"));
    }

    @Test
    public void test08ContextRestart() {
        // ContextTest>>testRestart uses #should:notTakeMoreThan: (requires process switching)
        assertEquals(BooleanObject.TRUE, evaluate("[ContextTest new privRestartTest. true] value"));
    }

    @Test
    public void test09CompressAndDecompressBitmaps() {
        // Iterate over all ToolIcons, copy their bitmaps, and then compress and decompress them.
        assertEquals(BooleanObject.TRUE, evaluate("ToolIcons icons values allSatisfy: [:icon | | sourceBitmap sourceArray destBitmap |\n" +
                        "  icon unhibernate.\n" + // Ensure icon is decompressed and has a bitmap.
                        "  sourceBitmap := icon bits copy.\n" +
                        "  sourceArray := sourceBitmap compressToByteArray.\n" +
                        "  destBitmap := Bitmap new: sourceBitmap size.\n" +
                        "  destBitmap decompress: destBitmap fromByteArray: sourceArray at: 1.\n" +
                        "  destBitmap = sourceBitmap]"));
    }

    @Test
    public void test10TinyBenchmarks() {
        final String resultString = context.eval(SqueakLanguageConfig.ID, "1 tinyBenchmarks").asString();
        assertTrue(resultString.contains("bytecodes/sec"));
        assertTrue(resultString.contains("sends/sec"));
        println("tinyBenchmarks: " + resultString);
    }

    @Test
    public void test11InteropJavaStringConversion() {
        /* For a Java Strings to Smalltalk ByteString/WideString test, see Java>>testJavaString. */

        /* Smalltalk ByteString to Java */
        final String byteString = context.eval(SqueakLanguageConfig.ID, "String with: $A").asString();
        assertEquals("A", byteString);

        /* Smalltalk WideString to Java */
        final String wideString = context.eval(SqueakLanguageConfig.ID, "String with: (Character value: 16r1f43b)").asString();
        assertEquals(new String(Character.toChars(0x1f43b)), wideString);
    }

    @Test
    public void test12CannotReturnAtStart() {
        assertEquals("bla2", ((NativeObject) evaluate("""
                        | result |
                        [ result := [^'bla1'] on: BlockCannotReturn do: [:e | 'bla2' ]] fork.
                        Processor yield.
                        result""")).asStringUnsafe());
    }

    @Test
    public void test13CannotReturnInTheMiddle() {
        assertEquals("bla2", ((NativeObject) evaluate("""
                        | result |
                        [ result := [thisContext yourself. ^'bla1'] on: BlockCannotReturn do: [:e | 'bla2' ]] fork.
                        Processor yield.
                        result""")).asStringUnsafe());
    }

    @Test
    public void test14ImageSnapshot() {
        final String newImageName = "test14ImageSnapshot.image";
        final String newChangesName = "test14ImageSnapshot.changes";
        evaluate(String.format("Smalltalk saveAs: '%s'", newImageName));
        final TruffleFile newImageFile = image.env.getInternalTruffleFile(image.getImagePath()).getParent().resolve(newImageName);
        final TruffleFile newChangesFile = image.env.getInternalTruffleFile(image.getImagePath()).getParent().resolve(newChangesName);
        assertTrue(newImageFile.exists());
        assertTrue(newChangesFile.exists());
        /* Open the saved image and run code in it. */
        final Context newContext = Context.newBuilder(SqueakLanguageConfig.ID).allowAllAccess(true).option(SqueakLanguageConfig.ID + "." + SqueakLanguageOptions.IMAGE_PATH,
                        newImageFile.getPath()).option(SqueakLanguageConfig.ID + "." + SqueakLanguageOptions.HEADLESS, "true").build();
        newContext.enter();
        try {
            final Value result = newContext.eval(SqueakLanguageConfig.ID, "1 + 2 * 3");
            assertTrue(result.fitsInInt());
            assertEquals(9, result.asInt());
        } finally { /* Cleanup */
            newContext.leave();
            try {
                newImageFile.delete();
                newChangesFile.delete();
            } catch (final IOException e) {
                fail(e.getMessage());
            }
        }
    }
}
