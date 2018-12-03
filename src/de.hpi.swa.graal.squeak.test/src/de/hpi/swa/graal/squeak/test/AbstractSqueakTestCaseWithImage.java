package de.hpi.swa.graal.squeak.test;

import static org.junit.Assert.assertNotEquals;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.process.GetActiveProcessNode;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;

public class AbstractSqueakTestCaseWithImage extends AbstractSqueakTestCase {

    private static final int TIMEOUT_IN_SECONDS = 5 * 60;
    private static Object smalltalkDictionary;
    private static Object smalltalkAssociation;
    private static Object evaluateSymbol;
    private static Object compilerSymbol;

    @BeforeClass
    public static void loadTestImage() {
        final String imagePath = getPathToTestImage();
        ensureImageContext(imagePath);
        image.getOutput().println("Test image loaded from " + imagePath + "...");
        patchImageForTesting();
    }

    @AfterClass
    public static void cleanUp() {
        smalltalkDictionary = null;
        smalltalkAssociation = null;
        evaluateSymbol = null;
        compilerSymbol = null;
    }

    private static void patchImageForTesting() {
        final PointersObject activeProcess = GetActiveProcessNode.create(image).executeGet();
        activeProcess.atput0(PROCESS.SUSPENDED_CONTEXT, image.nil);
        image.getOutput().println("Modifying StartUpList for testing...");
        evaluate("{Delay. EventSensor. Project} do: [:ea | Smalltalk removeFromStartUpList: ea]");
        image.getOutput().println("Processing StartUpList...");
        evaluate("Smalltalk processStartUpList: true");
        image.getOutput().println("Setting author information...");
        evaluate("Utilities authorName: 'GraalSqueak'");
        evaluate("Utilities setAuthorInitials: 'GraalSqueak'");
        image.getOutput().println("Initializing fresh MorphicUIManager...");
        evaluate("Project current instVarNamed: #uiManager put: MorphicUIManager new");
        image.getOutput().println("Increasing default timeout...");
        patchMethod("TestCase", "defaultTimeout", "defaultTimeout ^ 25");
        if (!runsOnMXGate()) {
            patchMethod("TestCase", "runCase", "runCase [self setUp. [self performTest] ensure: [self tearDown]] on: Error do: [:e | e printVerboseOn: FileStream stderr. e signal]");
        }
    }

    private static boolean runsOnMXGate() {
        return "true".equals(System.getenv("MX_GATE"));
    }

    protected static void assumeNotOnMXGate() {
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
            smalltalkDictionary = image.specialObjectsArray.at0Object(SPECIAL_OBJECT.SMALLTALK_DICTIONARY);
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

    protected static Object asSymbol(final String value) {
        final String fakeMethodName = "fakeAsSymbol" + value.hashCode();
        final CompiledMethodObject method = makeMethod(
                        new Object[]{4L, image.getAsSymbolSelector(), image.wrap(value), image.newSymbol(fakeMethodName), getSmalltalkAssociation()},
                        0x21, 0xD0, 0x7C);
        return runMethod(method, getSmalltalkDictionary());
    }

    /*
     * Executes a fake Smalltalk method equivalent to:
     *
     * `^ (Smalltalk at: #Compiler) evaluate: expression`
     *
     */
    protected static Object evaluate(final String expression) {
        final String fakeMethodName = "fakeEvaluate" + expression.hashCode();
        final CompiledMethodObject method = makeMethod(
                        new Object[]{6L, getEvaluateSymbol(), getSmalltalkAssociation(), getCompilerSymbol(), image.wrap(expression), asSymbol(fakeMethodName), getSmalltalkAssociation()},
                        0x41, 0x22, 0xC0, 0x23, 0xE0, 0x7C);
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

    protected static String runTestCase(final String testClassName) {
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

    private static String extractFailuresAndErrorsFromTestResult(final Object result) {
        if (!(result instanceof AbstractSqueakObject) || !result.toString().equals("a TestResult")) {
            return "did not return a TestResult, got " + result.toString();
        }
        final PointersObject testResult = (PointersObject) result;
        final boolean hasPassed = (boolean) testResult.send("hasPassed");
        if (hasPassed) {
            return "passed";
        }
        final AbstractSqueakObject failures = (AbstractSqueakObject) testResult.send("failures");
        final AbstractSqueakObject errors = (AbstractSqueakObject) testResult.send("errors");
        final List<String> output = new ArrayList<>();
        appendTestResult(output, (ArrayObject) failures.send("asArray"), " (F)");
        appendTestResult(output, (ArrayObject) errors.send("asArray"), " (E)");
        assert output.size() > 0 : "Should not be empty";
        return String.join(", ", output);
    }

    private static void appendTestResult(final List<String> output, final ArrayObject array, final String suffix) {
        final SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();
        final SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        for (int i = 0; i < sizeNode.execute(array); i++) {
            final AbstractSqueakObject value = (AbstractSqueakObject) at0Node.execute(array, i);
            assert value != image.nil;
            output.add(((PointersObject) value).at0(0) + suffix);
        }
    }
}
