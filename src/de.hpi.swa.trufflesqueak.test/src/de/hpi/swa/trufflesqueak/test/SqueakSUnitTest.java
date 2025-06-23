/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import de.hpi.swa.trufflesqueak.test.SqueakTests.SqueakTest;
import de.hpi.swa.trufflesqueak.test.SqueakTests.TestType;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

/**
 * Run tests from the Squeak image.
 *
 * <p>
 * This test exercises all tests from the Squeak image. Optionally a subset of test selectors may be
 * selected using the system property "squeakTests" and the following syntax:
 *
 * <pre>
 * ObjectTest
 * ObjectTest>>testBecome
 * ObjectTest>>testBecome,ObjectTest>>testBecomeForward,SqueakSSLTest
 * </pre>
 *
 * Example VM parameter: {@code -DsqueakTests=ArrayTest}.<br/>
 * Using MX, individual tests can also be run from command line:
 *
 * <pre>
 * $ mx unittest -DsqueakTests="SqueakSSLTest>>testConnectAccept" SqueakSUnitTest --very-verbose --enable-timing
 * </pre>
 *
 * The system property {@code reloadImage} defines the behavior in event of a Java exception from a
 * primitive or test timeout expiry. The property value "exception" reloads the image and tries to
 * ensure a clean state for the next test case. If no next test case exists, reloading the image is
 * skipped. Using a value of {@code never} turns off image reloading, such that the image is only
 * loaded once in the very beginning of the test session. This can be unreliable, but saves a
 * significant amount of time when running buggy test suites locally.
 *
 * </p>
 */
@RunWith(Parameterized.class)
public final class SqueakSUnitTest extends AbstractSqueakTestCaseWithImage {

    private static final String TEST_CLASS_PROPERTY = "squeakTests";

    private static final List<SqueakTest> TESTS = selectTestsToRun().collect(toList());

    @Parameter public SqueakTest test;

    private static boolean stopRunningSuite;

    @Parameters(name = "{0} (#{index})")
    public static Collection<SqueakTest> getParameters() {
        return TESTS;
    }

    private static Stream<SqueakTest> selectTestsToRun() {
        final String toRun = System.getProperty(TEST_CLASS_PROPERTY);
        if (toRun != null && !MiscUtils.isBlank(toRun)) {
            return SqueakTests.getTestsToRun(toRun);
        }
        return SqueakTests.allTests();
    }

    @BeforeClass
    public static void beforeClass() {
        printStatistics();
        if (runsOnMXGate()) {
            loadTruffleSqueakPackages();
        }
    }

    private static void printStatistics() {
        final Map<TestType, Long> counts = countByType(TESTS);
        print(TestType.PASSING, counts, AnsiCodes.GREEN);
        print(TestType.SLOWLY_PASSING, counts, AnsiCodes.GREEN);
        print(TestType.FLAKY, counts, AnsiCodes.YELLOW);
        print(TestType.EXPECTED_FAILURE, counts, AnsiCodes.YELLOW);
        print(TestType.SLOWLY_FAILING, counts, AnsiCodes.RED);
        print(TestType.FAILING, counts, AnsiCodes.RED);
        print(TestType.NOT_TERMINATING, counts, AnsiCodes.RED);
        print(TestType.BROKEN_IN_SQUEAK, counts, AnsiCodes.BLUE);
        print(TestType.IGNORED, counts, AnsiCodes.BOLD);
    }

    @Test
    public void runSqueakTest() throws Throwable {
        checkTermination();

        TestResult result;
        try {
            result = runTestCase(test);
        } catch (final RuntimeException e) {
            e.printStackTrace();
            stopRunningSuite = true;
            throw e;
        }
        RuntimeException exceptionDuringReload = null;
        if (!(result.passed() && result.message().equals(PASSED_VALUE))) {
            printlnErr("Closing current image context and reloading: " + result.message());
            if (result.reason() != null) {
                result.reason().printStackTrace();
            } else {
                printlnErr("(no reason)");
            }
            try {
                if (test.type() == TestType.PASSING) {
                    reloadImage();
                    println("Retrying test that is expected to pass...");
                    result = runTestCase(test);
                }
            } catch (final RuntimeException e) {
                exceptionDuringReload = e;
            }
        }
        try {
            checkResult(result);
        } finally {
            if (exceptionDuringReload != null) {
                printlnErr("Exception during reload: " + exceptionDuringReload);
                exceptionDuringReload.printStackTrace();
                stopRunningSuite = true;
            }
        }
    }

