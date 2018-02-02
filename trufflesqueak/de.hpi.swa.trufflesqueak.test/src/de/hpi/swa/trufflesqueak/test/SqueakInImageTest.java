package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertNotEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.BeforeClass;
import org.junit.Test;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.PointersObject;

public class SqueakInImageTest extends AbstractSqueakTestCase {
    private static final String IMAGE_PATH = System.getenv("TRAVIS_BUILD_DIR") + "/images/test.image";
    private static Object smalltalkDictionary;
    private static Object smalltalkAssociation;
    private static Object evaluateSymbol;
    private static Object compilerSymbol;

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

    private Object getEvaluateSymbol() {
        if (evaluateSymbol == null) {
            evaluateSymbol = asSymbol("evaluate:");
        }
        return evaluateSymbol;
    }

    private Object getCompilerSymbol() {
        if (compilerSymbol == null) {
            compilerSymbol = asSymbol("Compiler");
        }
        return compilerSymbol;
    }

    private Object asSymbol(String value) {
        String fakeMethodName = "fakeAsSymbol" + value.hashCode();
        CompiledCodeObject method = makeMethod(
                        new Object[]{4, image.asSymbol, image.wrap(value), image.newSymbol(fakeMethodName), getSmalltalkAssociation()},
                        new int[]{0x21, 0xD0, 0x7C});
        return runMethod(method, getSmalltalkDictionary());
    }

    private Object evaluate(String expression) {
        // ^ (Smalltalk at: #Compiler) evaluate: '{expression}'
        String fakeMethodName = "fakeEvaluate" + expression.hashCode();
        CompiledCodeObject method = makeMethod(
                        new Object[]{6, getEvaluateSymbol(), getSmalltalkAssociation(), getCompilerSymbol(), image.wrap(expression), asSymbol(fakeMethodName), getSmalltalkAssociation()},
                        new int[]{0x41, 0x22, 0xC0, 0x23, 0xE0, 0x7C});
        return runMethod(method, getSmalltalkDictionary());
    }

    @Test
    public void testNumerical() {
        // Evaluate a few simple expressions to ensure that methodDictionaries grow correctly.
        for (int i = 0; i < 10; i++) {
            assertEquals(i + 1, evaluate(i + " + 1"));
        }
        assertEquals(4, evaluate("-1 \\\\ 5"));
    }

    @Test
    public void testEnsure() {
        assertEquals(21, evaluate("[21] ensure: [42]"));
        assertEquals(42, evaluate("[21] ensure: [^42]"));
        assertEquals(21, evaluate("[^21] ensure: [42]"));
        assertEquals(42, evaluate("[^21] ensure: [^42]"));
    }

    @Test
    public void testOnError() {
        Object result = evaluate("[self error: 'foobar'] on: Error do: [:err| ^ err messageText]");
        assertEquals("foobar", result.toString());
    }

    @Test
    public void testValue() {
        assertEquals(42, evaluate("[42] value"));
        assertEquals(21, evaluate("[[21] value] value"));
    }

    @Test
    public void testTinyBenchmarks() {
        String resultString = evaluate("1 tinyBenchmarks").toString();
        assertTrue(resultString.contains("bytecodes/sec"));
        assertTrue(resultString.contains("sends/sec"));
    }

    @Test
    public void testRunArrayTest() {
        /*
         * Disable timeout logic by patching TestCase>>#timeout:after: (uses processes -> incompatible to
         * running headless).
         */
        Object patchResult = evaluate(
                        "TestCase addSelectorSilently: #timeout:after: withMethod: (TestCase compile: 'timeout: aBlock after: seconds ^ aBlock value' notifying: nil trailer: (CompiledMethodTrailer empty) ifFail: [^ nil]) method");
        assertNotEquals(image.nil, patchResult);

        // Run all tests of ArrayTest
        // String resultString = evaluate("ArrayTest buildSuite run").toString();

        // Run selected tests
        Object result = evaluate("#(#testAtWrap #testEmpty #testIsArray #testIsLiteral #testLiteralEqual #testNewWithSize) allSatisfy: [:ea | (ArrayTest run: ea) hasPassed]");
        assertEquals(image.sqTrue, result);
    }

}
