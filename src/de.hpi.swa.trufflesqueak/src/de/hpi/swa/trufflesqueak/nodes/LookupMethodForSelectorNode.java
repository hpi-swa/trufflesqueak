/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.util.MethodCacheEntry;

@ReportPolymorphism
public abstract class LookupMethodForSelectorNode extends AbstractNode {
    protected static final int LOOKUP_CACHE_SIZE = 6;
    protected final NativeObject selector;

    public LookupMethodForSelectorNode(final NativeObject selector) {
        this.selector = selector;
    }

    public static LookupMethodForSelectorNode create(final NativeObject selector) {
        return LookupMethodForSelectorNodeGen.create(selector);
    }

    public abstract Object executeLookup(ClassObject classObject);

    @SuppressWarnings("unused")
    @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"classObject == cachedClass"}, //
                    assumptions = {"cachedClass.getClassHierarchyStable()", "cachedClass.getMethodDictStable()"})
    protected static final Object doCached(final ClassObject classObject,
                    @Cached("classObject") final ClassObject cachedClass,
                    @Cached("classObject.lookupInMethodDictSlow(selector)") final Object cachedMethod) {
        return cachedMethod;
    }

    @Specialization(replaces = "doCached")
    protected final Object doUncached(final ClassObject classObject,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        final MethodCacheEntry cachedEntry = image.findMethodCacheEntry(classObject, selector);
        if (cachedEntry.getResult() == null) {
            cachedEntry.setResult(classObject.lookupInMethodDictSlow(selector));
        }
        return cachedEntry.getResult(); /* `null` return signals a doesNotUnderstand. */
    }
}
