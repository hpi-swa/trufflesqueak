package de.hpi.swa.graal.squeak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.TEST_RESULT;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.process.GetActiveProcessNode;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SqueakSUnitTest extends AbstractSqueakTestCase {
    private static final int TIMEOUT_IN_SECONDS = 5 * 60;
    private static Object smalltalkDictionary;
    private static Object smalltalkAssociation;
    private static Object evaluateSymbol;
    private static Object compilerSymbol;

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
        runTestCase("BitBltClipBugs");
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

    @BeforeClass
    public static void loadTestImage() {
        final String imagePath = getPathToTestImage();
        ensureImageContext(imagePath);
        image.getOutput().println("Test image loaded from " + imagePath + "...");
        patchImageForTesting();
    }

    private static void patchImageForTesting() {
        final PointersObject activeProcess = GetActiveProcessNode.create(image).executeGet();
        activeProcess.atput0(PROCESS.SUSPENDED_CONTEXT, image.nil);
        image.getOutput().println("Modifying StartUpList for testing...");
        evaluate("{EventSensor. Project} do: [:ea | Smalltalk removeFromStartUpList: ea]");
        image.getOutput().println("Processing StartUpList...");
        evaluate("Smalltalk processStartUpList: true");
        image.getOutput().println("Setting author information...");
        evaluate("Utilities authorName: 'GraalSqueak'");
        evaluate("Utilities setAuthorInitials: 'GraalSqueak'");
        image.getOutput().println("Initializing fresh MorphicUIManager...");
        evaluate("Project current instVarNamed: #uiManager put: MorphicUIManager new");

        patchMethod("TestCase", "timeout:after:", "timeout: aBlock after: seconds ^ aBlock value");
        patchMethod("BlockClosure", "valueWithin:onTimeout:", "valueWithin: aDuration onTimeout: timeoutBlock ^ self value");
        if (!runsOnMXGate()) {
            patchMethod("TestCase", "runCase", "runCase [self setUp. [self performTest] ensure: [self tearDown]] on: Error do: [:e | e printVerboseOn: FileStream stderr. e signal]");
        }
    }

    private static boolean runsOnMXGate() {
        return "true".equals(System.getenv("MX_GATE"));
    }

    private static void assumeNotOnMXGate() {
        Assume.assumeFalse("TestCase skipped on `mx gate`.", runsOnMXGate());
    }

    private static String getPathToTestImage() {
        final String imagePath64bit = getPathToTestImage("test-64bit.image");
        if (imagePath64bit != null) {
            return imagePath64bit;
        }
        final String imagePath32bit = getPathToTestImage("test-32bit.image");
        if (imagePath32bit != null) {
            return imagePath32bit;
        }
        throw new SqueakException("Unable to locate test image.");
    }

    private static String getPathToTestImage(final String imageName) {
        File currentDirectory = new File(System.getProperty("user.dir"));
        while (currentDirectory != null) {
            final String pathToImage = currentDirectory.getAbsolutePath() + File.separator + "images" + File.separator + imageName;
            if (new File(pathToImage).exists()) {
                return pathToImage;
            }
            currentDirectory = currentDirectory.getParentFile();
        }
        return null;
    }

    private static Object getSmalltalkDictionary() {
        if (smalltalkDictionary == null) {
            smalltalkDictionary = image.specialObjectsArray.at0Object(SPECIAL_OBJECT_INDEX.SmalltalkDictionary);
        }
        return smalltalkDictionary;
    }

    private static Object getSmalltalkAssociation() {
        if (smalltalkAssociation == null) {
            smalltalkAssociation = new PointersObject(image, image.schedulerAssociation.getSqueakClass(), new Object[]{image.newSymbol("Smalltalk"), getSmalltalkDictionary()});
        }
        return smalltalkAssociation;
    }

    private static Object getEvaluateSymbol() {
        if (evaluateSymbol == null) {
            evaluateSymbol = asSymbol("evaluate:");
        }
        return evaluateSymbol;
    }

    private static Object getCompilerSymbol() {
        if (compilerSymbol == null) {
            compilerSymbol = asSymbol("Compiler");
        }
        return compilerSymbol;
    }

    private static Object asSymbol(final String value) {
        final String fakeMethodName = "fakeAsSymbol" + value.hashCode();
        final CompiledMethodObject method = makeMethod(
                        new Object[]{4L, image.getAsSymbolSelector(), image.wrap(value), image.newSymbol(fakeMethodName), getSmalltalkAssociation()},
                        new int[]{0x21, 0xD0, 0x7C});
        return runMethod(method, getSmalltalkDictionary());
    }

    /*
     * Executes a fake Smalltalk method equivalent to:
     *
     * `^ (Smalltalk at: #Compiler) evaluate: expression`
     *
     */
    private static Object evaluate(final String expression) {
        //
        final String fakeMethodName = "fakeEvaluate" + expression.hashCode();
        final CompiledMethodObject method = makeMethod(
                        new Object[]{6L, getEvaluateSymbol(), getSmalltalkAssociation(), getCompilerSymbol(), image.wrap(expression), asSymbol(fakeMethodName), getSmalltalkAssociation()},
                        new int[]{0x41, 0x22, 0xC0, 0x23, 0xE0, 0x7C});
        image.interrupt.reset(); // Avoid incorrect state across executions
        return runMethod(method, getSmalltalkDictionary());
    }

    private static void patchMethod(final String className, final String selector, final String body) {
        image.getOutput().println("Patching " + className + ">>#" + selector + "...");
        final Object patchResult = evaluate(String.join(" ",
                        className, "addSelectorSilently:", "#" + selector, "withMethod: (", className, "compile: '" + body + "'",
                        "notifying: nil trailer: (CompiledMethodTrailer empty) ifFail: [^ nil]) method"));
        assertNotEquals(image.nil, patchResult);
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

    private static String runTestCase(final String testClassName) {
        final String timeoutErrorMessage = "did not terminate in " + TIMEOUT_IN_SECONDS + "s";
        final String[] result = new String[]{timeoutErrorMessage};

        image.getOutput().print(testClassName + ": ");
        image.getOutput().flush();

        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                result[0] = invokeTestCase(testClassName);
            }
        });
        final long startTime = System.currentTimeMillis();
        thread.start();
        try {
            thread.join(TIMEOUT_IN_SECONDS * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (thread.isAlive()) {
            thread.interrupt();
        }
        final double timeToRun = (System.currentTimeMillis() - startTime) / 1000.0;
        image.getOutput().println(result[0] + " [" + timeToRun + "s]");
        return testClassName + ": " + result[0];
    }

    private static String invokeTestCase(final String testClassName) {
        try {
            return extractFailuresAndErrorsFromTestResult(evaluate(testClassName + " buildSuite run"));
        } catch (Exception e) {
            e.printStackTrace();
            return "failed with an error: " + e.toString();
        }
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

    private static String extractFailuresAndErrorsFromTestResult(final Object result) {
        final SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();
        final SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();

        if (!(result instanceof AbstractSqueakObject) || !result.toString().equals("a TestResult")) {
            return "did not return a TestResult, got " + result.toString();
        }
        final PointersObject testResult = (PointersObject) result;
        final List<String> output = new ArrayList<>();
        final ArrayObject failureArray = (ArrayObject) ((PointersObject) testResult.at0(TEST_RESULT.FAILURES)).at0(1);
        for (int i = 0; i < sizeNode.execute(failureArray); i++) {
            final AbstractSqueakObject value = (AbstractSqueakObject) at0Node.execute(failureArray, i);
            if (value != image.nil) {
                output.add(((PointersObject) value).at0(0) + " (E)");
            }
        }
        final ArrayObject errorArray = (ArrayObject) ((PointersObject) testResult.at0(TEST_RESULT.ERRORS)).at0(0);
        for (int i = 0; i < sizeNode.execute(errorArray); i++) {
            final AbstractSqueakObject value = (AbstractSqueakObject) at0Node.execute(errorArray, i);
            if (value != image.nil) {
                output.add(((PointersObject) value).at0(0) + " (F)");
            }
        }
        if (output.size() == 0) {
            return "passed";
        }
        return String.join(", ", output);
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
