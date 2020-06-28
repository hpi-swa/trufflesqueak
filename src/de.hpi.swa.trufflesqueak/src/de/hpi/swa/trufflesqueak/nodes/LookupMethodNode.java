/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.METHOD_DICT;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.util.MethodCacheEntry;

@GenerateUncached
@ReportPolymorphism
public abstract class LookupMethodNode extends AbstractNode {
    protected static final int LOOKUP_CACHE_SIZE = 6;

    public static LookupMethodNode create() {
        return LookupMethodNodeGen.create();
    }

    public static LookupMethodNode getUncached() {
        return LookupMethodNodeGen.getUncached();
    }

    public abstract Object executeLookup(ClassObject sqClass, NativeObject selector);

    @SuppressWarnings("unused")
    @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"classObject == cachedClass",
                    "selector == cachedSelector"}, assumptions = {"cachedClass.getClassHierarchyStable()", "cachedClass.getMethodDictStable()"})
    protected static final Object doCached(final ClassObject classObject, final NativeObject selector,
                    @Cached("classObject") final ClassObject cachedClass,
                    @Cached("selector") final NativeObject cachedSelector,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image,
                    @Cached("doUncachedSlow(cachedClass, cachedSelector, image)") final Object cachedMethod) {
        return cachedMethod;
    }

    protected static final Object doUncachedSlow(final ClassObject classObject, final NativeObject selector, final SqueakImageContext image) {
        return doUncached(classObject, selector, image);
    }

    @Specialization(replaces = "doCached")
    protected static final Object doUncached(final ClassObject classObject, final NativeObject selector,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        final MethodCacheEntry cachedEntry = image.findMethodCacheEntry(classObject, selector);
        if (cachedEntry.getResult() == null) {
            lookupInMethodDictAndCache(classObject, selector, cachedEntry);
        }
        return cachedEntry.getResult(); /* `null` return signals a doesNotUnderstand. */
    }

    @TruffleBoundary
    private static void lookupInMethodDictAndCache(final ClassObject classObject, final NativeObject selector, final MethodCacheEntry cachedEntry) {
        final AbstractPointersObjectReadNode readValuesNode = AbstractPointersObjectReadNode.getUncached();
        ClassObject lookupClass = classObject;
        while (lookupClass != null) {
            final VariablePointersObject methodDict = lookupClass.getMethodDict();
            final Object[] methodDictVariablePart = methodDict.getVariablePart();
            for (int i = 0; i < methodDictVariablePart.length; i++) {
                if (selector == methodDictVariablePart[i]) {
                    cachedEntry.setResult(readValuesNode.executeArray(methodDict, METHOD_DICT.VALUES).getObjectStorage()[i]);
                    return;
                }
            }
            lookupClass = lookupClass.getSuperclassOrNull();
        }
        assert !selector.isDoesNotUnderstand() : "Could not find does not understand method";
    }
}
