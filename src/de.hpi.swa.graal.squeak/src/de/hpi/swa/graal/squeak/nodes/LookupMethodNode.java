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
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.VariablePointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.METHOD_DICT;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;

@ReportPolymorphism
public abstract class LookupMethodNode extends AbstractNode {
    protected static final int LOOKUP_CACHE_SIZE = 6;

    public static LookupMethodNode create() {
        return LookupMethodNodeGen.create();
    }

    public abstract Object executeLookup(ClassObject sqClass, NativeObject selector);

    @SuppressWarnings("unused")
    @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"classObject == cachedClass",
                    "selector == cachedSelector"}, assumptions = {"cachedClass.getClassHierarchyStable()", "cachedClass.getMethodDictStable()"})
    protected static final Object doCached(final ClassObject classObject, final NativeObject selector,
                    @Cached("classObject") final ClassObject cachedClass,
                    @Cached("selector") final NativeObject cachedSelector,
                    @Cached("doUncachedSlow(cachedClass, cachedSelector)") final Object cachedMethod) {
        return cachedMethod;
    }

    protected static final Object doUncachedSlow(final ClassObject classObject, final NativeObject selector) {
        return doUncached(classObject, selector, AbstractPointersObjectReadNode.getUncached());
    }

    @Specialization(replaces = "doCached")
    protected static final Object doUncached(final ClassObject classObject, final NativeObject selector,
                    /**
                     * An AbstractPointersObjectReadNode is sufficient for accessing `values`
                     * instance variable here.
                     */
                    @Cached final AbstractPointersObjectReadNode readValuesNode) {
        ClassObject lookupClass = classObject;
        while (lookupClass != null) {
            final VariablePointersObject methodDict = lookupClass.getMethodDict();
            final Object[] methodDictVariablePart = methodDict.getVariablePart();
            for (int i = 0; i < methodDictVariablePart.length; i++) {
                if (selector == methodDictVariablePart[i]) {
                    return readValuesNode.executeArray(methodDict, METHOD_DICT.VALUES).getObjectStorage()[i];
                }
            }
            lookupClass = lookupClass.getSuperclassOrNull();
        }
        assert !selector.isDoesNotUnderstand() : "Could not find does not understand method";
        return null; // Signals a doesNotUnderstand.
    }
}
