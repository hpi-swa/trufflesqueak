package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertNotEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.PointersObject;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SqueakInImageTest extends AbstractSqueakTestCase {
    private static final String IMAGE_PATH = System.getenv("TRAVIS_BUILD_DIR") + "/images/test.image";
    private static Object smalltalkDictionary;
    private static Object smalltalkAssociation;
    private static Object evaluateSymbol;
    private static Object compilerSymbol;

    private static final Pattern runPattern = Pattern.compile("([0-9]+)\\ run");
    private static final Pattern passesPattern = Pattern.compile("([0-9]+)\\ passes");

    @Test
    public void testAAsSymbol() {
        assertEquals(image.asSymbol, asSymbol("asSymbol"));
    }

    @Test
    public void testBNumerical() {
        // Evaluate a few simple expressions to ensure that methodDictionaries grow correctly.
        for (long i = 0; i < 10; i++) {
            assertEquals(i + 1, evaluate(i + " + 1"));
        }
        assertEquals(4L, evaluate("-1 \\\\ 5"));
    }

    @Test
    public void testCThisContext() {
        assertEquals(42L, evaluate("thisContext return: 42"));
    }

    @Test
    public void testDEnsure() {
        assertEquals(21L, evaluate("[21] ensure: [42]"));
        assertEquals(42L, evaluate("[21] ensure: [^42]"));
        assertEquals(21L, evaluate("[^21] ensure: [42]"));
        assertEquals(42L, evaluate("[^21] ensure: [^42]"));
    }

    @Test
    public void testEOnError() {
        Object result = evaluate("[self error: 'foobar'] on: Error do: [:err| ^ err messageText]");
        assertEquals("foobar", result.toString());
        assertEquals("foobar", evaluate("[[self error: 'foobar'] value] on: Error do: [:err| ^ err messageText]").toString());
        assertEquals(image.sqTrue, evaluate("[[self error: 'foobar'] on: ZeroDivide do: [:e|]] on: Error do: [:err| ^ true]"));
        assertEquals(image.sqTrue, evaluate("[self error: 'foobar'. false] on: Error do: [:err| ^ err return: true]"));
    }

    @Test
    public void testFValue() {
        assertEquals(42L, evaluate("[42] value"));
        assertEquals(21L, evaluate("[[21] value] value"));
    }

    @Test
    public void testGSUnitTest() {
        assertEquals(image.sqTrue, evaluate("(TestCase new should: [1/0] raise: ZeroDivide) isKindOf: TestCase"));
    }

    @Test
    public void testWTinyBenchmarks() {
        String resultString = evaluate("1 tinyBenchmarks").toString();
        assertTrue(resultString.contains("bytecodes/sec"));
        assertTrue(resultString.contains("sends/sec"));
    }

    @Test
    public void testXPassingSqueakTests() {
        String[] testClasses = {"BagTest", "BooleanTest", "CollectionTest", "ProtoObjectTest", "SetTest", "UndefinedObjectTest"};
        for (int i = 0; i < testClasses.length; i++) {
            String testClass = testClasses[i];
            if (!evaluate(testClass + " buildSuite run hasPassed").equals(image.sqTrue)) {
                fail(testClass + " failed");
            }
        }
    }

    @Test
    public void testYFailingSqueakTests() {
        String[] testClasses = {"ArrayTest", "DateTest", "DictionaryTest", "DependentsArrayTest", "FalseTest", "LinkedListTest", "ObjectTest", "SmallIntegerTest",
                        "StringTest", "SymbolTest"};
        image.getOutput().println();
        image.getOutput().println("== Failing Squeak Tests ===================");
        for (int i = 0; i < testClasses.length; i++) {
            String testClass = testClasses[i];
            image.getOutput().print(testClass + ": ");
            String resultString = evaluate(testClass + " buildSuite run asString").toString();
            image.getOutput().println(resultString);
            Matcher runMatcher = runPattern.matcher(resultString);
            Matcher passesMatcher = passesPattern.matcher(resultString);
            if (runMatcher.find() && passesMatcher.find()) {
                if (runMatcher.group(1).equals(passesMatcher.group(1))) {
                    fail(testClass + " was failing but appears to be passing now");
                }
            } else {
                fail("Unable to find number of runs or/and number of passes");
            }
        }
        image.getOutput().print("==================================== ");
    }

    @Ignore
    @Test
    public void testZBrokenSqueakTests() {
        evaluate("ClosureTest buildSuite run");
        evaluate("FloatTest buildSuite run");
        evaluate("NumberTest buildSuite run");
        evaluate("ProcessTest buildSuite run");
        evaluate("TrueTest buildSuite run"); // doesNotUnderstand not working yet
    }

    @BeforeClass
    public static void setUpSqueakImageContext() {
        PrintWriter out = new PrintWriter(System.out, true);
        PrintWriter err = new PrintWriter(System.err, true);
        image = new SqueakImageContext(null, null, out, err);
        try {
            image.fillInFrom(new FileInputStream(IMAGE_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
        patchTestCaseTimeoutAfter();
    }

    private static void patchTestCaseTimeoutAfter() {
        /*
         * Disable timeout logic by patching TestCase>>#timeout:after: (uses processes -> incompatible to
         * running headless).
         */
        Object patchResult = evaluate(
                        "TestCase addSelectorSilently: #timeout:after: withMethod: (TestCase compile: 'timeout: aBlock after: seconds ^ aBlock value' notifying: nil trailer: (CompiledMethodTrailer empty) ifFail: [^ nil]) method");
        assertNotEquals(image.nil, patchResult);
    }

    private static Object getSmalltalkDictionary() {
        if (smalltalkDictionary == null) {
            smalltalkDictionary = image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.SmalltalkDictionary);
        }
        return smalltalkDictionary;
    }

    private static Object getSmalltalkAssociation() {
        if (smalltalkAssociation == null) {
            smalltalkAssociation = new PointersObject(image, image.schedulerAssociation.getSqClass(), new Object[]{image.newSymbol("Smalltalk"), getSmalltalkDictionary()});
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

    private static Object asSymbol(String value) {
        String fakeMethodName = "fakeAsSymbol" + value.hashCode();
        CompiledCodeObject method = makeMethod(
                        new Object[]{4L, image.asSymbol, image.wrap(value), image.newSymbol(fakeMethodName), getSmalltalkAssociation()},
                        new int[]{0x21, 0xD0, 0x7C});
        return runMethod(method, getSmalltalkDictionary());
    }

    private static Object evaluate(String expression) {
        // ^ (Smalltalk at: #Compiler) evaluate: '{expression}'
        String fakeMethodName = "fakeEvaluate" + expression.hashCode();
        CompiledCodeObject method = makeMethod(
                        new Object[]{6L, getEvaluateSymbol(), getSmalltalkAssociation(), getCompilerSymbol(), image.wrap(expression), asSymbol(fakeMethodName), getSmalltalkAssociation()},
                        new int[]{0x41, 0x22, 0xC0, 0x23, 0xE0, 0x7C});
        return runMethod(method, getSmalltalkDictionary());
    }
}
