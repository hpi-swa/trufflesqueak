package de.hpi.swa.graal.squeak.test;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import de.hpi.swa.graal.squeak.test.SqueakSUnitTestMap.TEST_TYPE;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SqueakSUnitTest extends AbstractSqueakTestCaseWithImage {

    @Parameters(name = "{0}: {1} (#{index})")
    public static Collection<Object[]> getParameters() {
        return Stream.of(TEST_TYPE.PASSING, TEST_TYPE.FAILING, TEST_TYPE.FLAKY, TEST_TYPE.NOT_TERMINATING).flatMap(SqueakSUnitTest::getSqueakTests).collect(toList());
    }

    @Parameter public String testType;

    @Parameter(1) public String testClass;

    @Test
    public void runSqueakTest() {
        if (testType.equals(TEST_TYPE.NOT_TERMINATING)) {
            assumeNotOnMXGate();
        }

        final String result = runTestCase(testClass);

        final boolean passed = result.contains("passed");
        if (testType.equals(TEST_TYPE.PASSING)) {
            assertTrue(passed);
        } else if (testType.equals(TEST_TYPE.FAILING)) {
            assertFalse(passed);
        }
    }

    private static Stream<Object[]> getSqueakTests(final String type) {
        final List<Object[]> tests = new ArrayList<>();
        final Object[] testMap = SqueakSUnitTestMap.SQUEAK_TEST_CASES;
        for (int index = 0; index < testMap.length; index += 2) {
            if (testMap[index + 1].equals(type)) {
                tests.add(new Object[]{testMap[index + 1], testMap[index]});
            }
        }
        return tests.stream();
    }
}
