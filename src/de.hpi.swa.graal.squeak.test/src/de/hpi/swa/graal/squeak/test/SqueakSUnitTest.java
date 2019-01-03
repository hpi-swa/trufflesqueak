package de.hpi.swa.graal.squeak.test;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import de.hpi.swa.graal.squeak.test.SqueakTests.SqueakTest;
import de.hpi.swa.graal.squeak.test.SqueakTests.TestType;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Run tests from the Squeak image.
 *
 * <p>
 * This test exercises all tests from the Squeak image. Optionally a single test class may be
 * selected via the system property "squeakTestClass" (example VM parameter:
 * {@code -DsqueakTestClass=ObjectTest})
 * </p>
 */
@RunWith(Parameterized.class)
public class SqueakSUnitTest extends AbstractSqueakTestCaseWithImage {

    private static final String TEST_CLASS_PROPERTY = "squeakTestClass";

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
    public void runSqueakTest() {
        checkTermination();

        final String result = runTestCase(test.className, test.selector);

        checkResult(result);
    }

    private void checkTermination() {
        Assume.assumeFalse("skipped", test.type == TestType.IGNORED || test.type == TestType.NOT_TERMINATING || test.type == TestType.BROKEN_IN_SQUEAK);
        if (test.type == TestType.SLOWLY_FAILING || test.type == TestType.SLOWLY_PASSING) {
            assumeNotOnMXGate();
        }
    }

    private void checkResult(final String result) {
        final boolean passed = result.contains("passed");

        switch (test.type) {
            case PASSING: // falls through
            case SLOWLY_PASSING:
                assertTrue(result, passed);
                break;

            case FAILING: // falls through
            case SLOWLY_FAILING: // falls through
            case BROKEN_IN_SQUEAK:
                assertFalse(result, passed);
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
}