    private void checkTermination() {
        Assume.assumeFalse("skipped", stopRunningSuite || test.type() == TestType.IGNORED || test.type() == TestType.NOT_TERMINATING || test.type() == TestType.BROKEN_IN_SQUEAK);
        if (test.type() == TestType.SLOWLY_FAILING || test.type() == TestType.SLOWLY_PASSING) {
            assumeNotOnMXGate();
        }
    }

    private void checkResult(final TestResult result) throws Throwable {
        switch (test.type()) {
            case PASSING, SLOWLY_PASSING, EXPECTED_FAILURE -> {
                if (result.reason() != null) {
                    throw result.reason();
                }
                assertTrue(result.message(), result.passed());
            }
            case PASSING_WITH_NFI -> checkPassingIf(image.supportsNFI(), result);
            case FAILING, SLOWLY_FAILING, BROKEN_IN_SQUEAK -> assertFalse(result.message(), result.passed());
            case FLAKY -> {
                // no verdict possible
            }
            case NOT_TERMINATING -> fail("This test unexpectedly terminated");
            case IGNORED -> fail("This test should never have been run");
            default -> throw new IllegalArgumentException(test.type().toString());
        }
    }

    private static void checkPassingIf(final boolean check, final TestResult result) throws Throwable {
        if (check) {
            if (result.reason() != null) {
                throw result.reason();
            }
            assertTrue(result.message(), result.passed());
        } else {
            assertFalse(result.message(), result.passed());
        }
    }

    private static void loadTruffleSqueakPackages() {
        final long start = System.currentTimeMillis();
        final String loadTemplate = """
                        [[[ | mc |
                        mc := MCFileTreeRepository path: '%s'.
                        Installer monticello
                            mc: mc;
                            packages: mc allPackageNames;
                            install ]
                                on: Warning do: [ :w | w resume ]]
                                on: Error do: [ :e | e retry ]]
                                on: ProgressInitiationException do: [ :e |
                                    e isNested
                                        ifTrue: [ e pass ]
                                        ifFalse: [ e rearmHandlerDuring:
                                            [[ e sendNotificationsTo: [ :min :max :current | "silence" ]]
                                                on: ProgressNotification do: [ :notification | notification resume ]]]].

                        (Smalltalk at: #TruffleSqueakUtilities) setUpAfterLoadingPackages.

                        Smalltalk snapshot: true andQuit: false.
                        """;
        evaluate(String.format(loadTemplate, getPathToInImageCode()));
        println("TruffleSqueak packages loaded and image saved in " + ((double) System.currentTimeMillis() - start) / 1000 + "s.");
    }

    private static Map<TestType, Long> countByType(final Collection<SqueakTest> tests) {
        return tests.stream().collect(groupingBy(SqueakTest::type, counting()));
    }

    private static void print(final TestType type, final Map<TestType, Long> counts, final String color) {
        // Checkstyle: stop
        System.out.printf("%s%5s %s tests%s%n",
                        color,
                        counts.getOrDefault(type, 0L),
                        type.getMessage(),
                        AnsiCodes.RESET);
        // Checkstyle: resume
    }

    protected static final class AnsiCodes {
        private static final String BOLD = "\033[1m";
        private static final String RED = "\033[31;1m";
        private static final String GREEN = "\033[32;1m";
        private static final String BLUE = "\033[34;1m";
        private static final String YELLOW = "\033[33;1m";
        private static final String RESET = "\033[0m";
    }
}
