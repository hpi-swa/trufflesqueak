package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.METHOD_DICT;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ReadArrayObjectNode;

public abstract class LookupMethodNode extends Node {
    protected static final int LOOKUP_CACHE_SIZE = 3;

    @Child private ReadArrayObjectNode readNode = ReadArrayObjectNode.create();

    public static LookupMethodNode create() {
        return LookupMethodNodeGen.create();
    }

    public abstract Object executeLookup(Object sqClass, Object selector);

    protected final Object performLookup(final ClassObject classObject, final NativeObject selector) {
        ClassObject lookupClass = classObject;
        while (lookupClass != null) {
            final PointersObject methodDict = lookupClass.getMethodDict();
            for (int i = METHOD_DICT.NAMES; i < methodDict.size(); i++) {
                final Object methodSelector = methodDict.at0(i);
                if (selector == methodSelector) {
                    final ArrayObject values = (ArrayObject) methodDict.at0(METHOD_DICT.VALUES);
                    return readNode.execute(values, i - METHOD_DICT.NAMES);
                }
            }
            lookupClass = lookupClass.getSuperclassOrNull();
        }
        assert !selector.isDoesNotUnderstand() : "Could not find does not understand method";
        return null; // Signals a doesNotUnderstand.
    }

    @SuppressWarnings("unused")
    @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"squeakClass == cachedSqClass",
                    "selector == cachedSelector"}, assumptions = {"methodLookupStable"})
    protected static final Object doDirect(final ClassObject squeakClass, final NativeObject selector,
                    @Cached("squeakClass") final ClassObject cachedSqClass,
                    @Cached("selector") final NativeObject cachedSelector,
                    @Cached("cachedSqClass.getMethodDictStable()") final Assumption methodLookupStable,
                    @Cached("performLookup(cachedSqClass, cachedSelector)") final Object cachedMethod) {
        return cachedMethod;
    }

    @Specialization(replaces = "doDirect")
    protected final Object doIndirect(final ClassObject sqClass, final NativeObject selector) {
        return performLookup(sqClass, selector);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static final CompiledMethodObject doFail(final Object sqClass, final Object selector) {
        throw new SqueakException("failed to lookup generic selector object on generic class");
    }
}
