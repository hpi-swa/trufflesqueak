/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import com.oracle.truffle.api.CompilerAsserts;

import com.oracle.truffle.api.CompilerDirectives;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;

public final class MethodCacheEntry {
    private ClassObject classObject;
    private NativeObject selector;
    private Object result;
    private ClassObject.DispatchFailureResult dispatchFailure;
    private int dispatchFailureArity = -1;

    public ClassObject getClassObject() {
        return classObject;
    }

    public Object getSelector() {
        return selector;
    }

    public Object getResult() {
        return result;
    }

    public ClassObject.DispatchFailureResult getOrCreateDispatchFailureResult(final int arity) {
        if (dispatchFailure == null || dispatchFailureArity != arity) {
            dispatchFailure = createDispatchFailureResult(arity);
            dispatchFailureArity = arity;
        }
        return dispatchFailure;
    }

    @CompilerDirectives.TruffleBoundary
    private ClassObject.DispatchFailureResult createDispatchFailureResult(final int arity) {
        return classObject.resolveDispatchFailure(selector, arity);
    }

    public void setResult(final Object object) {
        result = object;
        dispatchFailure = null;
        dispatchFailureArity = -1;
    }

    public void freeAndRelease() {
        selector = null; /* Mark it free. */
        result = null; /* Release the method. */
        dispatchFailure = null;
        dispatchFailureArity = -1;
    }

    public MethodCacheEntry reuseFor(final ClassObject lookupClass, final NativeObject lookupSelector) {
        classObject = lookupClass;
        selector = lookupSelector;
        result = null;
        dispatchFailure = null;
        dispatchFailureArity = -1;
        return this;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "MethodCache[" + classObject + "/" + selector + "/" + result + "]" + " @" + Integer.toHexString(hashCode());
    }
}
