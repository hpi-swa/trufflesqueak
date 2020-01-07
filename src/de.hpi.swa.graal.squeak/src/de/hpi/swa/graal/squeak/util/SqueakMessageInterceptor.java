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

    private static final String SYSTEM_PROPERTY = "squeakBreakpoints";
    private static final Pattern METHOD = Pattern.compile("(\\w+)>>((\\w+\\:)+|\\w+)");
    private static final String DEFAULTS = "TestCase>>runCase,TestCase>>logFailure:,TestCase>>signalFailure:,Object>>halt,Object>>inform:,SmalltalkImage>>logSqueakError:inContext:,UnhandledError>>defaultAction";

    private static final Map<String, Set<byte[]>> classNameToSelectorsMap = initializeClassNameToSelectorsMap();
    private static final Map<byte[], Set<ClassObject>> selectorToClassesMap = new IdentityHashMap<>();
    private static final Map<NativeObject, ClassObject> nativeSelectorToClassMap = new IdentityHashMap<>();
    private static final Map<NativeObject, String> nativeSelectorToStringMap = new IdentityHashMap<>();

    private static Map<String, Set<byte[]>> initializeClassNameToSelectorsMap() {
        final Map<String, Set<byte[]>> aClassNameToSelectorsMap = new HashMap<>();
        final String toIntercept = DEFAULTS + "," + System.getProperty(SYSTEM_PROPERTY, "");
        if (toIntercept != null && !toIntercept.trim().isEmpty()) {
            for (final String token : toIntercept.split(",")) {
                final Matcher nameAndSelector = METHOD.matcher(token);
                if (nameAndSelector.matches()) {
                    final String className = nameAndSelector.group(1);
                    final String selector = nameAndSelector.group(2);
                    final Set<byte[]> selectors = aClassNameToSelectorsMap.getOrDefault(className, new HashSet<>());
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
            nativeSelectorToStringMap.put(symbol, "Reached breakpoint " + aClass.getClassNameUnsafe() + ">>" + symbol.asStringUnsafe());
        }
    }

    public static ClassObject breakpointClassFor(final NativeObject selector) {
        return nativeSelectorToClassMap.get(selector);
    }

    // This is the "halt" method - put a breakpoint in your IDE on the print statement within
    public static void breakpointReached(final NativeObject selector) {
        System.out.println(nativeSelectorToStringMap.get(selector));
    }
}
