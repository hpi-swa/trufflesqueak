package de.hpi.swa.graal.squeak.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.NativeObject;

public class SqueakMessageInterceptor {

    private static final String BREAK_PROPERTY = "squeakBreakpoints";
    private static final Pattern METHOD = Pattern.compile("(\\w+)>>((\\w+\\:)+|\\w+)");

    private static final Map<String, Set<byte[]>> classNameToSelectorsMap = initializeClassNameToSelectorsMap();
    private static final Map<byte[], Set<ClassObject>> selectorToClassesMap = new IdentityHashMap<>();
    private static final Map<NativeObject, ClassObject> nativeSelectorToClassMap = new IdentityHashMap<>();

    private static Map<String, Set<byte[]>> initializeClassNameToSelectorsMap() {
        final Map<String, Set<byte[]>> aClassNameToSelectorsMap = new HashMap<>();
        Set<byte[]> selectors = new HashSet<>();
        aClassNameToSelectorsMap.put("TestCase", selectors);
        selectors.add("logFailure:".getBytes());
        selectors.add("signalFailure:".getBytes());
        final String toIntercept = System.getProperty(BREAK_PROPERTY);
        if (toIntercept != null && !toIntercept.trim().isEmpty()) {
            for (final String token : toIntercept.split(",")) {
                final Matcher nameAndSelector = METHOD.matcher(token);
                if (nameAndSelector.matches()) {
                    final String className = nameAndSelector.group(1);
                    final String selector = nameAndSelector.group(2);
                    selectors = aClassNameToSelectorsMap.getOrDefault(className, new HashSet<>());
                    if (selectors.isEmpty()) {
                        aClassNameToSelectorsMap.put(className, selectors);
                    }
                    selectors.add(selector.getBytes());
                }
            }
        }
        return aClassNameToSelectorsMap;
    }

    public static void notifyLoadedClass(final ClassObject aClass, final String className) {
        final Set<byte[]> selectors = classNameToSelectorsMap.get(className);
        if (selectors == null) {
            return;
        }
        for (final byte[] selector : selectors) {
            final Set<ClassObject> classes = selectorToClassesMap.getOrDefault(selector, new HashSet<>());
            if (classes.isEmpty()) {
                selectorToClassesMap.put(selector, classes);
            } else {
                throw new RuntimeException("Cannot set more than one breakpoint per selector");
            }
            classes.add(aClass);
        }
    }

    public static void notifyLoadedSymbol(final NativeObject symbol, final byte[] bytes) {
        if (selectorToClassesMap.isEmpty()) {
            return;
        }
        byte[] requested = null;
        for (final byte[] selector : selectorToClassesMap.keySet()) {
            if (Arrays.equals(selector, bytes)) {
                requested = selector;
                break;
            }
        }
        if (requested == null) {
            return;
        }
        for (final ClassObject aClass : selectorToClassesMap.remove(requested)) {
            nativeSelectorToClassMap.put(symbol, aClass);
        }
    }

    public static ClassObject classFor(final NativeObject selector) {
        return nativeSelectorToClassMap.get(selector);
    }
}
