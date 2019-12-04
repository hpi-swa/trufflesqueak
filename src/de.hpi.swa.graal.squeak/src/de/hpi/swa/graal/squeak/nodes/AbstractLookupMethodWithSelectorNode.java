/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;

@ReportPolymorphism
public abstract class AbstractLookupMethodWithSelectorNode extends AbstractNode {
    protected static final int LOOKUP_CACHE_SIZE = 6;

    public abstract Object executeLookup(ClassObject sqClass);

    @SuppressWarnings("unused")
    @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"classObject == cachedClass"}, assumptions = {"cachedClass.getClassHierarchyStable()", "cachedClass.getMethodDictStable()"})
    protected final static Object doCached(final ClassObject classObject,
                    @Cached("classObject") final ClassObject cachedClass,
                    @Cached("doUncachedSlow(cachedClass)") final Object cachedMethod) {
        return cachedMethod;
    }

    protected final Object doUncachedSlow(final ClassObject classObject) {
        return doUncached(classObject, AbstractPointersObjectReadNode.getUncached());
    }

    protected abstract Object doUncached(ClassObject classObject, AbstractPointersObjectReadNode uncached);
}
