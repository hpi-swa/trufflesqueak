package de.hpi.swa.graal.squeak.test;

import static org.junit.Assert.assertNotEquals;

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

import com.oracle.truffle.api.Truffle;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.ExecuteTopLevelContextNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;

public class AbstractSqueakTestCaseWithImage extends AbstractSqueakTestCase {
    private static final int TIMEOUT_SECONDS = 60;
    private static final int PRIORITY_10_LIST_INDEX = 9;

    private static PointersObject idleProcess;

    @BeforeClass
    public static void loadTestImage() {
        final String imagePath = getPathToTestImage();
        loadImageContext(imagePath);
        image.getOutput().println("Test image loaded from " + imagePath + "...");
        patchImageForTesting();
    }

    @AfterClass
    public static void cleanUp() {
        idleProcess = null;
        destroyImageContext();
    }

    private static void reloadImage() {
        cleanUp();
        loadTestImage();
    }

    private static void patchImageForTesting() {
        image.getActiveProcess().atput0(PROCESS.SUSPENDED_CONTEXT, image.nil);
        image.getOutput().println("Modifying StartUpList for testing...");
        evaluate("{Delay. EventSensor. Project} do: [:ea | Smalltalk removeFromStartUpList: ea]");
        image.getOutput().println("Processing StartUpList...");
        evaluate("Smalltalk processStartUpList: true");
        final ArrayObject lists = (ArrayObject) image.getScheduler().at0(PROCESS_SCHEDULER.PROCESS_LISTS);
        final PointersObject priority10List = (PointersObject) lists.at0(PRIORITY_10_LIST_INDEX);
        final Object firstLink = priority10List.at0(LINKED_LIST.FIRST_LINK);
        final Object lastLink = priority10List.at0(LINKED_LIST.LAST_LINK);
        assert firstLink != image.nil && firstLink == lastLink : "Unexpected idleProcess state";
        idleProcess = (PointersObject) firstLink;
        assert idleProcess.at0(PROCESS.NEXT_LINK) == image.nil : "Idle process expected to have `nil` successor";
        image.getOutput().println("Setting author information...");
        evaluate("Utilities authorName: 'GraalSqueak'");
        evaluate("Utilities setAuthorInitials: 'GraalSqueak'");
        image.getOutput().println("Initializing fresh MorphicUIManager...");
        evaluate("Project current instVarNamed: #uiManager put: MorphicUIManager new");
        image.getOutput().println("Increasing default timeout...");
        patchMethod("TestCase", "defaultTimeout", "defaultTimeout ^ " + TIMEOUT_SECONDS);
        if (!runsOnMXGate()) {
            // Patch TestCase>>#performTest, so errors are printed to stderr for debugging purposes.
            patchMethod("TestCase", "performTest", "performTest [self perform: testSelector asSymbol] on: Error do: [:e | e printVerboseOn: FileStream stderr. e signal]");
        }
    }

    private static boolean runsOnMXGate() {
        return "true".equals(System.getenv("MX_GATE"));
    }

