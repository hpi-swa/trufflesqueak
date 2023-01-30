/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.trufflesqueak.util.DebugUtils;

public class AbstractSqueakTestCaseWithImage extends AbstractSqueakTestCase {
    private static final int SQUEAK_TIMEOUT_SECONDS = 60 * 2 * (DebugUtils.UNDER_DEBUG ? 1000 : 1);
    private static final int TIMEOUT_SECONDS = SQUEAK_TIMEOUT_SECONDS + 2;
    private static final int TEST_IMAGE_LOAD_TIMEOUT_SECONDS = 45 * (DebugUtils.UNDER_DEBUG ? 1000 : 1);
    private static final int PRIORITY_10_LIST_INDEX = 9;
    protected static final String PASSED_VALUE = "passed";

    protected static final String[] TRUFFLESQUEAK_TEST_CASE_NAMES = truffleSqueakTestCaseNames();

    private static PointersObject idleProcess;
    // For now we are single-threaded, so the flag can be static.
    private static volatile boolean testWithImageIsActive;
    private static ExecutorService executor;

    @BeforeClass
    public static void setUp() {
        loadTestImage();
        testWithImageIsActive = false;
    }

    public static void loadTestImage() {
        loadTestImage(true);
    }

    private static void loadTestImage(final boolean retry) {
        executor = Executors.newSingleThreadExecutor();
        final String imagePath = getPathToTestImage();
        try {
            runWithTimeout(imagePath, value -> loadImageContext(value), TEST_IMAGE_LOAD_TIMEOUT_SECONDS);
            println("Test image loaded from " + imagePath + "...");
            patchImageForTesting();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Test image load from " + imagePath + " was interrupted");
        } catch (final ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        } catch (final TimeoutException e) {
            e.printStackTrace();
            if (retry) {
                println("Retrying...");
                reloadImage();
            } else {
                throw new IllegalStateException("Timed out while trying to load the image from " + imagePath +
                                ".\nMake sure the image is not currently loaded by another executable");
            }
        }
    }

    @AfterClass
    public static void cleanUp() {
        executor.shutdown();
        idleProcess = null;
        image.interrupt.reset();
        destroyImageContext();
    }

    protected static void reloadImage() {
        cleanUp();
        loadTestImage(false);
    }

    private static void patchImageForTesting() {
        image.interrupt.start();
        final ArrayObject lists = (ArrayObject) image.getScheduler().instVarAt0Slow(PROCESS_SCHEDULER.PROCESS_LISTS);
        final PointersObject priority10List = (PointersObject) ArrayObjectReadNode.getUncached().execute(lists, PRIORITY_10_LIST_INDEX);
        final Object firstLink = priority10List.instVarAt0Slow(LINKED_LIST.FIRST_LINK);
        final Object lastLink = priority10List.instVarAt0Slow(LINKED_LIST.LAST_LINK);
        assert firstLink != NilObject.SINGLETON && firstLink == lastLink : "Unexpected idleProcess state";
        idleProcess = (PointersObject) firstLink;
        assert idleProcess.instVarAt0Slow(PROCESS.NEXT_LINK) == NilObject.SINGLETON : "Idle process expected to have `nil` successor";
        println("Increasing default timeout...");
        patchMethod("TestCase", "defaultTimeout", "defaultTimeout ^ " + SQUEAK_TIMEOUT_SECONDS);
        if (!runsOnMXGate()) {
            // Patch TestCase>>#performTest, so errors are printed to stderr for debugging purposes.
            patchMethod("TestCase", "performTest", "performTest [self perform: testSelector asSymbol] on: Error do: [:e | e printVerboseOn: FileStream stderr. e signal]");
        }
        println("Image ready for testing...");
    }

    protected static final boolean runsOnMXGate() {
        return "true".equals(System.getenv("MX_GATE"));
    }

