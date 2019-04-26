package de.hpi.swa.graal.squeak.test;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.test.SqueakTests.SqueakTest;

public class SqueakConsistencyTest extends AbstractSqueakTestCaseWithImage {

    @Test
    public void memoryIsSorted() {
        final List<String> tests = SqueakTests.rawTestNames();
        final List<String> orderedTests = SqueakTests.rawTestNames();
        orderedTests.sort(Comparator.comparing(String::toLowerCase));

        if (!orderedTests.equals(tests)) {
            assertEquals("Test memory must be sorted", join(orderedTests), join(tests));
        }
    }

    private static String join(final List<String> list) {
        return String.join("\n", list);
    }

    @Test
    public void memoryContainsNoDuplicates() {
        final List<String> tests = SqueakTests.rawTestNames();
        final Set<String> uniqueTests = new HashSet<>(tests);

        final List<String> duplicates = new ArrayList<>(tests);
        uniqueTests.forEach(duplicates::remove);
        assertEquals("Test memory contains duplicates: " + duplicates, tests.size(), uniqueTests.size());
    }

    @Test
    public void memoryContainsAllTestsFromImage() {
        // ImageSegmentTest works on "Smalltalk vmVMMakerVersion" to determine compatibility
        patchMethod("ImageSegmentTest class", "testSelectors", "testSelectors ^ super testSelectors");

        final Object collection = evaluate("(TestCase buildSuite tests collect: [:test | test asString])");

        final Set<String> imageTests = new HashSet<>(inspectCollection(collection));
        final Set<String> categorizedTests = collectTests();

        final Set<String> missing = new HashSet<>(imageTests);
        missing.removeAll(categorizedTests);
        printOrderedSelectors(missing);
        assertEquals("Tests from the image are not covered", emptySet(), missing);

        final Set<String> removed = new HashSet<>(categorizedTests);
        removed.removeAll(imageTests);
        printOrderedSelectors(removed);
        assertEquals("Tests no longer exist in the image", emptySet(), removed);
    }

    private static List<String> inspectCollection(final Object collection) {
        final ArrayObject array = (ArrayObject) ((PointersObject) collection).at0(0);
        final List<String> items = new ArrayList<>();
        for (final NativeObject aso : array.getNativeObjectStorage()) {
            items.add(aso.asStringUnsafe());
        }
        return items;
    }

    private static Set<String> collectTests() {
        return SqueakTests.allTests().map(SqueakTest::qualifiedName).collect(toSet());
    }

    private static void printOrderedSelectors(final Collection<String> selectors) {
        final List<String> ordered = new ArrayList<>(selectors);
        ordered.sort(Comparator.naturalOrder());
        ordered.forEach(System.out::println);
    }
}
