/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.test;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import de.hpi.swa.graal.squeak.test.SqueakTests.SqueakTest;
import de.hpi.swa.graal.squeak.test.SqueakTests.TestType;

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
public class SqueakSUnitTest extends AbstractSqueakTestCaseWithImage {

    private static final String TEST_CLASS_PROPERTY = "squeakTests";

    private static final String RELOAD_IMAGE_PROPERTY = "reloadImage";
    private static final String RELOAD_ON_EXCEPTION = "exception";
    private static final String RELOAD_NEVER = "never";

    protected static final List<SqueakTest> TESTS = selectTestsToRun().collect(toList());

    @Parameter public SqueakTest test;

    @Parameters(name = "{0} (#{index})")
    public static Collection<SqueakTest> getParameters() {
        return TESTS;
    }

    private static Stream<SqueakTest> selectTestsToRun() {
        final String toRun = System.getProperty(TEST_CLASS_PROPERTY);
        if (toRun != null && !toRun.trim().isEmpty()) {
            return SqueakTests.getTestsToRun(toRun);
        }
        return SqueakTests.allTests();
    }

    @Test
    public void runSqueakTest() throws Throwable {
        checkTermination();

        final TestResult result = runTestCase(buildRequest());

        checkResult(result);
    }

    private void checkTermination() {
        Assume.assumeFalse("skipped", test.type == TestType.IGNORED || test.type == TestType.NOT_TERMINATING || test.type == TestType.BROKEN_IN_SQUEAK);
        if (test.type == TestType.SLOWLY_FAILING || test.type == TestType.SLOWLY_PASSING) {
            assumeNotOnMXGate();
        }
    }

    private TestRequest buildRequest() {
        return new TestRequest(test.className, test.selector, isReloadOnException());
    }

    private boolean isReloadOnException() {
        final String value = System.getProperty(RELOAD_IMAGE_PROPERTY, RELOAD_NEVER);
        switch (value) {
            case RELOAD_NEVER:
                return false;
            case RELOAD_ON_EXCEPTION:
                return !isLastTestCase();
            default:
                throw new IllegalArgumentException(value);
        }
    }

    private boolean isLastTestCase() {
        final SqueakTest last = TESTS.get(TESTS.size() - 1);
        return !last.nameEquals(test);
    }

    private void checkResult(final TestResult result) throws Throwable {

        switch (test.type) {
            case PASSING: // falls through
            case SLOWLY_PASSING:
                if (result.reason != null) {
                    throw result.reason;
                }
                assertTrue(result.message, result.passed);
                break;

            case PASSING_WITH_NFI:
                checkPassingIf(image.supportsNFI(), result);
                break;

            case FAILING: // falls through
            case SLOWLY_FAILING: // falls through
            case BROKEN_IN_SQUEAK: // falls through
            case EXPECTED_FAILURE:
                assertFalse(result.message, result.passed);
                break;

            case FLAKY:
                // no verdict possible
                break;

            case NOT_TERMINATING:
                fail("This test unexpectedly terminated");
                break;

            case IGNORED:
                fail("This test should never have been run");
                break;

            default:
                throw new IllegalArgumentException(test.type.toString());
        }
    }

    private static void checkPassingIf(final boolean check, final TestResult result) throws Throwable {
        if (check) {
            if (result.reason != null) {
                throw result.reason;
            }
            assertTrue(result.message, result.passed);
        } else {
            assertFalse(result.message, result.passed);
        }
    }
}
