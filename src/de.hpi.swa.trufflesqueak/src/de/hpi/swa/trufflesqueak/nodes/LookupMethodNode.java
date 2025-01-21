/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.util.MethodCacheEntry;

@GenerateInline
@GenerateUncached
@GenerateCached(false)
public abstract class LookupMethodNode extends AbstractNode {
    protected static final int LOOKUP_CACHE_SIZE = 6;

    public abstract Object executeLookup(Node node, ClassObject sqClass, NativeObject selector);

    public static final Object executeUncached(final ClassObject sqClass, final NativeObject selector) {
        return LookupMethodNodeGen.getUncached().executeLookup(null, sqClass, selector);
    }

    @SuppressWarnings("unused")
    @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"classObject == cachedClass", "selector == cachedSelector"}, //
                    assumptions = {"cachedClass.getClassHierarchyAndMethodDictStable()"})
    protected static final Object doCached(final ClassObject classObject, final NativeObject selector,
                    @Cached("classObject") final ClassObject cachedClass,
                    @Cached("selector") final NativeObject cachedSelector,
                    @Cached("classObject.lookupInMethodDictSlow(selector)") final Object cachedMethod) {
        return cachedMethod;
    }

    @ReportPolymorphism.Megamorphic
    @Specialization(replaces = "doCached")
    protected static final Object doUncached(final ClassObject classObject, final NativeObject selector,
                    @Bind final SqueakImageContext image) {
        final MethodCacheEntry cachedEntry = image.findMethodCacheEntry(classObject, selector);
        if (cachedEntry.getResult() == null) {
            cachedEntry.setResult(classObject.lookupInMethodDictSlow(selector));
        }
        return cachedEntry.getResult(); /* `null` return signals a doesNotUnderstand. */
    }
}
