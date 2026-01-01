/*
 * Copyright (c) 2022-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2022-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import jdk.internal.module.Modules;

public final class ReflectionUtils {
    public static Constructor<?> lookupConstructor(final Class<?> declaringClass, final Class<?>... parameterTypes) {
        try {
            final Constructor<?> result = declaringClass.getDeclaredConstructor(parameterTypes);
            openModule(declaringClass);
            result.setAccessible(true);
            return result;
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Field lookupField(final Class<?> declaringClass, final String fieldName) {
        try {
            final Field result = declaringClass.getDeclaredField(fieldName);
            openModule(declaringClass);
            result.setAccessible(true);
            return result;
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Method lookupMethod(final Class<?> declaringClass, final String methodName, final Class<?>... parameterTypes) {
        try {
            final Method result = declaringClass.getDeclaredMethod(methodName, parameterTypes);
            openModule(declaringClass);
            result.setAccessible(true);
            return result;
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Ensure that this class is allowed to call setAccessible for an element of the provided
     * declaring class.
     */
    private static void openModule(final Class<?> declaringClass) {
        openModuleByClass(declaringClass, ReflectionUtils.class);
    }

    public static void openModuleByClass(final Class<?> declaringClass, final Class<?> accessingClass) {
        final Module declaringModule = declaringClass.getModule();
        final String packageName = declaringClass.getPackageName();
        Module namedAccessingModule = null;
        if (accessingClass != null) {
            final Module accessingModule = accessingClass.getModule();
            if (accessingModule.isNamed()) {
                namedAccessingModule = accessingModule;
            }
        }
        if (namedAccessingModule != null ? declaringModule.isOpen(packageName, namedAccessingModule) : declaringModule.isOpen(packageName)) {
            return;
        }
        if (namedAccessingModule != null) {
            Modules.addOpens(declaringModule, packageName, namedAccessingModule);
        } else {
            Modules.addOpensToAllUnnamed(declaringModule, packageName);
        }
    }
}
