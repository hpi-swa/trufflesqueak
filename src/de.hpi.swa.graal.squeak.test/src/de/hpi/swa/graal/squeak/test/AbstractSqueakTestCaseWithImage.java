/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.test;

import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLogger;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
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
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, SqueakImageContext.class);
    private static final int SQUEAK_TIMEOUT_SECONDS = 60 * 2;
    private static final int TIMEOUT_SECONDS = SQUEAK_TIMEOUT_SECONDS + 5;
    private static final int PRIORITY_10_LIST_INDEX = 9;
    private static final int USER_PRIORITY_LIST_INDEX = 39;
    private static final String PASSED_VALUE = "passed";

    private static PointersObject idleProcess;
    private static boolean isClear;     // we have to be single-threaded, so the flag can be static

    @BeforeClass
    public static void loadTestImage() {
        final String imagePath = getPathToTestImage();
        loadImageContext(imagePath);
        image.getOutput().println("Test image loaded from " + imagePath + "...");
        patchImageForTesting();
        isClear = true;
    }

    @AfterClass
    public static void cleanUp() {
        idleProcess = null;
        image.interrupt.reset();
        destroyImageContext();
    }

    private static void reloadImage(final TestRequest request) {
        if (request.reloadImageOnException) {
            cleanUp();
            loadTestImage();
        }
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
            ensureCleanImageState();
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
            LOG.fine(() -> "After ensuring clean image state" + image.currentState());
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
        final ArrayObject externalObjects = (ArrayObject) image.getSpecialObject(SPECIAL_OBJECT.EXTERNAL_OBJECTS_ARRAY);
        if (externalObjects.isEmptyType()) {
            return;
        }
        final Object[] semaphores = externalObjects.getObjectStorage();
        for (int i = 0; i < semaphores.length; i++) {
            resetList(NilObject.SINGLETON, semaphores[i], "External semaphore #" + (i + 1));
            semaphores[i] = NilObject.SINGLETON;
        }
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
        image.evaluate("[(Delay classPool at: #SuspendedDelays ifAbsent: [OrderedCollection new]) removeAll. " +
                        "Delay classPool at: #ScheduledDelay put: nil; at: #FinishedDelay put: nil; at: #ActiveDelay put: nil. " +
                        "Delay startTimerEventLoop] value");
    }

    private static void ensureUserProcessForTesting() {
        final PointersObject activeProcess = image.getActiveProcessSlow();
        final long activePriority = (long) activeProcess.instVarAt0Slow(PROCESS.PRIORITY);
        if (activePriority == USER_PRIORITY_LIST_INDEX + 1) {
            return;
        }
        LOG.severe(() -> "STARTING ACTIVE PROCESS @" + activeProcess.hashCode() + " PRIORITY WAS: " + activePriority + image.currentState());
        final PointersObject newProcess = new PointersObject(image, image.processClass);
        newProcess.instVarAtPut0Slow(PROCESS.PRIORITY, Long.valueOf(USER_PRIORITY_LIST_INDEX + 1));
        image.getScheduler().instVarAtPut0Slow(PROCESS_SCHEDULER.ACTIVE_PROCESS, newProcess);

        if (activePriority == PRIORITY_10_LIST_INDEX + 1) {
            assert activeProcess == idleProcess;
            LOG.severe(() -> "IDLE PROCESS IS ACTIVE, REINSTALL IT (ProcessorScheduler installIdleProcess)");
            image.evaluate("ProcessorScheduler installIdleProcess");
            final ArrayObject lists = (ArrayObject) image.getScheduler().instVarAt0Slow(PROCESS_SCHEDULER.PROCESS_LISTS);
            final PointersObject priority10List = (PointersObject) ArrayObjectReadNode.getUncached().execute(lists, PRIORITY_10_LIST_INDEX);
            final Object firstLink = priority10List.instVarAt0Slow(LINKED_LIST.FIRST_LINK);
            final Object lastLink = priority10List.instVarAt0Slow(LINKED_LIST.LAST_LINK);
            assert firstLink instanceof PointersObject && firstLink == lastLink &&
                            ((PointersObject) firstLink).instVarAt0Slow(PROCESS.NEXT_LINK) == NilObject.SINGLETON : "Unexpected idleProcess state";
            idleProcess = (PointersObject) firstLink;
            LOG.fine(() -> image.currentState());
            return;
        }
    }

    protected static void patchMethod(final String className, final String selector, final String body) {
        image.getOutput().println("Patching " + className + ">>#" + selector + "...");
        final Object patchResult = image.evaluate(String.join(" ",
                        className, "addSelectorSilently:", "#" + selector, "withMethod: (", className, "compile: '" + body + "'",
                        "notifying: nil trailer: (CompiledMethodTrailer empty) ifFail: [^ nil]) method"));
        assertNotEquals(NilObject.SINGLETON, patchResult);
    }

    protected static TestResult runTestCase(final TestRequest request) {
        if (!isClear) {
            throw new IllegalStateException("The previous test case has not finished yet");
        }
        try {
            return runWithTimeout(request, () -> {
                isClear = false;
                LOG.fine(() -> "\nRunning test " + request.testCase + ">>" + request.testSelector);
                context.enter();
                try {
                    return extractFailuresAndErrorsFromTestResult(request);
                } finally {
                    context.leave();
                    isClear = true;
                }
            });
        } finally {
            if (!isClear) {
                LOG.severe("The context has not been left, we have to close it");
                if (!request.reloadImageOnException) {
                    // regardless of what the request says, we need to clean up.
                    cleanUp();
                    loadTestImage();
                }
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
            final boolean shouldPass = (boolean) image.evaluate(shouldPassCommand(request));
            if (shouldPass) {
                return TestResult.failure(testResult);
            } else {
                return TestResult.success("expected failure");
            }
        }
    }

    private static String testCommand(final TestRequest request) {
        return String.format("[[(%s selector: #%s) runCase. '%s'] on: TestFailure do: [:e | e asString ]] on: Error do: [:e | e asString, String crlf, e signalerContext shortStack]",
                        request.testCase, request.testSelector, PASSED_VALUE);
    }

    private static String shouldPassCommand(final TestRequest request) {
        return String.format("(%s selector: #%s) shouldPass", request.testCase, request.testSelector, PASSED_VALUE);
    }

    private static TestResult runWithTimeout(final TestRequest request, final Supplier<TestResult> action) {
        try {
            return CompletableFuture.supplyAsync(action).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (final TimeoutException e) {
            LOG.severe(() -> "Java timeout occurred in test " +
                            request.testCase + ">>" + request.testSelector + image.currentState());
            reloadImage(request);
            return TestResult.fromException("did not terminate in " + TIMEOUT_SECONDS + "s", e);
        } catch (final InterruptedException e) {
            LOG.fine(() -> "Interrupted execution in test " +
                            request.testCase + ">>" + request.testSelector + image.currentState());
            Thread.currentThread().interrupt();
            return TestResult.fromException("interrupted", e);
        } catch (final ExecutionException e) {
            LOG.severe(() -> "Execution exception " + e.getCause() + " occurred in test " +
                            request.testCase + ">>" + request.testSelector + image.currentState());
            reloadImage(request);
            return TestResult.fromException("failed with an error", e.getCause());
        }
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
