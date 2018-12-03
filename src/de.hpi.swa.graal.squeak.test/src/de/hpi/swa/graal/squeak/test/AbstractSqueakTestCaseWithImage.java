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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;

public class AbstractSqueakTestCaseWithImage extends AbstractSqueakTestCase {

    private static final int TIMEOUT_MINUTES = 5;
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

    protected static void patchMethod(final String className, final String selector, final String body) {
        image.getOutput().println("Patching " + className + ">>#" + selector + "...");
        final Object patchResult = evaluate(String.join(" ",
                        className, "addSelectorSilently:", "#" + selector, "withMethod: (", className, "compile: '" + body + "'",
                        "notifying: nil trailer: (CompiledMethodTrailer empty) ifFail: [^ nil]) method"));
        assertNotEquals(image.nil, patchResult);
    }

    protected static String runTestCase(final String testClassName) {
        return runTestCase(testClassName, null);
    }

    protected static String runTestCase(final String testClassName, final String testMethodName) {
        final String preamble = testName(testClassName, testMethodName) + ": ";
        return preamble + invokeTestCase(testClassName, testMethodName);
    }

    private static String testName(final String testClassName, final String testMethodName) {
        return testClassName + (testMethodName == null ? "" : "#" + testMethodName);
    }

    private static String invokeTestCase(final String testClassName, final String testMethodName) {
        return runWithTimeout(() -> {
            final String testCommand = testCommand(testClassName, testMethodName);
            return extractFailuresAndErrorsFromTestResult(evaluate(testCommand));
        });
    }

    private static String testCommand(final String testClassName, final String testMethodName) {
        if (testMethodName == null) {
            return String.format("%s buildSuite run", testClassName);
        } else {
            return String.format("%s run: #%s", testClassName, testMethodName);
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

    private static String runWithTimeout(final Supplier<String> action) {
        try {
            return CompletableFuture.supplyAsync(action).get(TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (final TimeoutException e) {
            return "did not terminate in " + TIMEOUT_MINUTES + "m";
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return "interrupted";
        } catch (final ExecutionException e) {
            e.getCause().printStackTrace();
            return "failed with an error: " + e.getCause();
        }
    }
}
