/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.interop;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.VariablePointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.METHOD_DICT;
import de.hpi.swa.graal.squeak.nodes.AbstractLookupMethodWithSelectorNodes.LookupMethodNode;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.graal.squeak.util.MiscUtils;

/** Similar to {@link LookupMethodNode}, but for interop. */
@GenerateUncached
public abstract class ListMethodsNode extends AbstractNode {
    protected static final int LOOKUP_CACHE_SIZE = 3;

    public abstract Object executeLookup(ClassObject sqClass, String selectorBytes);

    @SuppressWarnings("unused")
    @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"classObject == cachedClass",
                    "selector.equals(cachedSelector)"}, assumptions = {"cachedClass.getClassHierarchyStable()", "cachedClass.getMethodDictStable()"})
    protected static final Object doCached(final ClassObject classObject, final String selector,
                    @Cached("classObject") final ClassObject cachedClass,
                    @Cached("selector") final String cachedSelector,
                    @Cached("doUncachedSlow(cachedClass, cachedSelector)") final Object cachedMethod) {
        return cachedMethod;
    }

    protected static final Object doUncachedSlow(final ClassObject classObject, final String selector) {
        return doUncached(classObject, selector, AbstractPointersObjectReadNode.getUncached(), ArrayObjectReadNode.getUncached());
    }

    @Specialization(replaces = "doCached")
    protected static final Object doUncached(final ClassObject classObject, final String selector,
                    /**
                     * An AbstractPointersObjectReadNode is sufficient for accessing `values`
                     * instance variable here.
                     */
                    @Cached final AbstractPointersObjectReadNode pointersReadValuesNode,
                    @Cached final ArrayObjectReadNode arrayReadNode) {
        final byte[] selectorBytes = MiscUtils.toBytes(selector);
        ClassObject lookupClass = classObject;
        while (lookupClass != null) {
            final VariablePointersObject methodDict = lookupClass.getMethodDict();
            final Object[] methodDictVariablePart = methodDict.getVariablePart();
            for (int i = 0; i < methodDictVariablePart.length; i++) {
                final Object methodSelector = methodDictVariablePart[i];
                if (methodSelector instanceof NativeObject && Arrays.equals(selectorBytes, ((NativeObject) methodSelector).getByteStorage())) {
                    return arrayReadNode.execute(pointersReadValuesNode.executeArray(methodDict, METHOD_DICT.VALUES), i - METHOD_DICT.NAMES);
                }
            }
            lookupClass = lookupClass.getSuperclassOrNull();
        }
        return null; // Signals a doesNotUnderstand.
    }
}
