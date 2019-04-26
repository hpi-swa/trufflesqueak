package de.hpi.swa.graal.squeak.interop;

import java.util.Arrays;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.METHOD_DICT;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.LookupMethodNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;

/** Similar to {@link LookupMethodNode}, but for interop. */
@GenerateUncached
public abstract class LookupMethodByStringNode extends AbstractNode {
    protected static final int LOOKUP_CACHE_SIZE = 3;

    public abstract Object executeLookup(ClassObject sqClass, String selectorBytes);

    public static LookupMethodByStringNode getUncached() {
        return LookupMethodByStringNodeGen.getUncached();
    }

    @SuppressWarnings("unused")
    @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"classObject == cachedClass",
                    "selector.equals(cachedSelector)"}, assumptions = {"classHierarchyStable", "methodDictStable"})
    protected static final Object doCached(final ClassObject classObject, final String selector,
                    @Cached("classObject") final ClassObject cachedClass,
                    @Cached("selector") final String cachedSelector,
                    @Cached("cachedClass.getClassHierarchyStable()") final Assumption classHierarchyStable,
                    @Cached("cachedClass.getMethodDictStable()") final Assumption methodDictStable,
                    @Cached final ArrayObjectReadNode readNode,
                    @Cached("doUncached(cachedClass, cachedSelector, readNode)") final Object cachedMethod) {
        return cachedMethod;
    }

    @Specialization(replaces = "doCached")
    protected static final Object doUncached(final ClassObject classObject, final String selector,
                    @Cached final ArrayObjectReadNode readNode) {
        final byte[] selectorBytes = selector.getBytes();
        ClassObject lookupClass = classObject;
        while (lookupClass != null) {
            final PointersObject methodDict = lookupClass.getMethodDict();
            for (int i = METHOD_DICT.NAMES; i < methodDict.size(); i++) {
                final Object methodSelector = methodDict.at0(i);
                if (methodSelector instanceof NativeObject && Arrays.equals(selectorBytes, ((NativeObject) methodSelector).getByteStorage())) {
                    final ArrayObject values = (ArrayObject) methodDict.at0(METHOD_DICT.VALUES);
                    return readNode.execute(values, i - METHOD_DICT.NAMES);
                }
            }
            lookupClass = lookupClass.getSuperclassOrNull();
        }
        return null; // Signals a doesNotUnderstand.
    }
}
