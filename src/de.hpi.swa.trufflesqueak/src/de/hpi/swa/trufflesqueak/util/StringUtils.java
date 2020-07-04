/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public final class StringUtils {
    private static final MethodHandle COMPARE_TO = initCompareTo();

    private static MethodHandle initCompareTo() {
        try {
            final Method compareTo = Class.forName("java.lang.StringLatin1").getDeclaredMethod("compareTo", byte[].class, byte[].class);
            compareTo.setAccessible(true);
            return MethodHandles.publicLookup().unreflect(compareTo);
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
            return null;
        }
    }

    @TruffleBoundary(allowInlining = true)
    public static int compareTo(final byte[] value, final byte[] other) {
        try {
            return (int) COMPARE_TO.invokeExact(value, other);
        } catch (final Throwable e) {
            CompilerDirectives.transferToInterpreter();
            e.printStackTrace();
            return 0;
        }
    }
}
