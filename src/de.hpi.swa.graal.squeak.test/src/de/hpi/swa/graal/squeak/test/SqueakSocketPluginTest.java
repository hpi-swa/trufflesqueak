package de.hpi.swa.graal.squeak.test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.IndirectCallNode;

import de.hpi.swa.graal.squeak.GraalSqueakMain;
import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageReaderNode;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.process.GetActiveProcessNode;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SqueakSocketPluginTest extends AbstractSqueakTestCase {
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

    // SOCKET TEST
    @Test
    public void testClientConnect() {
        executeTest("SocketTest", "testClientConnect");
    }

    @Test
    public void testDataReceive() {
        executeTest("SocketTest", "testDataReceive");
    }

    @Test
    public void testDataSending() {
        executeTest("SocketTest", "testDataSending");
    }

    @Test
    public void testLocalAddress() {
        executeTest("SocketTest", "testLocalAddress");
    }

    @Test
    public void testLocalPort() {
        executeTest("SocketTest", "testLocalPort");
    }

    @Test
    public void testPeerName() {
        executeTest("SocketTest", "testLocalPort");
    }

    @Test
    public void testReceiveTimeout() {
        executeTest("SocketTest", "testReceiveTimeout");
    }

    @Test
    public void testRemoteAddress() {
        executeTest("SocketTest", "testRemoteAddress");
    }

    @Test
    public void testRemotePort() {
        executeTest("SocketTest", "testRemotePort");
    }

    // @Test
    // public void testSendTimeout() {
    // executeTest("SocketTest", "testSendTimeout");
    // }

    @Test
    public void testServerAccept() {
        executeTest("SocketTest", "testServerAccept");
    }

    // @Test
    // public void testSocketReuse() {
    // executeTest("SocketTest", "testSocketReuse");
    // }

    @Test
    public void testStringFromAddress() {
        executeTest("SocketTest", "testStringFromAddress");
    }

    @Test
    public void testUDP() {
        executeTest("SocketTest", "testUDP");
    }

    @BeforeClass
    public static void loadTestImage() {
        final String imagePath = getPathToTestImage();
        ensureImageContext(imagePath);
        image.getOutput().println();
        image.getOutput().println("== Running " + SqueakLanguage.NAME + " SUnit Tests on " + GraalSqueakMain.getRuntimeName() + " ==");
        image.getOutput().println("Loading test image at " + imagePath + "...");
        final FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(imagePath);
        } catch (FileNotFoundException e) {
            throw new AssertionError("Test image not found");
        }
        final RootCallTarget target = Truffle.getRuntime().createCallTarget(new SqueakImageReaderNode(inputStream, image));
        IndirectCallNode.create().call(target, new Object[0]);
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
