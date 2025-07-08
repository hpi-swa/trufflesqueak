/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.interop;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import com.oracle.truffle.api.strings.TruffleString;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.METHOD_DICT;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.LookupMethodNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

/** Similar to {@link LookupMethodNode}, but for interop. */
@GenerateInline
@GenerateUncached
@GenerateCached(false)
@SuppressWarnings("truffle-inlining") // inline = false is default for @Cached
public abstract class LookupMethodByStringNode extends AbstractNode {
    protected static final int LOOKUP_CACHE_SIZE = 3;

    public abstract Object executeLookup(Node node, ClassObject sqClass, String selectorBytes);

    public static final Object executeUncached(final ClassObject sqClass, final String selectorBytes) {
        return LookupMethodByStringNodeGen.getUncached().executeLookup(null, sqClass, selectorBytes);
    }

    @SuppressWarnings("unused")
    @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"classObject == cachedClass", "selector.equals(cachedSelector)"}, assumptions = {"cachedClass.getClassHierarchyAndMethodDictStable()"})
    protected static final Object doCached(final Node node, final ClassObject classObject, final String selector,
                    @Cached("classObject") final ClassObject cachedClass,
                    @Cached("selector") final String cachedSelector,
                    @Cached("doUncachedSlow(node, cachedClass, cachedSelector)") final Object cachedMethod) {
        return cachedMethod;
    }

    protected static final Object doUncachedSlow(final Node node, final ClassObject classObject, final String selector) {
        return doUncached(node, classObject, selector, AbstractPointersObjectReadNode.getUncached(), ArrayObjectReadNode.getUncached(), TruffleString.GetInternalByteArrayNode.getUncached());
    }

    @ReportPolymorphism.Megamorphic
    @Specialization(replaces = "doCached")
    protected static final Object doUncached(final Node node, final ClassObject classObject, final String selector,
                    /*
                     * An AbstractPointersObjectReadNode is sufficient for accessing `values`
                     * instance variable here.
                     */
                    @Cached final AbstractPointersObjectReadNode pointersReadValuesNode,
                    @Cached final ArrayObjectReadNode arrayReadNode,
                    @Cached TruffleString.GetInternalByteArrayNode getInternalByteArrayNode) {
        final byte[] selectorBytes = MiscUtils.toBytes(selector);
        ClassObject lookupClass = classObject;
        while (lookupClass != null) {
            final VariablePointersObject methodDict = lookupClass.getMethodDict();
            final Object[] methodDictVariablePart = methodDict.getVariablePart();
            for (int i = 0; i < methodDictVariablePart.length; i++) {
                final Object methodSelector = methodDictVariablePart[i];
                if (methodSelector instanceof final NativeObject m && Arrays.equals(selectorBytes, m.getTruffleStringAsReadonlyBytes(getInternalByteArrayNode))) {
                    return arrayReadNode.execute(node, pointersReadValuesNode.executeArray(node, methodDict, METHOD_DICT.VALUES), i);
                }
            }
            lookupClass = lookupClass.getSuperclassOrNull();
        }
        return null; // Signals a doesNotUnderstand.
    }
}