    protected static final void assumeNotOnMXGate() {
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
        Path currentDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (currentDirectory != null) {
            final File file = currentDirectory.resolve("images").resolve(imageName).toFile();
            if (file.exists()) {
                return file.getAbsolutePath();
            }
            currentDirectory = currentDirectory.getParent();
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
        context.enter();
        try {
            return image.evaluate(expression);
        } finally {
            context.leave();
        }
    }

    protected static void patchMethod(final String className, final String selector, final String body) {
        println("Patching " + className + ">>#" + selector + "...");
        final Object patchResult = evaluate(String.join(" ",
                        className, "addSelectorSilently:", "#" + selector, "withMethod: (", className, "compile: '" + body + "'",
                        "notifying: nil trailer: (CompiledMethodTrailer empty) ifFail: [^ nil]) method"));
        assertNotEquals(NilObject.SINGLETON, patchResult);
    }

    protected static TestResult runTestCase(final TestRequest request) {
        if (testWithImageIsActive) {
            throw new IllegalStateException("The previous test case has not finished yet");
        }
        try {
            return runWithTimeout(request, value -> extractFailuresAndErrorsFromTestResult(value), TIMEOUT_SECONDS);
        } catch (final TimeoutException e) {
            return TestResult.fromException("did not terminate in " + TIMEOUT_SECONDS + "s", e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test was interrupted");
        } catch (final ExecutionException e) {
            return TestResult.fromException("failed with an error", e.getCause());
        }
    }

    protected static <T, R> R runWithTimeout(final T argument, final Function<T, R> function, final int timeout) throws InterruptedException, ExecutionException, TimeoutException {
        final Future<R> future = executor.submit(() -> {
            testWithImageIsActive = true;
            try {
                return function.apply(argument);
            } finally {
                testWithImageIsActive = false;
            }

        });
        try {
            return future.get(timeout, TimeUnit.SECONDS);
        } finally {
            if (testWithImageIsActive) {
                if (context != null) {
                    context.close(true);
                }
                testWithImageIsActive = false;
            }
            future.cancel(true);
        }
    }

    private static TestResult extractFailuresAndErrorsFromTestResult(final TestRequest request) {
        final Object result = evaluate(testCommand(request));
        if (!(result instanceof final NativeObject n) || !n.isByteType()) {
            return TestResult.failure("did not return a ByteString, got " + result);
        }
        final String testResult = ((NativeObject) result).asStringUnsafe();
        if (PASSED_VALUE.equals(testResult)) {
            return TestResult.success();
        } else {
            final boolean shouldPass = (boolean) evaluate(shouldPassCommand(request));
            // we cannot estimate or reliably clean up the state of the image after some unknown
            // exception was thrown
            if (shouldPass) {
                return TestResult.failure(testResult);
            } else {
                return TestResult.success(); // Expected failure in Squeak.
            }
        }
    }

    private static String testCommand(final TestRequest request) {
        return String.format("[(%s selector: #%s) runCase. '%s'] on: TestFailure, Error do: [:e | (String streamContents: [:s | e printVerboseOn: s]) withUnixLineEndings ]",
                        request.testCase, request.testSelector, PASSED_VALUE);
    }

    private static String shouldPassCommand(final TestRequest request) {
        return String.format("[(%s selector: #%s) shouldPass] on: Error do: [:e | false]", request.testCase, request.testSelector);
    }

    protected static final class TestRequest {
        protected final String testCase;
        protected final String testSelector;

        protected TestRequest(final String testCase, final String testSelector) {
            this.testCase = testCase;
            this.testSelector = testSelector;
        }
    }

    protected static final class TestResult {
        private static final TestResult SUCCESS = new TestResult(true, PASSED_VALUE, null);
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

        protected static TestResult success() {
            return SUCCESS;
        }
    }

    private static String[] truffleSqueakTestCaseNames() {
        final File[] srcDirectories = new File(getPathToInImageCode()).listFiles(File::isDirectory);
        final ArrayList<String> testCaseNames = new ArrayList<>();
        for (final File subDirectory : srcDirectories) {
            for (final File classDirectories : subDirectory.listFiles(pathname -> pathname.isDirectory() && pathname.getName().endsWith("Test.class"))) {
                testCaseNames.add(classDirectories.getName().substring(0, classDirectories.getName().lastIndexOf(".class")));
            }
        }
        return testCaseNames.toArray(new String[0]);
    }

    protected static final String getPathToInImageCode() {
        Path currentDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (currentDirectory != null) {
            final File file = currentDirectory.resolve("src").resolve("image").resolve("src").toFile();
            if (file.isDirectory()) {
                return file.getAbsolutePath();
            }
            currentDirectory = currentDirectory.getParent();
        }
        return null;
    }
}
