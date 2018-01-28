package de.hpi.swa.trufflesqueak.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.BeforeClass;
import org.junit.Test;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;

public class SqueakInImageTest extends AbstractSqueakTestCase {
    private static final String IMAGE_PATH = String.format("%s/images/test.image", System.getenv("TRAVIS_BUILD_DIR"));
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
            smalltalkAssociation = image.newAssociation(image.newSymbol("Smalltalk"), getSmalltalkDictionary());
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
        String fakeMethodName = String.format("fakeAsSymbol%s", value.hashCode());
        CompiledCodeObject method = makeMethod(
                        new Object[]{4, image.asSymbol, image.wrap(value), image.newSymbol(fakeMethodName), getSmalltalkAssociation()},
                        new int[]{0x21, 0xD0, 0x7C});
        return runMethod(method, getSmalltalkDictionary());
    }

    private Object evaluate(String expression) {
        // ^ (Smalltalk at: #Compiler) evaluate: '{expression}'
        String fakeMethodName = String.format("fakeEvaluate%s", expression.hashCode());
        CompiledCodeObject method = makeMethod(
                        new Object[]{6, getEvaluateSymbol(), getSmalltalkAssociation(), getCompilerSymbol(), image.wrap(expression), asSymbol(fakeMethodName), getSmalltalkAssociation()},
                        new int[]{0x41, 0x22, 0xC0, 0x23, 0xE0, 0x7C});
        return runMethod(method, getSmalltalkDictionary());
    }

    @Test
    public void testCachesSimple() {
        for (int i = 0; i < 2; i++) {
            assertEquals(i + 1, evaluate(String.format("%s + 1", i)));
        }
    }
}
