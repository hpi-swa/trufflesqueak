/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */

package de.hpi.swa.graal.squeak.test;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.Statement;

import de.hpi.swa.graal.squeak.test.SqueakTests.SqueakTest;
import de.hpi.swa.graal.squeak.test.Travis.AnsiCodes;

public final class TestLog {

    /**
     * Rule that creates new Travis folds for each test class.
     */
    public static class Rule implements TestRule {

        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    Travis.get().begin(description.getClassName(), description.getTestClass().getSimpleName());
                    try {
                        System.out.println("Running tests in " + description.getTestClass().getSimpleName());
                        base.evaluate();
                    } finally {
                        Travis.get().end();
                    }
                }
            };
        }
    }

    /**
     * Listener that creates Travis folds for each Squeak test class.
     *
     * <p>
     * JUnit run listeners are executed in the following order: first, our listener is run. Then,
     * the MX VerboseTextListener is run. The order is the same for the notifications of test start
     * and end.
     *
     * For the start notification, this is alright: first print log start marker, then MX starts
     * measuring.
     *
     * For the finish notification, order is an obstacle: MX would have to be run first, such that
     * the total test runtime is printed, and then the log end marker. We fix the order by emitting
     * the previous log end marker during the test start notification. This works for all cases
     * except the last test, because there is no reliable way to be called after the MX listener has
     * run.
     *
     * Pro: keep MX tooling and command line toggles functional. Con: the MX log line of the last
     * test of a Squeak test class will be part of the next fold.
     * </p>
     *
     * <p>
     * During the execution of a Squeak test class, we initialize a counter to the number of
     * expected test selectors, and count down. This relies on the sorting of Squeak test cases.
     * </p>
     */
    public static class Listener extends RunListener {

        private static final Map<String, Long> testToSelectorCount;
        private static final List<Failure> failures;
        private static AtomicLong expectedSelectors = new AtomicLong();

        static {
            testToSelectorCount = SqueakSUnitTest.TESTS.stream().collect(groupingBy(t -> t.className, counting()));
            failures = Collections.synchronizedList(new ArrayList<>());
        }

        @Override
        public void testStarted(final Description description) {
            if (enabled(description)) {
                if (firstSqueakTest(description)) {
                    Statistics.print(SqueakSUnitTest.TESTS);
                }
                end();
                begin(squeakTestOf(description));
            }
        }

        @Override
        public void testFailure(final Failure failure) {
            failures.add(failure);
        }

        @Override
        public void testFinished(final Description description) {
            if (enabled(description) && lastSqueakTest(description)) {
                end();
            }
        }

        private static boolean enabled(final Description description) {
            return description.getTestClass() == SqueakSUnitTest.class;
        }

        private static boolean firstSqueakTest(final Description description) {
            return SqueakSUnitTest.TESTS.get(0).nameEquals(squeakTestOf(description));
        }

        private static boolean lastSqueakTest(final Description description) {
            return SqueakSUnitTest.TESTS.get(SqueakSUnitTest.TESTS.size() - 1).nameEquals(squeakTestOf(description));
        }

        private static SqueakTest squeakTestOf(final Description description) {
            final Matcher test = SqueakTests.TEST_CASE.matcher(description.getMethodName());
            if (test.find()) {
                return new SqueakTest(null, test.group(1), test.group(2));
            }
            throw new IllegalArgumentException(description.toString());
        }

        private static synchronized void begin(final SqueakTest test) {
            if (expectedSelectors.get() == 0) {
                expectedSelectors.set(testToSelectorCount.get(test.className));
                Travis.get().begin(test.className, "Squeak test class: " + test.className);
            }
        }

        private static synchronized void end() {
            if (expectedSelectors.get() > 0 && expectedSelectors.decrementAndGet() == 0) {
                Travis.get().end();
                printAndClearFailures();
            }
        }

        private static void printAndClearFailures() {
            if (failures.isEmpty()) {
                return;
            }

            // Checkstyle: stop
            System.out.println(AnsiCodes.BOLD + AnsiCodes.RED + "Failed selectors:");
            for (final Failure failure : failures) {
                System.out.println(failure.getDescription());
            }
            System.out.println(AnsiCodes.RESET);
            // Checkstyle: resume

            failures.clear();
        }
    }
}
