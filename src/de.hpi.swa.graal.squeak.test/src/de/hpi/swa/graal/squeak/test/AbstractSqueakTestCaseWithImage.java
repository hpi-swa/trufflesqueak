/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.test;

import static org.junit.Assert.assertNotEquals;

import java.io.File;
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

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLogger;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.nodes.ExecuteTopLevelContextNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

public class AbstractSqueakTestCaseWithImage extends AbstractSqueakTestCase {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, AbstractSqueakTestCaseWithImage.class);
    private static final int SQUEAK_TIMEOUT_SECONDS = Integer.valueOf(System.getProperty("SQUEAK_TIMEOUT", "2000"));    // generous
                                                                                                                        // default
                                                                                                                        // for
                                                                                                                        // debugging
    private static final int TIMEOUT_SECONDS = SQUEAK_TIMEOUT_SECONDS + 2;
    private static final int TEST_IMAGE_LOAD_TIMEOUT_SECONDS = Integer.valueOf(System.getProperty("IMAGE_LOAD_TIMEOUT", "1000"));    // generous
                                                                                                                                     // default
                                                                                                                                     // for
                                                                                                                                     // debugging
    private static final int PRIORITY_10_LIST_INDEX = 9;
    private static final int USER_PRIORITY_LIST_INDEX = 39;
    private static final String PASSED_VALUE = "passed";

    private static PointersObject idleProcess;
    private static volatile boolean testWithImageIsActive;     // for now we are single-threaded, so
                                                               // the flag can be static
    private static ExecutorService executor;

    @BeforeClass
    public static void setUp() {
        loadTestImage();
        testWithImageIsActive = false;
    }

    public static void loadTestImage() {
        executor = Executors.newSingleThreadExecutor();
        final String imagePath = getPathToTestImage();
        try {
            runWithTimeout(imagePath, value -> loadImageContext(value), TEST_IMAGE_LOAD_TIMEOUT_SECONDS);
            image.getOutput().println("Test image loaded from " + imagePath + "...");
            patchImageForTesting();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Test image load from " + imagePath + " was interrupted");
        } catch (final ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        } catch (final TimeoutException e) {
            throw new IllegalStateException("Timed out while trying to load the image from " + imagePath +
                            ".\nMake sure the image is not currently loaded by another executable");
        }
    }

    @AfterClass
    public static void cleanUp() {
        executor.shutdown();
        idleProcess = null;
        image.interrupt.reset();
        destroyImageContext();
    }

    private static void reloadImage() {
        cleanUp();
        loadTestImage();
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
        image.getOutput().println("Increasing default timeout...");
        patchMethod("TestCase", "defaultTimeout", "defaultTimeout ^ " + SQUEAK_TIMEOUT_SECONDS);
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
        context.enter();
        try {
            LOG.fine(() -> "\nEvaluating " + expression + image.currentState());
            final ExecuteTopLevelContextNode doItContextNode = image.getDoItContextNode(expression);
            return Truffle.getRuntime().createCallTarget(doItContextNode).call();
        } finally {
            context.leave();
        }
    }

    private static void ensureCleanImageState() {
        if (idleProcess != null) {
            if (idleProcess.instVarAt0Slow(PROCESS.NEXT_LINK) != NilObject.SINGLETON) {
                image.printToStdErr("Resetting dirty idle process...");
                idleProcess.instVarAtPut0Slow(PROCESS.NEXT_LINK, NilObject.SINGLETON);
            }
            resetProcessLists();
            resetSemaphoreLists();
            ensureTimerLoop();
            ensureUserProcessForTesting();
        }
    }

    private static void resetProcessLists() {
        final Object[] lists = ((ArrayObject) image.getScheduler().instVarAt0Slow(PROCESS_SCHEDULER.PROCESS_LISTS)).getObjectStorage();
        for (int i = 0; i < lists.length; i++) {
            final Object expectedValue = i == PRIORITY_10_LIST_INDEX ? idleProcess : NilObject.SINGLETON;
            resetList(expectedValue, lists[i], "scheduler list #" + (i + 1));
        }
    }

    private static void resetSemaphoreLists() {
        image.interrupt.reset();
        final Object interruptSema = image.getSpecialObject(SPECIAL_OBJECT.THE_INTERRUPT_SEMAPHORE);
        resetList(NilObject.SINGLETON, interruptSema, "Interrupt semaphore");
        // The timer semaphore is taken care of in ensureTimerLoop, since the delays need to be
        // reset as well
        final ArrayObject oldExternalObjects = (ArrayObject) image.getSpecialObject(SPECIAL_OBJECT.EXTERNAL_OBJECTS_ARRAY);
        evaluate("[ ExternalObjectTable current " +
                        "            initializeCaches;" +
                        "            externalObjectsArray: (Smalltalk specialObjectsArray at: 39 put: (Array new: 20)) ] value");
        final ArrayObject externalObjects = (ArrayObject) image.getSpecialObject(SPECIAL_OBJECT.EXTERNAL_OBJECTS_ARRAY);
        assert oldExternalObjects.getSqueakHash() != externalObjects.getSqueakHash();
    }

    private static void resetList(final Object newValue, final Object listOrNil, final String linkedListName) {
        if (listOrNil instanceof PointersObject) {
            final PointersObject linkedList = (PointersObject) listOrNil;
            final Object key = linkedList.instVarAt0Slow(LINKED_LIST.FIRST_LINK);
            final Object value = linkedList.instVarAt0Slow(LINKED_LIST.LAST_LINK);
            if (key != newValue || value != newValue) {
                LOG.severe(String.format("Removing inconsistent entry (%s->%s) from %s...", key, value, linkedListName));
                linkedList.instVarAtPut0Slow(LINKED_LIST.FIRST_LINK, newValue);
                linkedList.instVarAtPut0Slow(LINKED_LIST.LAST_LINK, newValue);
            }
        }
    }

    private static void ensureTimerLoop() {
        evaluate("[Delay primSignal: nil atUTCMicroseconds: 0. Delay classPool " +
                        "at: #SuspendedDelays put: nil; at: #ScheduledDelay put: nil; at: #FinishedDelay put: nil; at: #ActiveDelay put: nil. " +
                        "Delay startTimerEventLoop] value");
    }

    private static void ensureUserProcessForTesting() {
        final PointersObject activeProcess = image.getActiveProcessSlow();
        final long activePriority = (long) activeProcess.instVarAt0Slow(PROCESS.PRIORITY);
        if (activePriority == USER_PRIORITY_LIST_INDEX + 1) {
            return;
        }
        final PointersObject newProcess = new PointersObject(image, image.processClass);
        newProcess.instVarAtPut0Slow(PROCESS.PRIORITY, Long.valueOf(USER_PRIORITY_LIST_INDEX + 1));
        image.getScheduler().instVarAtPut0Slow(PROCESS_SCHEDULER.ACTIVE_PROCESS, newProcess);

        if (activePriority == PRIORITY_10_LIST_INDEX + 1) {
            assert activeProcess == idleProcess;
            LOG.severe(() -> "IDLE PROCESS IS ACTIVE, REINSTALL IT (ProcessorScheduler installIdleProcess)");
            evaluate("ProcessorScheduler installIdleProcess");
            final ArrayObject lists = (ArrayObject) image.getScheduler().instVarAt0Slow(PROCESS_SCHEDULER.PROCESS_LISTS);
            final PointersObject priority10List = (PointersObject) ArrayObjectReadNode.getUncached().execute(lists, PRIORITY_10_LIST_INDEX);
            final Object firstLink = priority10List.instVarAt0Slow(LINKED_LIST.FIRST_LINK);
            final Object lastLink = priority10List.instVarAt0Slow(LINKED_LIST.LAST_LINK);
            assert firstLink instanceof PointersObject && firstLink == lastLink &&
                            ((PointersObject) firstLink).instVarAt0Slow(PROCESS.NEXT_LINK) == NilObject.SINGLETON : "Unexpected idleProcess state";
            idleProcess = (PointersObject) firstLink;
            return;
        }
    }

    protected static void patchMethod(final String className, final String selector, final String body) {
        image.getOutput().println("Patching " + className + ">>#" + selector + "...");
        final Object patchResult = evaluate(String.join(" ",
                        className, "addSelectorSilently:", "#" + selector, "withMethod: (", className, "compile: '" + body + "'",
                        "notifying: nil trailer: (CompiledMethodTrailer empty) ifFail: [^ nil]) method"));
        assertNotEquals(NilObject.SINGLETON, patchResult);
    }

    protected static TestResult runTestCase(final TestRequest request) {
        if (testWithImageIsActive) {
            throw new IllegalStateException("The previous test case has not finished yet");
        }
        ensureCleanImageState();
        System.out.println("\nRunning testcase " + request.testCase + "." + request.testSelector);
        try {
            return runWithTimeout(request, value -> extractFailuresAndErrorsFromTestResult(value), TIMEOUT_SECONDS);
        } catch (final TimeoutException e) {
            reloadImage();
            return TestResult.fromException("did not terminate in " + TIMEOUT_SECONDS + "s", e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test was interrupted");
        } catch (final ExecutionException e) {
            if (request.reloadImageOnException) {
                reloadImage();
            }
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
            future.cancel(true);
            if (testWithImageIsActive) {
                if (context != null) {
                    context.close(true);
                }
                testWithImageIsActive = false;
            }
        }
    }

    private static TestResult extractFailuresAndErrorsFromTestResult(final TestRequest request) {
        final Object result = evaluate(testCommand(request));
        if (!(result instanceof NativeObject) || !((NativeObject) result).isString()) {
            return TestResult.failure("did not return a ByteString, got " + result);
        }
        final String testResult = ((NativeObject) result).toString();
        if (PASSED_VALUE.equals(testResult)) {
            assert ((NativeObject) result).isByteType() : "Passing result should always be a ByteString";
            return TestResult.success(testResult);
        } else {
            final boolean shouldPass = (boolean) evaluate(shouldPassCommand(request));
            if (shouldPass) {
                return TestResult.failure(testResult);
            } else {
                return TestResult.failure("expected failure in Squeak");
            }
        }
    }

    private static String testCommand(final TestRequest request) {
        return String.format(
                        "[(%s selector: #%s) runCase. '%s'] on: TestFailure, Error do: [:e | e signalerContext longStack]",
                        request.testCase, request.testSelector, PASSED_VALUE);
    }

    private static String shouldPassCommand(final TestRequest request) {
        return String.format("[(%s selector: #%s) shouldPass] on: Error do: [:e | false]", request.testCase, request.testSelector, PASSED_VALUE);
    }

    protected static final class TestRequest {
        protected final String testCase;
        protected final String testSelector;
        protected final boolean reloadImageOnException;

        protected TestRequest(final String testCase, final String testSelector, final boolean reloadImageOnException) {
            this.testCase = testCase;
            this.testSelector = testSelector;
            this.reloadImageOnException = reloadImageOnException;
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