    protected static void assumeNotOnMXGate() {
        Assume.assumeFalse("skipped on `mx gate`", runsOnMXGate());
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
        throw SqueakException.create("Unable to locate test image.");
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

    /**
     * Some expressions need to be evaluate through the normal Compiler>>#evaluate: infrastructure,
     * for example because they require a parent context when they include non-local returns.
     */
    protected static Object compilerEvaluate(final String expression) {
        return evaluate("Compiler evaluate: '" + expression.replaceAll("'", "''") + "'");
    }

    protected static Object evaluate(final String expression) {
        ensureCleanImageState();
        final ExecuteTopLevelContextNode doItContextNode = image.getDoItContextNode(expression);
        return Truffle.getRuntime().createCallTarget(doItContextNode).call();
    }

    private static void ensureCleanImageState() {
        image.interrupt.reset();
        if (idleProcess != null) {
            if (idleProcess.at0(PROCESS.NEXT_LINK) != image.nil) {
                image.printToStdErr("Resetting dirty idle process...");
                idleProcess.atput0(PROCESS.NEXT_LINK, image.nil);
            }
            resetProcessLists();
        }
    }

    private static void resetProcessLists() {
        final AbstractSqueakObject[] lists = ((ArrayObject) image.getScheduler().at0(PROCESS_SCHEDULER.PROCESS_LISTS)).getAbstractSqueakObjectStorage();
        for (int i = 0; i < lists.length; i++) {
            final PointersObject linkedList = (PointersObject) lists[i];
            final Object key = linkedList.at0(LINKED_LIST.FIRST_LINK);
            final Object value = linkedList.at0(LINKED_LIST.LAST_LINK);
            final Object expectedValue = i == PRIORITY_10_LIST_INDEX ? idleProcess : image.nil;
            if (key != expectedValue || value != expectedValue) {
                image.printToStdErr(String.format("Removing inconsistent entry (%s->%s) from scheduler list #%s...", key, value, i + 1));
                linkedList.atput0(LINKED_LIST.FIRST_LINK, expectedValue);
                linkedList.atput0(LINKED_LIST.LAST_LINK, expectedValue);
            }
        }

    }

    protected static void patchMethod(final String className, final String selector, final String body) {
        image.getOutput().println("Patching " + className + ">>#" + selector + "...");
        final Object patchResult = evaluate(String.join(" ",
                        className, "addSelectorSilently:", "#" + selector, "withMethod: (", className, "compile: '" + body + "'",
                        "notifying: nil trailer: (CompiledMethodTrailer empty) ifFail: [^ nil]) method"));
        assertNotEquals(image.nil, patchResult);
    }

    protected static TestResult runTestCase(final String testClassName) {
        return runTestCase(testClassName, null);
    }

    protected static TestResult runTestCase(final String testClassName, final String testMethodName) {
        return invokeTestCase(testClassName, testMethodName);
    }

    private static TestResult invokeTestCase(final String testClassName, final String testMethodName) {
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

    private static TestResult extractFailuresAndErrorsFromTestResult(final Object result) {
        if (!(result instanceof AbstractSqueakObject) || !result.toString().equals("a TestResult")) {
            return TestResult.failure("did not return a TestResult, got " + result);
        }
        final PointersObject testResult = (PointersObject) result;
        final boolean hasPassed = (boolean) testResult.send("hasPassed");
        if (hasPassed) {
            return TestResult.success("passed");
        }
        final AbstractSqueakObject failures = (AbstractSqueakObject) testResult.send("failures");
        final AbstractSqueakObject errors = (AbstractSqueakObject) testResult.send("errors");
        final List<String> output = new ArrayList<>();
        appendTestResult(output, (ArrayObject) failures.send("asArray"), " (F)");
        appendTestResult(output, (ArrayObject) errors.send("asArray"), " (E)");
        assert output.size() > 0 : "Should not be empty";
        return TestResult.failure(String.join(", ", output));
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

    private static TestResult runWithTimeout(final Supplier<TestResult> action) {
        try {
            return CompletableFuture.supplyAsync(action).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (final TimeoutException e) {
            reloadImage();
            return TestResult.fromException("did not terminate in " + TIMEOUT_SECONDS + "s", e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return TestResult.fromException("interrupted", e);
        } catch (final ExecutionException e) {
            reloadImage();
            return TestResult.fromException("failed with an error", e.getCause());
        }
    }

    protected static final class TestResult {
        protected final boolean passed;
        protected final String message;
        protected final Throwable reason;

        private TestResult(final boolean passed, final String message, final Throwable reason) {
            this.passed = passed;
            this.message = message;
            this.reason = reason;
        }

        protected static TestResult fromException(final String message, final Throwable reason) {
            return new TestResult(false, message, reason);
        }

        protected static TestResult failure(final String message) {
            return new TestResult(false, message, null);
        }

        protected static TestResult success(final String message) {
            return new TestResult(true, message, null);
        }
    }
}
