package de.hpi.swa.graal.squeak.test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import de.hpi.swa.graal.squeak.GraalSqueakMain;
import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.process.GetActiveProcessNode;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SqueakSocketStreamTest extends AbstractSqueakTestCase {
    private static Object smalltalkDictionary;
    private static Object smalltalkAssociation;
    private static Object evaluateSymbol;
    private static Object compilerSymbol;

    private static void executeTest(final String testClass, final String testMethodName) {
        assumeNotOnMXGate();
        final Object evaluation = evaluate("(" + testClass + " run: #" + testMethodName + ") asString");
        final String result = evaluation.toString();
        image.getOutput().println(result);
        if (result.contains("1 errors")) {
            throw new RuntimeException();
        }
        if (!result.contains("1 passes")) {
            fail();
        }
    }

    @Test
    public void testNextIntoClose() {
        executeTest("SocketStreamTest", "testNextIntoClose");
    }

    @Test
    public void testNextIntoCloseNonSignaling() {
        executeTest("SocketStreamTest", "testNextIntoCloseNonSignaling");
    }

    @Test
    public void testUpTo() {
        executeTest("SocketStreamTest", "testUpTo");
    }

    @Test
    public void testUpToAfterCloseNonSignaling() {
        executeTest("SocketStreamTest", "testUpToAfterCloseNonSignaling");
    }

    @Test
    public void testUpToAfterCloseSignaling() {
        executeTest("SocketStreamTest", "testUpToAfterCloseSignaling");
    }

    @Test
    public void testUpToAll() {
        executeTest("SocketStreamTest", "testUpToAll");
    }

    @Test
    public void testUpToAllAfterCloseNonSignaling() {
        executeTest("SocketStreamTest", "testUpToAllAfterCloseNonSignaling");
    }

    @Test
    public void testUpToAllAfterCloseSignaling() {
        executeTest("SocketStreamTest", "testUpToAllAfterCloseSignaling");
    }

    @Test
    public void testUpToAllAsciiVsBinary() {
        executeTest("SocketStreamTest", "testUpToAllAsciiVsBinary");
    }

    @Test
    public void testUpToAllCrlfAscii() {
        executeTest("SocketStreamTest", "testUpToAllCrlfAscii");
    }

    @Test
    public void testUpToAllCrlfBinary() {
        executeTest("SocketStreamTest", "testUpToAllCrlfBinary");
    }

    @Test
    public void testUpToAllCrlfCrlfAscii() {
        executeTest("SocketStreamTest", "testUpToAllCrlfCrlfAscii");
    }

    @Test
    public void testUpToAllCrlfCrlfBinary() {
        executeTest("SocketStreamTest", "testUpToAllCrlfCrlfBinary");
    }

    @Test
    public void testUpToAllEmptyPatternAscii() {
        executeTest("SocketStreamTest", "testUpToAllEmptyPatternAscii");
    }

    @Test
    public void testUpToAllEmptyPatternBinary() {
        executeTest("SocketStreamTest", "testUpToAllEmptyPatternBinary");
    }

    @Test
    public void testUpToAllLimit() {
        executeTest("SocketStreamTest", "testUpToAllLimit");
    }

    @Test
    public void testUpToAllLongPatternAscii() {
        executeTest("SocketStreamTest", "testUpToAllLongPatternAscii");
    }

    @Test
    public void testUpToAllLongPatternBinary() {
        executeTest("SocketStreamTest", "testUpToAllLongPatternBinary");
    }

    @Test
    public void testUpToAllMediumPatternAscii() {
        executeTest("SocketStreamTest", "testUpToAllMediumPatternAscii");
    }

    @Test
    public void testUpToAllMediumPatternBinary() {
        executeTest("SocketStreamTest", "testUpToAllMediumPatternBinary");
    }

    @Test
    public void testUpToAllShortPatternAscii() {
        executeTest("SocketStreamTest", "testUpToAllShortPatternAscii");
    }

    @Test
    public void testUpToAllShortPatternAscii2() {
        executeTest("SocketStreamTest", "testUpToAllShortPatternAscii2");
    }

    @Test
    public void testUpToAllShortPatternBinary() {
        executeTest("SocketStreamTest", "testUpToAllShortPatternBinary");
    }

    @Test
    public void testUpToAllShortPatternBinary2() {
        executeTest("SocketStreamTest", "testUpToAllShortPatternBinary2");
    }

    @Test
    public void testUpToAllTimeout() {
        executeTest("SocketStreamTest", "testUpToAllTimeout");
    }

    @Test
    public void testUpToAsciiVsBinary() {
        executeTest("SocketStreamTest", "testUpToAsciiVsBinary");
    }

    @Test
    public void testUpToEndClose() {
        executeTest("SocketStreamTest", "testUpToEndClose");
    }

    @Test
    public void testUpToEndCloseNonSignaling() {
        executeTest("SocketStreamTest", "testUpToEndCloseNonSignaling");
    }

    @Test
    public void testUpToMax() {
        executeTest("SocketStreamTest", "testUpToMax");
    }

    @Test
    public void testUpToTimeout() {
        executeTest("SocketStreamTest", "testUpToTimeout");
    }

    @BeforeClass
    public static void loadTestImage() {
        final String imagePath = getPathToTestImage();
        ensureImageContext(imagePath);
        image.getOutput().println();
        image.getOutput().println("== Running " + SqueakLanguage.NAME + " SUnit Tests on " + GraalSqueakMain.getRuntimeName() + " ==");
        image.getOutput().println("Loading test image at " + imagePath + "...");
        try {
            image.fillInFrom(new FileInputStream(imagePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        patchMethod("TestCase", "timeout:after:", "timeout: aBlock after: seconds ^ aBlock value");
        patchMethod("BlockClosure", "valueWithin:onTimeout:", "valueWithin: aDuration onTimeout: timeoutBlock ^ self value");
        if (!runsOnMXGate()) {
            patchMethod("TestCase", "runCase", "runCase [self setUp. [self performTest] ensure: [self tearDown]] on: Error do: [:e | e printVerboseOn: FileStream stderr. e signal]");
        }
        patchMethod("Project class", "uiManager", "uiManager ^ MorphicUIManager new");
    }

    private static boolean runsOnMXGate() {
        return "true".equals(System.getenv("MX_GATE"));
    }

    private static void assumeNotOnMXGate() {
        Assume.assumeFalse("TestCase skipped on `mx gate`.", runsOnMXGate());
    }

    private static String getPathToTestImage() {
        File currentDirectory = new File(System.getProperty("user.dir"));
        while (currentDirectory != null) {
            final String pathToImage = currentDirectory.getAbsolutePath() + File.separator + "images" + File.separator + "test.image";
            if (new File(pathToImage).exists()) {
                return pathToImage;
            }
            currentDirectory = currentDirectory.getParentFile();
        }
        throw new SqueakException("Unable to locate test image.");
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
}
