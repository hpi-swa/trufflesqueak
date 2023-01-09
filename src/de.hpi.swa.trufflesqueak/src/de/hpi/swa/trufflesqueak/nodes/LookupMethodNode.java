/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.util.MethodCacheEntry;

@GenerateUncached
@GenerateInline(true)
@GenerateCached(false)
public abstract class LookupMethodNode extends AbstractNode {
    protected static final int LOOKUP_CACHE_SIZE = 6;

    public static LookupMethodNode getUncached() {
        return LookupMethodNodeGen.getUncached();
    }

    public abstract Object executeLookup(Node node, ClassObject sqClass, NativeObject selector);

    @SuppressWarnings("unused")
    @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"classObject == cachedClass", "selector == cachedSelector"}, //
                    assumptions = {"cachedClass.getClassHierarchyStable()", "cachedClass.getMethodDictStable()"})
    protected static final Object doCached(final ClassObject classObject, final NativeObject selector,
                    @Cached("classObject") final ClassObject cachedClass,
                    @Cached("selector") final NativeObject cachedSelector,
                    @Cached("classObject.lookupInMethodDictSlow(selector)") final Object cachedMethod) {
        return cachedMethod;
    }

    @ReportPolymorphism.Megamorphic
    @Specialization(replaces = "doCached")
    protected final Object doUncached(final ClassObject classObject, final NativeObject selector) {
        final MethodCacheEntry cachedEntry = getContext().findMethodCacheEntry(classObject, selector);
        if (cachedEntry.getResult() == null) {
            cachedEntry.setResult(classObject.lookupInMethodDictSlow(selector));
        }
        return cachedEntry.getResult(); /* `null` return signals a doesNotUnderstand. */
    }
}
