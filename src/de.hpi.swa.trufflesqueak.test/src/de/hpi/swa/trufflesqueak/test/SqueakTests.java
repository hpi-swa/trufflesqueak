/*
 * Copyright (c) 2017-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import de.hpi.swa.trufflesqueak.util.OS;

public final class SqueakTests {

    private static final Pattern TEST_CASE = Pattern.compile("(\\w+)>>(\\w+)");
    private static final Pattern TEST_CASE_LINE = Pattern.compile("^" + TEST_CASE.pattern());
    private static final String FILENAME = "tests.properties";
    private static final String TEST_TYPE_PREFIX_LINUX = "LINUX_";
    private static final String TEST_TYPE_PREFIX_MACOS = "MACOS_";
    private static final String TEST_TYPE_PREFIX_WINDOWS = "WINDOWS_";

    public enum TestType {
        PASSING("Passing"),
        PASSING_WITH_NFI("Passing with NFI"),
        SLOWLY_PASSING("Passing, but slow"),
        FLAKY("Flaky"),
        EXPECTED_FAILURE("Expected failure"),
        FAILING("Failing"),
        SLOWLY_FAILING("Failing and slow"),
        NOT_TERMINATING("Not Terminating"),
        BROKEN_IN_SQUEAK("Broken in Squeak"),
        IGNORED("Ignored"); // unable to run (e.g., OOM, ...)

        private final String message;

        TestType(final String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    protected record SqueakTest(TestType type, String className, String selector) {
        private String qualifiedName() {
            return className + ">>#" + selector;
        }

        @Override
        public String toString() {
            return (type == null ? "" : type.getMessage() + ": ") + className + ">>" + selector;
        }

        private boolean nameEquals(final SqueakTest test) {
            return className.equals(test.className) && selector.equals(test.selector);
        }
    }

    private SqueakTests() {
    }

    public static Stream<SqueakTest> getTestsToRun(final String filterExpression) {
        final List<SqueakTest> tests = allTests().filter(getTestFilter(filterExpression)).toList();
        if (tests.isEmpty()) {
            throw new IllegalArgumentException("No test cases found for filter expression '" + filterExpression + "'");
        }
        return tests.stream();
    }

    private static Predicate<SqueakTest> getTestFilter(final String filterExpression) {
        final List<String> classNames = new ArrayList<>();
        final List<SqueakTest> selectors = new ArrayList<>();

        for (final String token : filterExpression.split(",")) {
            final Matcher nameAndSelector = TEST_CASE.matcher(token);
            if (nameAndSelector.matches()) {
                selectors.add(new SqueakTest(null, nameAndSelector.group(1), nameAndSelector.group(2)));
            } else {
                classNames.add(token);
            }
        }

        return test -> classNames.contains(test.className) || selectors.stream().anyMatch(s -> s.nameEquals(test));
    }

    private static List<String> rawTestNames() {
        final InputStream testMapResource = SqueakTests.class.getResourceAsStream(FILENAME);
        if (testMapResource == null) {
            throw new IllegalStateException("Unable to find " + FILENAME);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(testMapResource))) {
            return reader.lines().map(TEST_CASE_LINE::matcher).filter(Matcher::find).map(Matcher::group).collect(toList());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Use in TruffleSqueakTest>>testTestMapConsistency
    public static boolean testTestMapConsistency(final List<String> imageTestNames) {
        // Convert into ArrayList to avoid excessive interop calls during comparisons
        final List<String> imageTestNameList = new ArrayList<>(imageTestNames);
        final List<String> mapTestNameList = rawTestNames();
        final HashSet<String> mapTestNameSet = new HashSet<>(mapTestNameList);
        if (mapTestNameList.size() != mapTestNameSet.size()) {
            printError("test.properties contains duplicates");
            return false;
        }
        final HashSet<String> imageTestNameSet = new HashSet<>(imageTestNameList);
        if (imageTestNameList.size() != imageTestNameSet.size()) {
            printError("Image reported duplicate tests");
            return false;
        }
        imageTestNameList.forEach(mapTestNameSet::remove);
        if (!mapTestNameSet.isEmpty()) {
            printError("Additional tests in test.properties:\n" + String.join("\n", mapTestNameSet));
            return false;
        }
        mapTestNameList.forEach(imageTestNameSet::remove);
        if (!imageTestNameSet.isEmpty()) {
            printError("Additional tests in image:\n" + String.join("\n", imageTestNameSet));
            return false;
        }
        return true;
    }

    // Checkstyle: stop
    private static void printError(final String value) {
        System.err.println(value);
        System.err.flush();
    }
    // Checkstyle: resume

    // Checkstyle: stop
    public static Stream<SqueakTest> allTests() {
        final Properties properties = loadProperties();
        return properties.stringPropertyNames().stream() //
                        .map(test -> parseTest(test, properties.getProperty(test))) //
                        .sorted(Comparator.comparing((final SqueakTest t) -> t.type).thenComparing(SqueakTest::qualifiedName));
    }
    // Checkstyle: resume

    private static SqueakTest parseTest(final String test, final String type) {
        final Matcher matcher = TEST_CASE_LINE.matcher(test);
        if (matcher.matches()) {
            return new SqueakTest(parseType(type), matcher.group(1), matcher.group(2));
        }

        throw new IllegalArgumentException(test);
    }

    private static TestType parseType(final String type) {
        final String[] parts = type.split(",");
        if (parts.length == 1) {
            return TestType.valueOf(type.toUpperCase());
        } else {
            final String prefix;
            if (OS.isLinux()) {
                prefix = TEST_TYPE_PREFIX_LINUX;
            } else if (OS.isMacOS()) {
                prefix = TEST_TYPE_PREFIX_MACOS;
            } else if (OS.isWindows()) {
                prefix = TEST_TYPE_PREFIX_WINDOWS;
            } else {
                throw new IllegalArgumentException("OS not supported");
            }
            for (final String part : parts) {
                final String partUpperCase = part.toUpperCase();
                if (partUpperCase.startsWith(prefix)) {
                    return TestType.valueOf(partUpperCase.substring(prefix.length()));
                }
            }
            throw new IllegalArgumentException("Unable to find type for " + prefix);
        }
    }

    private static Properties loadProperties() {
        try (InputStream in = SqueakTests.class.getResourceAsStream(FILENAME)) {
            final Properties properties = new Properties();
            properties.load(in);
            return properties;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
