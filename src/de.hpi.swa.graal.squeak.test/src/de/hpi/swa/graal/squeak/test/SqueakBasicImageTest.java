/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import de.hpi.swa.graal.squeak.model.BooleanObject;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SqueakBasicImageTest extends AbstractSqueakTestCaseWithImage {

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
        final Object result = compilerEvaluate("[[self error: 'foobar'] on: Error do: [:err| ^ err messageText]]value");
        assertEquals("'foobar'", result.toString());
        assertEquals("'foobar'", compilerEvaluate("[[self error: 'foobar'] value] on: Error do: [:err| ^ err messageText]").toString());
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
    public void test08MethodContextRestart() {
        // MethodContextTest>>testRestart uses #should:notTakeMoreThan: (requires process switching)
        assertEquals(BooleanObject.TRUE, evaluate("[MethodContextTest new privRestartTest. true] value"));
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
        image.getOutput().println("tinyBenchmarks: " + resultString);
    }

    @Test
    public void test11InteropJavaStringConversion() {
        /* Java Strings to Smalltalk ByteString/WideString */
        final String[] values = new String[]{"65" /* $A */, "16r1f43b" /* Bear Emoji */};
        for (final String value : values) {
            final String javaString = String.format("((Java type: 'java.lang.String') new: ((Java type: 'java.lang.Character') toChars: %s))", value);
            final String smalltalkString = String.format("(String with: (Character value: %s))", value);
            assertEquals(BooleanObject.TRUE, evaluate(String.format("%s = %s", javaString, smalltalkString)));
        }

        /* Smalltalk ByteString to Java */
        final String byteString = context.eval(SqueakLanguageConfig.ID, "String with: $A").asString();
        assertEquals("A", byteString);

        /* Smalltalk WideString to Java */
        final String wideString = context.eval(SqueakLanguageConfig.ID, "String with: (Character value: 16r1f43b)").asString();
        assertEquals(new String(Character.toChars(0x1f43b)), wideString);
    }

    @Test
    public void test12InteropJavaMiscellaneous() {
        // Issue #78
        final InetAddress inetAddress = context.eval(SqueakLanguageConfig.ID, "(Java type: 'java.net.InetAddress') getByAddress: #[192 168 0 1]").asHostObject();
        try {
            assertEquals(InetAddress.getByAddress(new byte[]{(byte) 192, (byte) 168, 0, 1}), inetAddress);
        } catch (final UnknownHostException e) {
            fail(e.toString());
        }
    }

    @Test
    public void test13CannotReturnAtStart() {
        assertEquals("'bla2'", evaluate("| result | \n" +
                        "[ result := [^'bla1'] on: BlockCannotReturn do: [:e | 'bla2' ]] fork. \n" +
                        "Processor yield.\n" +
                        "result").toString());
    }

    @Test
    public void test14CannotReturnInTheMiddle() {
        assertEquals("'bla2'", evaluate("| result | \n" +
                        "[ result := [thisContext yourself. ^'bla1'] on: BlockCannotReturn do: [:e | 'bla2' ]] fork. \n" +
                        "Processor yield.\n" +
                        "result").toString());
    }
}
