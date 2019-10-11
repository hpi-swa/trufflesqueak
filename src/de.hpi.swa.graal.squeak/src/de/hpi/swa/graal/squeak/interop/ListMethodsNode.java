/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.interop;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.VariablePointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.METHOD_DICT;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.LookupMethodNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.VariablePointersObjectReadNode;
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
                    @Cached("doUncached(cachedClass, cachedSelector)") final Object cachedMethod) {
        return cachedMethod;
    }

    protected static final Object doUncached(final ClassObject classObject, final String selector) {
        return doUncached(classObject, selector, VariablePointersObjectReadNode.getUncached(), ArrayObjectReadNode.getUncached());
    }

    @Specialization(replaces = "doCached")
    protected static final Object doUncached(final ClassObject classObject, final String selector,
                    @Cached final VariablePointersObjectReadNode pointersReadValuesNode,
                    @Cached final ArrayObjectReadNode arrayReadNode) {
        final byte[] selectorBytes = MiscUtils.toBytes(selector);
        ClassObject lookupClass = classObject;
        while (lookupClass != null) {
            final VariablePointersObject methodDict = lookupClass.getMethodDict();
            final Object[] methodDictVariablePart = methodDict.getVariablePart();
            for (int i = 0; i < methodDictVariablePart.length; i++) {
                final Object methodSelector = methodDictVariablePart[i];
                if (methodSelector instanceof NativeObject && Arrays.equals(selectorBytes, ((NativeObject) methodSelector).getByteStorage())) {
                    final ArrayObject values = pointersReadValuesNode.executeArray(methodDict, METHOD_DICT.VALUES);
                    return arrayReadNode.execute(values, i - METHOD_DICT.NAMES);
                }
            }
            lookupClass = lookupClass.getSuperclassOrNull();
        }
        return null; // Signals a doesNotUnderstand.
    }
}
