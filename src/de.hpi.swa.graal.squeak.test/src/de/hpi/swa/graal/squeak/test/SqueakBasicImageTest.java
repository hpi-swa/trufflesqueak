package de.hpi.swa.graal.squeak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

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
        assertEquals("foobar", result.toString());
        assertEquals("foobar", compilerEvaluate("[[self error: 'foobar'] value] on: Error do: [:err| ^ err messageText]").toString());
        assertEquals(image.sqTrue, compilerEvaluate("[[self error: 'foobar'] on: ZeroDivide do: [:e|]] on: Error do: [:err| ^ true]"));
        assertEquals(image.sqTrue, compilerEvaluate("[self error: 'foobar'. false] on: Error do: [:err| ^ err return: true]"));
    }

    @Test
    public void test06Value() {
        assertEquals(42L, evaluate("[42] value"));
        assertEquals(21L, evaluate("[[21] value] value"));
    }

    @Test
    public void test07SUnitTest() {
        assertEquals(image.sqTrue, evaluate("(TestCase new should: [1/0] raise: ZeroDivide) isKindOf: TestCase"));
    }

    @Test
    public void test08MethodContextRestart() {
        // MethodContextTest>>testRestart uses #should:notTakeMoreThan: (requires process switching)
        assertEquals(image.sqTrue, evaluate("[MethodContextTest new privRestartTest. true] value"));
    }

    @Test
    public void test09CompressAndDecompressBitmaps() {
        // Iterate over all ToolIcons, copy their bitmaps, and then compress and decompress them.
        assertEquals(image.sqTrue, evaluate("ToolIcons icons values allSatisfy: [:icon | | sourceBitmap sourceArray destBitmap |\n" +
                        "  icon unhibernate.\n" + // Ensure icon is decompressed and has a bitmap.
                        "  sourceBitmap := icon bits copy.\n" +
                        "  sourceArray := sourceBitmap compressToByteArray.\n" +
                        "  destBitmap := Bitmap new: sourceBitmap size.\n" +
                        "  destBitmap decompress: destBitmap fromByteArray: sourceArray at: 1.\n" +
                        "  destBitmap = sourceBitmap]"));
    }

    @Test
    public void test10TinyBenchmarks() {
        final String resultString = evaluate("1 tinyBenchmarks").toString();
        assertTrue(resultString.contains("bytecodes/sec"));
        assertTrue(resultString.contains("sends/sec"));
        image.getOutput().println("tinyBenchmarks: " + resultString);
    }
}
