/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.trufflesqueak.util.DebugUtils;

public class AbstractSqueakTestCaseWithImage extends AbstractSqueakTestCase {
    private static final int SQUEAK_TIMEOUT_SECONDS = 90 * (DebugUtils.UNDER_DEBUG ? 1000 : 1);
    private static final int TEST_IMAGE_LOAD_TIMEOUT_SECONDS = 45 * (DebugUtils.UNDER_DEBUG ? 1000 : 1);
    private static final int PRIORITY_10_LIST_INDEX = 9;
    private static final String TEST_IMAGE_FILE_NAME = "test-64bit.image";

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
            runWithTimeout(new TestImageSpec(imagePath, true), AbstractSqueakTestCase::loadImageContext);
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
        final PointersObject priority10List = (PointersObject) ArrayObjectReadNode.executeUncached(lists, PRIORITY_10_LIST_INDEX);
        final Object firstLink = priority10List.instVarAt0Slow(LINKED_LIST.FIRST_LINK);
        final Object lastLink = priority10List.instVarAt0Slow(LINKED_LIST.LAST_LINK);
        assert firstLink != NilObject.SINGLETON && firstLink == lastLink : "Unexpected idleProcess state";
        idleProcess = (PointersObject) firstLink;
        assert idleProcess.instVarAt0Slow(PROCESS.NEXT_LINK) == NilObject.SINGLETON : "Idle process expected to have `nil` successor";
        println("Increasing default timeout...");
        patchMethod("TestCase", "defaultTimeout", "defaultTimeout ^ " + SQUEAK_TIMEOUT_SECONDS);
        println("Image ready for testing...");
    }

    private static String getPathToTestImage() {
        Path currentDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (currentDirectory != null) {
            final File file = currentDirectory.resolve("images").resolve(TEST_IMAGE_FILE_NAME).toFile();
            if (file.exists()) {
                return file.getAbsolutePath();
            }
            currentDirectory = currentDirectory.getParent();
        }
        throw SqueakException.create("Unable to locate test image.");
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

    protected static <T, R> void runWithTimeout(final T argument, final Function<T, R> function) throws InterruptedException, ExecutionException, TimeoutException {
        final Future<R> future = executor.submit(() -> {
            testWithImageIsActive = true;
            try {
                return function.apply(argument);
            } finally {
                testWithImageIsActive = false;
            }
        });
        try {
            future.get(AbstractSqueakTestCaseWithImage.TEST_IMAGE_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
}
