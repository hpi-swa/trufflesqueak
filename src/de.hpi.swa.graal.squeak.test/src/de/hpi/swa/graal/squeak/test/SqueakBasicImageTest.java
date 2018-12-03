package de.hpi.swa.graal.squeak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import de.hpi.swa.graal.squeak.model.LargeIntegerObject;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SqueakBasicImageTest extends AbstractSqueakTestCaseWithImage {

    @Test
    public void test01AsSymbol() {
        assertEquals(image.getAsSymbolSelector(), asSymbol("asSymbol"));
    }

    @Test
    public void test02Numerical() {
        // Evaluate a few simple expressions to ensure that methodDictionaries grow correctly.
        for (long i = 0; i < 10; i++) {
            assertEquals(i + 1, evaluate(i + " + 1"));
        }
        assertEquals(4L, evaluate("-1 \\\\ 5"));
        if (image.flags.is64bit()) {
            assertEquals(LargeIntegerObject.SMALLINTEGER64_MIN, evaluate("SmallInteger minVal"));
            assertEquals(LargeIntegerObject.SMALLINTEGER64_MAX, evaluate("SmallInteger maxVal"));
        } else {
            assertEquals(LargeIntegerObject.SMALLINTEGER32_MIN, evaluate("SmallInteger minVal"));
            assertEquals(LargeIntegerObject.SMALLINTEGER32_MAX, evaluate("SmallInteger maxVal"));
        }
        // Long.MIN_VALUE / -1
        assertEquals("9223372036854775808", evaluate("-9223372036854775808 / -1").toString());
    }

    @Test
    public void test03ThisContext() {
        assertEquals(42L, evaluate("thisContext return: 42"));
    }

    @Test
    public void test04Ensure() {
        assertEquals(21L, evaluate("[21] ensure: [42]"));
        assertEquals(42L, evaluate("[21] ensure: [^42]"));
        assertEquals(21L, evaluate("[^21] ensure: [42]"));
        assertEquals(42L, evaluate("[^21] ensure: [^42]"));
    }

    @Test
    public void test05OnError() {
        final Object result = evaluate("[self error: 'foobar'] on: Error do: [:err| ^ err messageText]");
        assertEquals("foobar", result.toString());
        assertEquals("foobar", evaluate("[[self error: 'foobar'] value] on: Error do: [:err| ^ err messageText]").toString());
        assertEquals(image.sqTrue, evaluate("[[self error: 'foobar'] on: ZeroDivide do: [:e|]] on: Error do: [:err| ^ true]"));
        assertEquals(image.sqTrue, evaluate("[self error: 'foobar'. false] on: Error do: [:err| ^ err return: true]"));
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
    public void test09TinyBenchmarks() {
        final String resultString = evaluate("1 tinyBenchmarks").toString();
        assertTrue(resultString.contains("bytecodes/sec"));
        assertTrue(resultString.contains("sends/sec"));
        image.getOutput().println("tinyBenchmarks: " + resultString);
    }

    @Test
    public void test10CompressAndDecompressBitmaps() {
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
    public void testInspectSqueakTest() {
        assumeNotOnMXGate();
        runTestCase("ArrayTest");
    }

    @Test
    public void testInspectSqueakTestSelector() {
        assumeNotOnMXGate();
        image.getOutput().println(evaluate("(WordArrayTest run: #testCannotPutNegativeValue) asString"));
    }

    @Test
    public void testWPassingSqueakTests() {
        final List<String> failing = new ArrayList<>();
        final String[] testClasses = getSqueakTests(SqueakSUnitTestMap.TEST_TYPE.PASSING);
        printHeader(SqueakSUnitTestMap.TEST_TYPE.PASSING, testClasses);
        for (int i = 0; i < testClasses.length; i++) {
            final String result = runTestCase(testClasses[i]);
            if (!result.contains("passed")) {
                failing.add(result);
            }
        }
        failIfNotEmpty(failing);
    }

    @Test
    public void testXFlakySqueakTests() {
        final String[] testClasses = getSqueakTests(SqueakSUnitTestMap.TEST_TYPE.FLAKY);
        printHeader(SqueakSUnitTestMap.TEST_TYPE.FLAKY, testClasses);
        for (int i = 0; i < testClasses.length; i++) {
            runTestCase(testClasses[i]);
        }
    }

    @Test
    public void testYFailingSqueakTests() {
        testAndFailOnPassing(SqueakSUnitTestMap.TEST_TYPE.FAILING);
    }

    @Test
    public void testZNotTerminatingSqueakTests() {
        assumeNotOnMXGate();
        testAndFailOnPassing(SqueakSUnitTestMap.TEST_TYPE.NOT_TERMINATING);
    }

    private static String[] getSqueakTests(final String type) {
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < SqueakSUnitTestMap.SQUEAK_TEST_CASES.length; i += 2) {
            if (SqueakSUnitTestMap.SQUEAK_TEST_CASES[i + 1].equals(type)) {
                result.add((String) SqueakSUnitTestMap.SQUEAK_TEST_CASES[i]);
            }
        }
        return result.toArray(new String[0]);
    }

    private static void testAndFailOnPassing(final String type) {
        final List<String> passing = new ArrayList<>();
        final String[] testClasses = getSqueakTests(type);
        printHeader(type, testClasses);
        for (int i = 0; i < testClasses.length; i++) {
            final String result = runTestCase(testClasses[i]);
            if (result.contains("passed")) {
                passing.add(result);
            }
        }
        failIfNotEmpty(passing);
    }

    private static void failIfNotEmpty(final List<String> list) {
        if (!list.isEmpty()) {
            fail(String.join("\n", list));
        }
    }

    private static void printHeader(final String type, final String[] testClasses) {
        image.getOutput().println();
        image.getOutput().println(String.format("== %s %s Squeak Tests ====================", testClasses.length, type));
    }
}
