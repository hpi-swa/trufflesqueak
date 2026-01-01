/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;

public final class MethodCacheEntry {
    private ClassObject classObject;
    private NativeObject selector;
    private Object result;

    public ClassObject getClassObject() {
        return classObject;
    }

    public Object getSelector() {
        return selector;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(final Object object) {
        result = object;
    }

    public void freeAndRelease() {
        selector = null; /* Mark it free. */
        result = null; /* Release the method. */
    }

    public MethodCacheEntry reuseFor(final ClassObject lookupClass, final NativeObject lookupSelector) {
        classObject = lookupClass;
        selector = lookupSelector;
        result = null;
        return this;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "MethodCache[" + classObject + "/" + selector + "/" + result + "]" + " @" + Integer.toHexString(hashCode());
    }
}
